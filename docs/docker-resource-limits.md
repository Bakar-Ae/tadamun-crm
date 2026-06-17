# Docker Resource Limits

## Purpose

Docker resource limits prevent one container from consuming all server memory.

## Current Limits

- MySQL: 1 GB
- Backend: 768 MB
- Frontend: 256 MB

## Why This Matters

Without limits, a memory leak or heavy query can affect the whole VPS.

## Validation

Run:

```powershell
docker compose config
docker ps