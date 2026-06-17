# API Error Exposure Review

## Purpose

Production APIs should return useful errors without leaking sensitive internal details.

## Current Error Handling

The CRM uses a global exception handler for:

- Validation errors
- Bad requests
- Too many login attempts

The API returns structured JSON with:

- timestamp
- status
- error
- message
- fieldErrors when validation fails

## Security Considerations

Avoid exposing:

- Stack traces
- SQL errors
- Java class names
- Internal server paths
- Database constraint names
- Authentication internals

## Current Safe Practices

- Login failures use a generic invalid credentials message.
- Validation errors return field-level messages.
- Rate limiting returns HTTP 429.
- Health endpoint does not expose full sensitive details.

## Future Improvement

Add a generic handler for unexpected exceptions that returns:

- status: 500
- error: Internal Server Error
- message: Unexpected server error

The server logs should contain the real exception, but the client should not.