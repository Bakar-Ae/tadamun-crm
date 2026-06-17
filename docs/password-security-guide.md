# Password Security Guide

## Rules

- Never commit real passwords.
- Never display demo passwords on the login page.
- Use BCrypt for password hashing.
- Require current password before password change.
- Require strong new passwords.
- Revoke refresh tokens after password change.
- Remove saved browser passwords if they were exposed or weak.
- Use environment variables for production secrets.

## Current Implementation

The CRM supports:

- BCrypt password hashing
- Authenticated password change
- Password confirmation validation
- Refresh token revocation after password change
- Login rate limiting
- JWT authentication

## Manual Validation

1. Login with the current password.
2. Change password from the Change Password page.
3. Confirm old password no longer works.
4. Confirm new password works.
5. Confirm user can continue using the CRM after logging in again.

## Production Notes

The default admin account should not use a public or common password.

For real deployment:

- Create admin password during setup.
- Store secrets in environment variables.
- Do not expose credentials in frontend code.
- Rotate passwords after first deployment.