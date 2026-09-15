[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$CertificatePath,

    [string]$AivenEnvironmentPath,

    [string]$OutputPath,

    [string]$FrontendBaseUrl = 'http://localhost:5173'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $AivenEnvironmentPath) { $AivenEnvironmentPath = Join-Path $repoRoot '.env.aiven.local' }
if (-not $OutputPath) { $OutputPath = Join-Path $repoRoot '.env.render.local' }
$OutputPath = [System.IO.Path]::GetFullPath($OutputPath)

if (Test-Path -LiteralPath $OutputPath) {
    throw 'The output file already exists. Keep its existing secrets; this script does not rotate keys.'
}
$tracked = @(& git -C $repoRoot ls-files -- $OutputPath)
if ($LASTEXITCODE -ne 0 -or $tracked.Count -gt 0) {
    throw 'The private environment file must not be tracked by Git.'
}
& git -C $repoRoot check-ignore --quiet -- $OutputPath
if ($LASTEXITCODE -ne 0) { throw 'The output path must be ignored by Git before secrets are generated.' }

$frontendUri = [uri]$FrontendBaseUrl
if (-not $frontendUri.IsAbsoluteUri -or
    ($frontendUri.Scheme -ne 'https' -and -not ($frontendUri.Scheme -eq 'http' -and $frontendUri.IsLoopback)) -or
    $frontendUri.UserInfo -or $frontendUri.Query -or $frontendUri.Fragment -or $frontendUri.AbsolutePath -ne '/') {
    throw 'FrontendBaseUrl must be an HTTPS origin (or local HTTP for initial testing).'
}
$FrontendBaseUrl = $frontendUri.GetLeftPart([System.UriPartial]::Authority)

$aiven = @{}
foreach ($line in [System.IO.File]::ReadAllLines((Resolve-Path -LiteralPath $AivenEnvironmentPath))) {
    if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
    if ($line -notmatch '^\s*([A-Z][A-Z0-9_]*)\s*=(.*)$') { throw 'Invalid line in the private Aiven environment file.' }
    $name = $matches[1]
    $value = $matches[2].Trim()
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'")))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    if ($aiven.ContainsKey($name)) { throw "Duplicate environment variable: $name" }
    $aiven[$name] = $value
}
foreach ($name in @('AIVEN_HOST', 'AIVEN_PORT', 'AIVEN_DATABASE', 'AIVEN_USERNAME', 'AIVEN_PASSWORD')) {
    if ([string]::IsNullOrWhiteSpace($aiven[$name])) { throw "Missing environment variable: $name" }
}
if ($aiven.AIVEN_HOST -notmatch '^[a-zA-Z0-9.-]+\.aivencloud\.com$' -or
    $aiven.AIVEN_PORT -notmatch '^\d{1,5}$' -or [int]$aiven.AIVEN_PORT -notin 1..65535 -or
    $aiven.AIVEN_DATABASE -notmatch '^\w+$' -or $aiven.AIVEN_USERNAME -notmatch '^\w+$') {
    throw 'Invalid Aiven connection fields.'
}
if ($aiven.AIVEN_PASSWORD -eq 'PASTE_AIVEN_PASSWORD_HERE' -or
    $aiven.AIVEN_PASSWORD -notmatch '^[a-zA-Z0-9_./+=:@%\-]+$') {
    throw 'The Aiven password is missing or needs special .env quoting. No secrets were generated.'
}
$certificateFile = (Resolve-Path -LiteralPath $CertificatePath).Path
$certificate = [System.Security.Cryptography.X509Certificates.X509Certificate2]::new($certificateFile)
try {
    if ($certificate.NotAfter.ToUniversalTime() -le [DateTime]::UtcNow -or
        $certificate.NotBefore.ToUniversalTime() -gt [DateTime]::UtcNow) {
        throw 'The CA certificate is outside its validity period.'
    }
} finally { $certificate.Dispose() }
$certificateBase64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($certificateFile))

function New-RandomSecret {
    param([int]$Bytes = 32, [switch]$Base64)
    $buffer = New-Object byte[] $Bytes
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($buffer) } finally { $random.Dispose() }
    if ($Base64) { return [Convert]::ToBase64String($buffer) }
    return [BitConverter]::ToString($buffer).Replace('-', '').ToLowerInvariant()
}

