# Monitoring And Logging Plan

## Goal

Track CRM health, errors, and important system behavior.

## What To Monitor

Backend:

- application startup
- API errors
- authentication failures
- authorization failures
- database connection errors
- slow requests

Database:

- container health
- disk usage
- backup success
- connection failures

Frontend:

- container health
- Nginx errors
- failed asset loading

Server:

- CPU usage
- memory usage
- disk usage
- network usage

## Current Log Commands

Backend:

```powershell
docker logs crm_backend