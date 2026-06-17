# Force Password Change Guide

## Purpose

The CRM can require users to change their password before using the system.

This protects the system when:

- A default admin account exists
- A temporary password is assigned
- An administrator resets a user's password
- A password may be weak or exposed

## Current Implementation

The `users` table has a `password_change_required` column.

When the flag is `true`:

- Login still succeeds
- The backend returns `passwordChangeRequired: true`
- The frontend redirects the user to `/change-password`
- The user cannot browse other CRM pages first

After password change:

- The password is updated using BCrypt
- `password_change_required` becomes `false`
- Active refresh tokens are revoked
- The frontend updates local storage
- The user is redirected to the dashboard

## Validation

1. Set `password_change_required = true` for a user.
2. Login as that user.
3. Confirm frontend opens `/change-password`.
4. Change password.
5. Confirm dashboard opens.
6. Confirm old password no longer works.
7. Confirm new password works.

## Production Notes

For production, this flag should be used when:

- Creating the first administrator
- Creating users with temporary passwords
- Resetting user passwords
- Responding to suspected credential compromise