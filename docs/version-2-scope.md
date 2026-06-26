# Tadamun CRM Version 2 Scope

## Goal

Version 2 will improve the CRM from a working Version 1 system into a stronger business-ready application.

Version 2 focuses on permissions, communication, documents, reporting, search, and team visibility.

## Included In Version 2

### 1. Advanced Permissions

Move beyond simple role checks.

Examples:

- `USER_CREATE`
- `USER_UPDATE`
- `USER_DEACTIVATE`
- `CUSTOMER_CREATE`
- `CUSTOMER_UPDATE`
- `CUSTOMER_ARCHIVE`
- `LEAD_CONVERT`
- `REPORT_VIEW`
- `AUDIT_LOG_VIEW`

### 2. Permission Management UI

Administrators can view and manage permissions assigned to roles.

### 3. Email Notifications

Send real emails for important actions.

Examples:

- Password reset
- User invitation
- Task assignment
- Task due reminders

### 4. Email Templates

Professional reusable email templates for system emails.

### 5. File Attachments

Allow files to be attached to:

- Customers
- Leads
- Tasks
- Notes

### 6. Advanced Reports

Add date filters, report categories, and better export options.

Examples:

- Lead report
- Customer report
- Task report
- User activity report

### 7. Team Dashboard

Manager-focused dashboard showing team activity and workload.

### 8. Global Search

Search across:

- Customers
- Leads
- Contacts
- Tasks
- Notes

### 9. Activity Timeline Upgrade

Improve customer and lead timelines with related notes, tasks, contacts, and audit events.

### 10. Testing Upgrade

Add more integration tests and frontend validation checks.

## Out Of Scope For Version 2

These are delayed to Version 3 or later:

- Multi-tenancy
- SaaS billing
- Public API
- Mobile application
- AI features
- Workflow automation
- Third-party integrations

## Version 2 Success Criteria

Version 2 is complete when:

- Permissions are more granular than roles
- Real email sending works
- Files can be uploaded and downloaded safely
- Reports are useful for managers
- Team dashboard is available
- Global search works across core CRM records
- Tests cover important business flows