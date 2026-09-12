[CmdletBinding()]
param(
    [ValidateRange(1, 50)]
    [int]$VirtualUsers = 5,

    [ValidatePattern('^\d+[smh]$')]
    [string]$Duration = '30s'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$resultDirectory = Join-Path $repoRoot 'tmp/load-test'
$queryFile = Join-Path $repoRoot 'performance/mysql/top-query-digests.sql'

Push-Location $repoRoot
try {
    $backendHealth = (& docker inspect `
        --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' `
        crm_backend 2>$null).Trim()
    if ($LASTEXITCODE -ne 0 -or $backendHealth -ne 'healthy') {
        throw 'crm_backend must be running and healthy before the load test.'
    }

    New-Item -ItemType Directory -Force -Path $resultDirectory | Out-Null
    $env:LOAD_TEST_VUS = [string]$VirtualUsers
    $env:LOAD_TEST_DURATION = $Duration

    Write-Host "Running read-only CRM load test: $VirtualUsers VUs for $Duration"
    & docker compose --profile performance run --rm load-test
    if ($LASTEXITCODE -ne 0) {
        throw 'The load test failed or breached a performance threshold.'
    }

    Write-Host ''
    Write-Host 'Top MySQL query digests (historical totals, read-only):'
    Get-Content -Raw -LiteralPath $queryFile |
        & docker compose exec -T mysql sh -c `
            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --batch --raw -uroot "$MYSQL_DATABASE"'
    if ($LASTEXITCODE -ne 0) {
        Write-Warning 'The load test passed, but the MySQL digest query failed.'
    }
}
finally {
    Remove-Item Env:LOAD_TEST_VUS -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_DURATION -ErrorAction SilentlyContinue
    Pop-Location
}
