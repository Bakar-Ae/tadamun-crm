# Security Headers Review

## Current Backend Headers

Spring Security config currently sets:

- Frame Options: DENY
- Content Type Options
- Referrer Policy: no-referrer
- Permissions Policy: disables geolocation, microphone, and camera

## Purpose

Security headers reduce browser-based attack risk.

They help defend against:

- Clickjacking
- MIME sniffing
- Referrer leakage
- Unwanted browser permissions

## Validation

Use browser dev tools or PowerShell to inspect response headers.

Example:

```powershell
Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -Method Get