[CmdletBinding()]
param(
    [ValidatePattern('^([01]\d|2[0-3]):[0-5]\d$')]
    [string]$At = '20:00',

    [ValidateNotNullOrEmpty()]
    [string]$TaskName = 'Tadamun CRM Backup Verification'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$verificationScript = Join-Path $PSScriptRoot 'Invoke-CrmBackupVerification.ps1'
if (-not (Test-Path -LiteralPath $verificationScript -PathType Leaf)) {
    throw "Backup verifier not found: $verificationScript"
}

$actionArguments = "-NoProfile -NonInteractive -ExecutionPolicy Bypass -File `"$verificationScript`""
$action = New-ScheduledTaskAction `
    -Execute 'powershell.exe' `
    -Argument $actionArguments `
    -WorkingDirectory $repoRoot
$trigger = New-ScheduledTaskTrigger -Daily -At $At
$settings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit (New-TimeSpan -Hours 1) `
    -MultipleInstances IgnoreNew
$principal = New-ScheduledTaskPrincipal `
    -UserId ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) `
    -LogonType Interactive `
    -RunLevel Limited

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Principal $principal `
    -Description 'Creates and restores an isolated Tadamun CRM MySQL backup, then records verification evidence.' `
    -Force | Out-Null

$task = Get-ScheduledTask -TaskName $TaskName
$taskInfo = Get-ScheduledTaskInfo -TaskName $TaskName
[pscustomobject]@{
    TaskName    = $task.TaskName
    State       = $task.State
    DailyAt     = $At
    NextRunTime = $taskInfo.NextRunTime
} | Format-List
