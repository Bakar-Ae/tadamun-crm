# Frontend Nginx Security Review

## Purpose

The frontend container serves static React files through Nginx.

Nginx should set browser security headers for frontend responses.

## Headers To Consider

- X-Frame-Options
- X-Content-Type-Options
- Referrer-Policy
- Permissions-Policy
- Content-Security-Policy

## Current Status

Frontend Nginx configuration should be reviewed before production hardening.

## Production Note

Content-Security-Policy should be added carefully because incorrect CSP rules can break frontend assets or API calls.