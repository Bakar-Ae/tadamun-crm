[CmdletBinding()]
param(
    [string]$BackupPath = 'backups\crm_db_phase_69.sql',

    [ValidateNotNullOrEmpty()]
    [string]$BackendImage = 'crm-system-backend:latest',

    [ValidateNotNullOrEmpty()]
    [string]$MySqlImage = 'mysql:8.4'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not [System.IO.Path]::IsPathRooted($BackupPath)) {
    $BackupPath = Join-Path $repoRoot $BackupPath
}
$BackupPath = [System.IO.Path]::GetFullPath($BackupPath)
if (-not (Test-Path -LiteralPath $BackupPath -PathType Leaf)) {
    throw "Migration rehearsal backup was not found: $BackupPath"
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$networkName = "crm_migration_rehearsal_$timestamp"
$mysqlContainer = "crm_migration_mysql_$timestamp"
$backendContainer = "crm_migration_backend_$timestamp"
$databaseName = 'crm_migration_rehearsal'
$databaseUser = 'crm_rehearsal'
$databasePassword = [guid]::NewGuid().ToString('N')
$rootPassword = [guid]::NewGuid().ToString('N')
$jwtSecret = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))
$apiKeyPepper = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))
$encryptionBytes = New-Object byte[] 32
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$random.GetBytes($encryptionBytes)
$random.Dispose()
$encryptionKey = [Convert]::ToBase64String($encryptionBytes)
$evidenceDirectory = Join-Path $repoRoot 'logs\migration-rehearsal'
$evidencePath = Join-Path $evidenceDirectory "rehearsal_$timestamp.json"
$stagedBackupPath = $BackupPath
$temporaryBackupPath = $null
$startedAt = Get-Date
$networkCreated = $false
$mysqlStarted = $false
$backendStarted = $false

$coreTables = @(
    'users',
    'customers',
    'contacts',
    'leads',
    'tasks',
    'notes',
    'attachments'
)

function Invoke-Docker {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments,

        [string]$FailureMessage = 'Docker command failed.'
    )

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw $FailureMessage
    }
}

