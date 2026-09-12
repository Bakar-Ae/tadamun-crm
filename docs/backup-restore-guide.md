# Backup and Restore Guide

## Objectives

- Recovery point objective (RPO): no more than 24 hours of data loss.
- Recovery time objective (RTO): restore a verified backup within 2 hours.
- Never overwrite the active CRM database while testing a backup.

## Automated Verification

The verifier creates a transaction-consistent dump from the healthy
`crm_mysql` container, validates its structure and completion marker, restores
it into a disposable MySQL 8.4 container, checks the schema and Flyway history,
and removes the disposable container.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Invoke-CrmBackupVerification.ps1
```

Backups are written to `backups/` and JSON evidence to
`logs/backup-verification/`. Both locations are ignored by Git. Successful
runs retain timestamped backup and evidence files for 30 days.

## Daily Schedule

Register or update the current user's daily Windows task:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\Register-CrmBackupVerificationTask.ps1 `
  -At "20:00"
```

The task runs when the user is logged in, starts after a missed trigger when the
computer becomes available, and may run while the laptop is on battery. Docker
Desktop and the `crm_mysql` container must be running; otherwise the verifier
records failure evidence and exits nonzero.

## Recovery Procedure

1. Select the newest backup whose JSON evidence has `Result: SUCCESS`.
2. Copy both files to protected storage before making recovery changes.
3. Stop application writes.
4. Restore into a new database or isolated MySQL container first.
5. Confirm the latest Flyway version and critical record counts.
6. Point the backend at the restored database only after validation.
7. Run authentication, tenant-isolation, customer, lead, and billing smoke
   tests before reopening writes.

Backups contain customer and authentication data. Restrict access, encrypt
off-machine copies, and never commit them to source control.
