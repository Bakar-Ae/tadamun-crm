# VPS Deployment Plan

## Recommended Deployment Type

Use one VPS with Docker Compose for Version 1.

## Recommended VPS Requirements

Minimum:

- 2 CPU cores
- 4 GB RAM
- 40 GB SSD
- Ubuntu 24.04 LTS

Better:

- 4 CPU cores
- 8 GB RAM
- 80 GB SSD

## Recommended Providers

- Hetzner
- DigitalOcean
- AWS Lightsail
- Azure VM
- Vultr

## Server Software

Install:

- Docker
- Docker Compose
- Git
- Nginx
- Certbot

## Production Services

Docker Compose will run:

- frontend
- backend
- mysql

Nginx will handle:

- HTTP
- HTTPS
- reverse proxy
- domain routing

## Domain Setup

Example domain:

```text
crm.example.com