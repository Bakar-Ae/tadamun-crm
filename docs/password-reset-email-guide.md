# Password Reset and Email Guide

## Purpose

The CRM supports password reset using secure reset tokens.

This feature allows users to recover access when they forget their password.

## Current Local Development Behavior

In local development, real email sending is disabled.

The backend prints the password reset link in Docker logs.

Configuration:

```properties
APP_EMAIL_ENABLED=false
MANAGEMENT_HEALTH_MAIL_ENABLED=false