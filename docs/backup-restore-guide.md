# Backup And Restore Guide

## Goal

Protect CRM data stored in MySQL.

## Backup Command

Run from project root on server:

```bash
docker exec crm_mysql mysqldump -u root -p crm_db > backup.sql