$environment = [ordered]@{
    SPRING_DATASOURCE_URL = "jdbc:mysql://$($aiven.AIVEN_HOST):$($aiven.AIVEN_PORT)/$($aiven.AIVEN_DATABASE)"
    SPRING_DATASOURCE_USERNAME = $aiven.AIVEN_USERNAME
    SPRING_DATASOURCE_PASSWORD = $aiven.AIVEN_PASSWORD
    AIVEN_CA_CERT_BASE64 = $certificateBase64
    JWT_SECRET = New-RandomSecret -Bytes 64
    PUBLIC_API_KEY_PEPPER = New-RandomSecret
    WEBHOOK_SECRET_ENCRYPTION_KEYS = 'v1:' + (New-RandomSecret -Base64)
    WEBHOOK_SECRET_ENCRYPTION_KEY_VERSION = 'v1'
    INTEGRATION_CREDENTIAL_ENCRYPTION_KEYS = 'v1:' + (New-RandomSecret -Base64)
    INTEGRATION_CREDENTIAL_ENCRYPTION_KEY_VERSION = 'v1'
    APP_BOOTSTRAP_ADMIN_EMAIL = 'admin@crm.com'
    APP_BOOTSTRAP_ADMIN_PASSWORD = New-RandomSecret -Bytes 24
    JAVA_TOOL_OPTIONS = '-Xms64m -Xmx224m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=16m -Xss512k -XX:+UseSerialGC -XX:ActiveProcessorCount=1 -XX:+ExitOnOutOfMemoryError'
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = '5'
    SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = '1'
    SERVER_TOMCAT_THREADS_MAX = '20'
    SERVER_TOMCAT_THREADS_MIN_SPARE = '2'
    WEBHOOK_ALLOW_LOCAL_HTTP = 'false'
    WEBHOOK_WORKER_POLL_INTERVAL_MS = '15000'
    WORKFLOW_WORKER_POLL_INTERVAL_MS = '15000'
    INTEGRATION_WORKER_POLL_INTERVAL_MS = '15000'
    TENANT_ENFORCEMENT_ENABLED = 'true'
    APP_TIME_ZONE = 'Africa/Mogadishu'
    CORS_ALLOWED_ORIGINS = $FrontendBaseUrl
    APP_FRONTEND_BASE_URL = $FrontendBaseUrl
    APP_EMAIL_ENABLED = 'false'
    MANAGEMENT_HEALTH_MAIL_ENABLED = 'false'
    MAIL_HOST = '127.0.0.1'
    MAIL_PORT = '1025'
    MAIL_USERNAME = 'disabled'
    MAIL_PASSWORD = 'disabled'
    MAIL_SMTP_AUTH = 'false'
    MAIL_SMTP_STARTTLS = 'false'
    MAIL_FROM_ADDRESS = 'no-reply@tadamun.invalid'
    MAIL_REPLY_TO = 'support@tadamun.invalid'
    STRIPE_ENABLED = 'false'
    ATTACHMENT_UPLOADS_ENABLED = 'false'
    ATTACHMENT_STORAGE_PATH = '/tmp/crm-attachments'
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE = 'health,info'
}
$lines = @(
    '# PRIVATE: Render backend environment. Never commit or share this file.'
    '# Import with Add from .env. This requires the Aiven-aware Docker entrypoint.'
    '# Replace BOTH frontend URL values after Render assigns the static-site URL.'
    '# Uploads, outbound email, and Stripe are disabled for this free demo.'
)
foreach ($entry in $environment.GetEnumerator()) { $lines += "$($entry.Key)=$($entry.Value)" }
$utf8 = New-Object System.Text.UTF8Encoding($false)
$stream = [System.IO.File]::Open($OutputPath, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::Write, [System.IO.FileShare]::None)
try {
    $bytes = $utf8.GetBytes(($lines -join "`n") + "`n")
    $stream.Write($bytes, 0, $bytes.Length)
} finally { $stream.Dispose() }

Write-Host "Private environment created: $OutputPath"
Write-Host "VARIABLES=$($environment.Count); UPLOADS_DISABLED=True; CERTIFICATE_INCLUDED=True"
Write-Host 'No secret values were printed. This is configuration preparation, not deployment.'