function Invoke-MySqlScalar {
    param(
        [Parameter(Mandatory)]
        [string]$Query
    )

    $output = & docker exec `
        -e "MYSQL_PWD=$databasePassword" `
        $mysqlContainer mysql `
        --batch --skip-column-names `
        "-u$databaseUser" $databaseName `
        --execute $Query
    if ($LASTEXITCODE -ne 0) {
        throw "Migration rehearsal SQL query failed: $Query"
    }

    $value = $output | Select-Object -Last 1
    if ($null -eq $value) {
        throw "Migration rehearsal SQL query returned no value: $Query"
    }
    return $value.ToString().Trim()
}

function Get-CoreTableCounts {
    $counts = [ordered]@{}
    foreach ($table in $coreTables) {
        $counts[$table] = [long](Invoke-MySqlScalar -Query "SELECT COUNT(*) FROM $table;")
    }
    return $counts
}

Push-Location $repoRoot
try {
    New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null

    $migrationVersions = Get-ChildItem `
        -LiteralPath (Join-Path $repoRoot 'backend\src\main\resources\db\migration') `
        -Filter 'V*.sql' |
        ForEach-Object {
            if ($_.Name -match '^V(\d+)__') {
                [int]$Matches[1]
            }
        }
    $expectedVersion = ($migrationVersions | Measure-Object -Maximum).Maximum
    if ($null -eq $expectedVersion) {
        throw 'No Flyway migrations were found.'
    }

    $backupBytes = [System.IO.File]::ReadAllBytes($BackupPath)
    if ($backupBytes.Length -ge 2 -and (
        ($backupBytes[0] -eq 0xFF -and $backupBytes[1] -eq 0xFE) -or
        ($backupBytes[0] -eq 0xFE -and $backupBytes[1] -eq 0xFF)
    )) {
        $sourceEncoding = if ($backupBytes[0] -eq 0xFF) {
            [System.Text.Encoding]::Unicode
        }
        else {
            [System.Text.Encoding]::BigEndianUnicode
        }
        $sqlText = $sourceEncoding.GetString($backupBytes, 2, $backupBytes.Length - 2)
        $temporaryBackupPath = Join-Path `
            ([System.IO.Path]::GetTempPath()) `
            "crm_migration_rehearsal_$timestamp.sql"
        [System.IO.File]::WriteAllText(
            $temporaryBackupPath,
            $sqlText,
            (New-Object System.Text.UTF8Encoding($false))
        )
        $stagedBackupPath = $temporaryBackupPath
        Write-Host 'Staged the historical UTF-16 backup as temporary UTF-8 SQL.'
    }

    Write-Host "Creating isolated migration network $networkName..."
    Invoke-Docker -Arguments @('network', 'create', $networkName) `
        -FailureMessage 'Could not create the migration rehearsal network.'
    $networkCreated = $true

    Write-Host "Starting isolated MySQL container $mysqlContainer..."
    Invoke-Docker -Arguments @(
        'run', '--name', $mysqlContainer,
        '--network', $networkName,
        '-e', "MYSQL_ROOT_PASSWORD=$rootPassword",
        '-e', "MYSQL_DATABASE=$databaseName",
        '-e', "MYSQL_USER=$databaseUser",
        '-e', "MYSQL_PASSWORD=$databasePassword",
        '-d', $MySqlImage
    ) -FailureMessage 'Could not start the migration rehearsal database.'
    $mysqlStarted = $true

    $databaseReady = $false
    $deadline = (Get-Date).AddMinutes(2)
    do {
        Start-Sleep -Seconds 2
        $previousErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'SilentlyContinue'
            & docker exec `
                -e "MYSQL_PWD=$databasePassword" `
                $mysqlContainer mysql `
                --batch --skip-column-names `
                "-u$databaseUser" $databaseName `
                --execute 'SELECT 1;' *> $null
            $databaseReady = $LASTEXITCODE -eq 0
        }
        finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
    } while (-not $databaseReady -and (Get-Date) -lt $deadline)
    if (-not $databaseReady) {
        throw 'The migration rehearsal database did not become ready within two minutes.'
    }

    Invoke-Docker -Arguments @(
        'cp', $stagedBackupPath, "${mysqlContainer}:/tmp/source_backup.sql"
    ) -FailureMessage 'Could not copy the production-style backup into the rehearsal database.'
    Invoke-Docker -Arguments @(
        'exec', $mysqlContainer, 'sh', '-c',
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE" < /tmp/source_backup.sql'
    ) -FailureMessage 'Could not restore the production-style backup.'

    $preMigrationVersion = [int](Invoke-MySqlScalar -Query `
        'SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1;')
    if ($preMigrationVersion -ge $expectedVersion) {
        throw "Backup is already at Flyway version $preMigrationVersion; an older migration source is required."
    }
    $beforeCounts = Get-CoreTableCounts

    Write-Host "Migrating Flyway V$preMigrationVersion to V$expectedVersion with $BackendImage..."
    Invoke-Docker -Arguments @(
        'run', '--name', $backendContainer,
        '--network', $networkName,
        '-e', "SPRING_DATASOURCE_URL=jdbc:mysql://${mysqlContainer}:3306/$databaseName",
        '-e', "SPRING_DATASOURCE_USERNAME=$databaseUser",
        '-e', "SPRING_DATASOURCE_PASSWORD=$databasePassword",
        '-e', "JWT_SECRET=$jwtSecret",
        '-e', "PUBLIC_API_KEY_PEPPER=$apiKeyPepper",
        '-e', "WEBHOOK_SECRET_ENCRYPTION_KEYS=v1:$encryptionKey",
        '-e', "INTEGRATION_CREDENTIAL_ENCRYPTION_KEYS=v1:$encryptionKey",
        '-e', 'CORS_ALLOWED_ORIGINS=http://localhost',
        '-e', 'MAIL_HOST=localhost',
        '-e', 'MAIL_PORT=1025',
        '-e', 'MAIL_USERNAME=',
        '-e', 'MAIL_PASSWORD=',
        '-e', 'MAIL_SMTP_AUTH=false',
        '-e', 'MAIL_SMTP_STARTTLS=false',
        '-e', 'MAIL_FROM_ADDRESS=noreply@example.invalid',
        '-e', 'MAIL_REPLY_TO=support@example.invalid',
        '-e', 'APP_FRONTEND_BASE_URL=http://localhost',
        '-e', 'APP_EMAIL_ENABLED=false',
        '-e', 'STRIPE_ENABLED=false',
        '-e', 'WEBHOOK_WORKER_ENABLED=false',
        '-e', 'WORKFLOW_WORKER_ENABLED=false',
        '-e', 'INTEGRATION_WORKER_ENABLED=false',
        '-e', 'ATTACHMENT_STORAGE_PATH=/tmp/attachments',
        '-d', $BackendImage
    ) -FailureMessage 'Could not start the current backend for migration rehearsal.'
    $backendStarted = $true

    $backendReady = $false
    $deadline = (Get-Date).AddMinutes(3)
    do {
        Start-Sleep -Seconds 3
        $backendState = (& docker inspect --format '{{.State.Status}}' $backendContainer 2>$null).Trim()
        if ($backendState -eq 'exited' -or $backendState -eq 'dead') {
            docker logs $backendContainer --tail 100
            throw 'The rehearsal backend stopped before becoming healthy.'
        }

        $previousErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'SilentlyContinue'
            & docker exec $backendContainer `
                curl -fsS http://localhost:8081/actuator/health *> $null
            $backendReady = $LASTEXITCODE -eq 0
        }
        finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
    } while (-not $backendReady -and (Get-Date) -lt $deadline)
    if (-not $backendReady) {
        docker logs $backendContainer --tail 100
        throw 'The rehearsal backend did not become healthy within three minutes.'
    }

    $postMigrationVersion = [int](Invoke-MySqlScalar -Query `
        'SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1;')
    $failedMigrations = [int](Invoke-MySqlScalar -Query `
        'SELECT COUNT(*) FROM flyway_schema_history WHERE success <> 1;')
    $afterCounts = Get-CoreTableCounts

    foreach ($table in $coreTables) {
        if ($beforeCounts[$table] -ne $afterCounts[$table]) {
            throw "Record count changed for ${table}: before=$($beforeCounts[$table]), after=$($afterCounts[$table])."
        }
    }
    if ($postMigrationVersion -ne $expectedVersion -or $failedMigrations -ne 0) {
        throw "Migration did not finish cleanly: expected=$expectedVersion, actual=$postMigrationVersion, failed=$failedMigrations."
    }

    $unownedRecords = [long](Invoke-MySqlScalar -Query `
        'SELECT (SELECT COUNT(*) FROM teams WHERE organization_id IS NULL) + (SELECT COUNT(*) FROM customers WHERE organization_id IS NULL) + (SELECT COUNT(*) FROM contacts WHERE organization_id IS NULL) + (SELECT COUNT(*) FROM leads WHERE organization_id IS NULL) + (SELECT COUNT(*) FROM tasks WHERE organization_id IS NULL) + (SELECT COUNT(*) FROM notes WHERE organization_id IS NULL) + (SELECT COUNT(*) FROM attachments WHERE organization_id IS NULL);')
    $defaultOrganizations = [int](Invoke-MySqlScalar -Query `
        "SELECT COUNT(*) FROM organizations WHERE slug = 'tadamun';")
    $membershipCount = [long](Invoke-MySqlScalar -Query `
        'SELECT COUNT(*) FROM organization_memberships;')
    if ($unownedRecords -ne 0 -or $defaultOrganizations -ne 1) {
        throw "Tenant migration validation failed: unowned=$unownedRecords, defaultOrganizations=$defaultOrganizations."
    }

    $result = [pscustomobject]@{
        Result                  = 'SUCCESS'
        VerifiedAtUtc           = (Get-Date).ToUniversalTime().ToString('o')
        SourceBackup            = $BackupPath
        SourceSha256            = (Get-FileHash -LiteralPath $BackupPath -Algorithm SHA256).Hash
        PreMigrationVersion     = $preMigrationVersion
        PostMigrationVersion    = $postMigrationVersion
        AppliedMigrations       = $postMigrationVersion - $preMigrationVersion
        FailedMigrations        = $failedMigrations
        PreservedCoreTables     = $coreTables.Count
        UnownedTenantRecords    = $unownedRecords
        DefaultOrganizations    = $defaultOrganizations
        OrganizationMemberships = $membershipCount
        DurationSeconds         = [math]::Round(((Get-Date) - $startedAt).TotalSeconds, 2)
    }
    $result | ConvertTo-Json | Set-Content -LiteralPath $evidencePath -Encoding UTF8
    $result | Format-List
    Write-Host "Evidence: $evidencePath"
}
catch {
    New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null
    [pscustomobject]@{
        Result        = 'FAILURE'
        VerifiedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
        SourceBackup  = $BackupPath
        Error         = $_.Exception.Message
    } | ConvertTo-Json | Set-Content -LiteralPath $evidencePath -Encoding UTF8
    throw
}
finally {
    if ($backendStarted) {
        & docker rm -f $backendContainer *> $null
    }
    if ($mysqlStarted) {
        & docker rm -f $mysqlContainer *> $null
    }
    if ($networkCreated) {
        & docker network rm $networkName *> $null
    }
    if ($null -ne $temporaryBackupPath -and (Test-Path -LiteralPath $temporaryBackupPath)) {
        Remove-Item -LiteralPath $temporaryBackupPath -Force
    }
    Pop-Location
}
