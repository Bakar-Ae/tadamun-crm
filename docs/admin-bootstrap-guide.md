# Admin Bootstrap Guide

## Current Development Setup

The CRM currently creates a default administrator during database migration.

This is useful for local development, but it is not safe for production.

## Production Risk

A known default administrator account can be dangerous if:

- The default password is public.
- The password is not changed after deployment.
- The application is exposed to the internet.
- The same default credentials are reused across environments.

## Production Rule

For production deployments:

- Do not expose default credentials.
- Force the admin password to be changed after setup.
- Store initial setup secrets outside source code.
- Rotate admin credentials after first login.
- Disable or remove bootstrap behavior after the first administrator exists.

## Future Improvement

A better production setup is:

1. Start the system without users.
2. Allow first-time setup only once.
3. Create the first administrator through a secure setup endpoint or CLI command.
4. Disable setup after the first admin exists.
5. Log the bootstrap event in audit logs.