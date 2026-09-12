[CmdletBinding()]
param(
    [ValidateNotNullOrEmpty()]
    [string]$SourceContainer = 'crm_mysql',

    [ValidateNotNullOrEmpty()]
    [string]$RestoreImage = 'mysql:8.4',

    [ValidateRange(1, 3650)]
    [int]$RetentionDays = 30
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$backupDirectory = Join-Path $repoRoot 'backups'
$evidenceDirectory = Join-Path $repoRoot 'logs\backup-verification'
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupPath = Join-Path $backupDirectory "crm_db_$timestamp.sql"
$evidencePath = Join-Path $evidenceDirectory "verification_$timestamp.json"
$containerBackupPath = '/tmp/crm_backup_verify.sql'
$restoreContainer = "crm_mysql_restore_verify_$timestamp"
$restoreDatabase = 'crm_restore_verify'
$restorePassword = [guid]::NewGuid().ToString('N')
$restoreStarted = $false

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

Push-Location $repoRoot
try {
    $sourceHealth = (& docker inspect `
        --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' `
        $SourceContainer 2>$null).Trim()
    if ($LASTEXITCODE -ne 0 -or $sourceHealth -ne 'healthy') {
        throw "$SourceContainer must be running and healthy before backup verification."
    }

    New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
    New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null

    Write-Host "Creating read-only MySQL backup from $SourceContainer..."
    Invoke-Docker -Arguments @(
        'exec', $SourceContainer, 'sh', '-c',
        'rm -f /tmp/crm_backup_verify.sql && MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u"$MYSQL_USER" --single-transaction --routines --triggers --events --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE" > /tmp/crm_backup_verify.sql'
    ) -FailureMessage 'MySQL backup creation failed.'

    Invoke-Docker -Arguments @(
        'cp', "${SourceContainer}:$containerBackupPath", $backupPath
    ) -FailureMessage 'Could not copy the MySQL backup to the host.'

    Invoke-Docker -Arguments @(
        'exec', $SourceContainer, 'rm', '-f', $containerBackupPath
    ) -FailureMessage 'Could not remove the temporary backup from the source container.'

    $backupFile = Get-Item -LiteralPath $backupPath
    if ($backupFile.Length -le 0) {
        throw 'The generated backup is empty.'
    }

    $createTableCount = @(
        Select-String -LiteralPath $backupPath -Pattern '^CREATE TABLE '
    ).Count
    $dumpCompleted = Select-String `
        -LiteralPath $backupPath `
        -Pattern '^-- Dump completed on ' `
        -Quiet
    if ($createTableCount -le 0 -or -not $dumpCompleted) {
        throw 'The generated file does not look like a complete MySQL dump.'
    }

    Write-Host "Restoring into isolated container $restoreContainer..."
    Invoke-Docker -Arguments @(
        'run', '--name', $restoreContainer,
        '-e', "MYSQL_ROOT_PASSWORD=$restorePassword",
        '-e', "MYSQL_DATABASE=$restoreDatabase",
        '-d', $RestoreImage
    ) -FailureMessage 'Could not start the isolated restore container.'
    $restoreStarted = $true

    $ready = $false
    $deadline = (Get-Date).AddMinutes(2)
    do {
        Start-Sleep -Seconds 2
        $previousErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'SilentlyContinue'
            & docker exec `
                -e "MYSQL_PWD=$restorePassword" `
                $restoreContainer mysql `
                --batch --skip-column-names -uroot `
                -e 'SELECT 1;' *> $null
            $ready = $LASTEXITCODE -eq 0
        }
        finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
    } while (-not $ready -and (Get-Date) -lt $deadline)

    if (-not $ready) {
        throw 'The isolated restore container did not become ready within two minutes.'
    }

    Invoke-Docker -Arguments @(
        'cp', $backupPath, "${restoreContainer}:/tmp/crm_backup.sql"
    ) -FailureMessage 'Could not copy the backup into the restore container.'

    Invoke-Docker -Arguments @(
        'exec', $restoreContainer, 'sh', '-c',
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE" < /tmp/crm_backup.sql'
    ) -FailureMessage 'The isolated database restore failed.'

    $tableCountOutput = & docker exec `
        -e "MYSQL_PWD=$restorePassword" `
        $restoreContainer mysql `
        --batch --skip-column-names -uroot $restoreDatabase `
        --execute 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE();'
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not count tables in the restored database.'
    }
    $tableCount = [int](($tableCountOutput | Select-Object -Last 1).Trim())
    if ($tableCount -ne $createTableCount) {
        throw "Restored table count mismatch: dump=$createTableCount, restored=$tableCount."
    }

    $requiredTableQuery = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('flyway_schema_history', 'users', 'organizations', 'customers', 'leads');"
    $requiredTableOutput = & docker exec `
        -e "MYSQL_PWD=$restorePassword" `
        $restoreContainer mysql `
        --batch --skip-column-names -uroot $restoreDatabase `
        --execute $requiredTableQuery
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not verify required tables in the restored database.'
    }
    $requiredTableCount = [int](($requiredTableOutput | Select-Object -Last 1).Trim())
    if ($requiredTableCount -ne 5) {
        throw 'One or more required CRM tables are missing from the restored database.'
    }

    $latestFlywayOutput = & docker exec `
        -e "MYSQL_PWD=$restorePassword" `
        $restoreContainer mysql `
        --batch --skip-column-names -uroot $restoreDatabase `
        --execute 'SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1;'
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not read Flyway history from the restored database.'
    }
    $latestFlywayVersion = ($latestFlywayOutput | Select-Object -Last 1).Trim()
    if ([string]::IsNullOrWhiteSpace($latestFlywayVersion)) {
        throw 'Could not verify Flyway history in the restored database.'
    }

    $hash = (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash
    $verificationResult = [pscustomobject]@{
        Result               = 'SUCCESS'
        VerifiedAtUtc        = (Get-Date).ToUniversalTime().ToString('o')
        BackupFile           = $backupPath
        SizeBytes            = $backupFile.Length
        Sha256               = $hash
        RestoredTables       = [int]$tableCount
        LatestFlywayVersion  = $latestFlywayVersion
        RequiredTablesFound  = [int]$requiredTableCount
    }
    $verificationResult |
        ConvertTo-Json |
        Set-Content -LiteralPath $evidencePath -Encoding UTF8

    $retentionCutoff = (Get-Date).AddDays(-$RetentionDays)
    Get-ChildItem -LiteralPath $backupDirectory -File |
        Where-Object {
            $_.Name -match '^crm_db_\d{8}-\d{6}\.sql$' -and
            $_.LastWriteTime -lt $retentionCutoff
        } |
        Remove-Item -Force
    Get-ChildItem -LiteralPath $evidenceDirectory -File |
        Where-Object {
            $_.Name -match '^verification_\d{8}-\d{6}\.json$' -and
            $_.LastWriteTime -lt $retentionCutoff
        } |
        Remove-Item -Force

    $verificationResult | Format-List
    Write-Host "Evidence: $evidencePath"
}
catch {
    New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null
    [pscustomobject]@{
        Result        = 'FAILURE'
        VerifiedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
        BackupFile    = $backupPath
        Error         = $_.Exception.Message
    } |
        ConvertTo-Json |
        Set-Content -LiteralPath $evidencePath -Encoding UTF8
    throw
}
finally {
    & docker exec $SourceContainer rm -f $containerBackupPath *> $null
    if ($restoreStarted) {
        & docker rm -f $restoreContainer *> $null
    }
    Pop-Location
}
