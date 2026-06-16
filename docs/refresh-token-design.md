# Refresh Token Design

## Goal

Improve authentication security and user experience.

## Current State

The CRM currently uses one JWT access token.

Limitations:

- User must log in again after access token expires
- Logout only clears token on frontend
- Backend cannot revoke access tokens easily

## Target Design

Use two tokens:

- Access token: short-lived JWT
- Refresh token: long-lived random token stored by hash in database

## Token Lifetimes

Recommended Version 2:

- Access token: 15 minutes
- Refresh token: 7 days

## Database Table

Table name:

```text
refresh_tokens