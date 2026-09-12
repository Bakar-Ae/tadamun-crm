# CRM Backup Runbook

## Daily Check

```powershell
$taskName = "Tadamun CRM Backup Verification"
Get-ScheduledTask -TaskName $taskName
Get-ScheduledTaskInfo -TaskName $taskName
```

A healthy task is `Ready`, has `LastTaskResult` equal to `0`, and has a future
`NextRunTime`. The latest JSON file in `logs/backup-verification/` must report
`Result` as `SUCCESS`.

## Manual Run

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Invoke-CrmBackupVerification.ps1
```

The live database is read only during the dump. Restore verification happens in
a temporary MySQL container and never writes to `crm_mysql`.

## Failure Response

1. Read the newest failure JSON in `logs/backup-verification/`.
2. Confirm Docker Desktop is running and `crm_mysql` is healthy.
3. Check available disk space and Docker logs.
4. Run the verifier manually and retain its console error.
5. Keep the newest previously verified backup while investigating.

## Phase 89 Restore Proof

- Verified: 2026-09-12 16:20 Africa/Mogadishu.
- Scheduled task result: `0` (`Ready`).
- Restored tables: `44`.
- Latest Flyway version: `35`.
- Required critical tables found: `5`.
- Backup size: `132335` bytes.
- The isolated restore container was removed after verification.
