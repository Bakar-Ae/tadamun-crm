# Content Security Policy Plan

## Purpose

Content Security Policy helps reduce the risk of cross-site scripting and unwanted external resource loading.

## Why We Are Planning First

CSP can break frontend applications if it is too strict.

The CRM frontend loads:

- JavaScript bundles
- CSS assets
- API requests to the backend
- Static assets from the frontend container

## Initial Policy Direction

For production, start with a conservative policy:

- default-src 'self'
- script-src 'self'
- style-src 'self' 'unsafe-inline'
- img-src 'self' data:
- font-src 'self'
- connect-src 'self' backend-domain
- frame-ancestors 'none'

## Notes

`style-src 'unsafe-inline'` may be needed because Tailwind/Vite styling can require inline style behavior depending on build output and browser behavior.

`connect-src` must include the backend API origin.

## Rollout Plan

1. Test CSP locally in report-only mode if possible.
2. Add CSP to frontend Nginx.
3. Confirm login works.
4. Confirm dashboard loads.
5. Confirm API calls work.
6. Confirm browser console has no CSP violations.
7. Then commit the enforced policy.