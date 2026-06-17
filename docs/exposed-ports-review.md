# Exposed Ports Review

## Current Local Ports

- Frontend: 5173
- Backend: 8081
- MySQL: 3307

## Local Development

These ports are acceptable for local Docker development.

## Production Recommendation

In production:

- Expose frontend through HTTPS reverse proxy.
- Expose backend only through reverse proxy or private network.
- Do not expose MySQL publicly.
- Restrict server firewall rules.
- Use HTTPS for browser traffic.

## Future Improvement

Create a separate production Compose file that does not publish MySQL to the internet.