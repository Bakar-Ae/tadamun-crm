# Phase 52 Permission Model

## Security Model

Tadamun CRM uses role-based access control with granular permissions.

Relationship:

User -> Role -> Permissions

Each user has one role.
Each role can have many permissions.
A permission can belong to many roles.

Direct user permissions are not supported in Version 2.

## Existing Roles

- ADMIN
- MANAGER
- SALES_REP
- SUPPORT_STAFF

## Permission Catalogue

### Users

- USER_VIEW
- USER_CREATE
- USER_UPDATE
- USER_DEACTIVATE
- USER_ROLE_CHANGE

### Customers

- CUSTOMER_VIEW
- CUSTOMER_CREATE
- CUSTOMER_UPDATE
- CUSTOMER_ARCHIVE

### Leads

- LEAD_VIEW
- LEAD_CREATE
- LEAD_UPDATE
- LEAD_CONVERT
- LEAD_ARCHIVE

### Contacts

- CONTACT_VIEW
- CONTACT_CREATE
- CONTACT_UPDATE
- CONTACT_ARCHIVE

### Tasks

- TASK_VIEW
- TASK_CREATE
- TASK_UPDATE
- TASK_ASSIGN
- TASK_COMPLETE

### Notes

- NOTE_VIEW
- NOTE_CREATE
- NOTE_UPDATE

### Reports and Administration

- DASHBOARD_VIEW
- REPORT_VIEW
- REPORT_EXPORT
- AUDIT_LOG_VIEW
- PERMISSION_MANAGE

## Rules

- ADMIN receives every permission.
- MANAGER manages CRM records, tasks, reports, and team activity.
- SALES_REP manages assigned sales records and tasks.
- SUPPORT_STAFF manages customer support records, contacts, notes, and tasks.
- Permission changes must create an audit log.
- Backend permissions are the source of truth.
- Frontend permission checks only control visibility.