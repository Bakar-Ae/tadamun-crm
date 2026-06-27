# Phase 53 Permission Management Design

## Purpose

Allow authorized administrators to manage permissions assigned to CRM roles.

## Security Rules

- All endpoints require PERMISSION_MANAGE.
- ADMIN permissions are immutable.
- Unknown permission names are rejected.
- Permission changes are performed in one database transaction.
- Every change creates an audit log.
- Backend authorization remains the source of truth.

## API Contract

### List Permission Catalogue

GET /api/v1/permissions

Returns every available permission with its description.

### List Roles and Permissions

GET /api/v1/roles

Returns:

- Role name
- Role description
- Whether the role is editable
- Assigned permissions

### Update Role Permissions

PUT /api/v1/roles/{roleName}/permissions

Request:

{
  "permissionNames": [
    "CUSTOMER_VIEW",
    "CUSTOMER_CREATE",
    "TASK_VIEW"
  ]
}

Rules:

- ADMIN cannot be modified.
- Duplicate values are ignored.
- Unknown permissions return 400.
- The complete old permission set is replaced.
- Added and removed permissions are written to audit logs.

## Audit Event

Action: ROLE_PERMISSIONS_UPDATED
Entity type: ROLE
Entity ID: role database ID

Details include:

- Role name
- Added permissions
- Removed permissions
- User who performed the change

## Frontend UX

- Add Roles & Permissions under administration.
- Show one selected role at a time.
- Group permissions by module.
- Show ADMIN as read-only.
- Display unsaved changes clearly.
- Require confirmation before saving.
- Show success or error feedback.
- Never use raw database IDs.