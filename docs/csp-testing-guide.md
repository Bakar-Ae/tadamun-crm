# CSP Testing Guide

## Goal

Test Content Security Policy safely before enforcing it.

## Recommended Approach

Use `Content-Security-Policy-Report-Only` first.

This lets the browser report violations without blocking the application.

## What To Test

- Login page loads
- Login request works
- Dashboard loads
- Sidebar navigation works
- Tables/pages load
- API calls work
- Static assets load
- Browser console has no CSP errors

## Local Suggested Report-Only Policy

```nginx
add_header Content-Security-Policy-Report-Only "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self' http://localhost:8081; frame-ancestors 'none';" always;
```

## Production Notes

For production, replace `http://localhost:8081` with the real backend API origin.

Do not enforce CSP until the report-only policy is tested.