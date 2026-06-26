# Tadamun CRM Version 2 Roadmap Plan

## Version 2 Phase Plan

### Phase 51 - Version 2 Requirements Reset

Confirm the final Version 2 feature list before implementation.

Output:

- Confirmed V2 scope
- Updated business rules
- Updated technical risks

### Phase 52 - Advanced Permission Model

Add a real permission model.

Output:

- Permission entity
- Role-permission relationship
- Permission seed data
- Backend permission checks

### Phase 53 - Permission Management UI

Add admin UI for permissions.

Output:

- Role permission page
- Permission display
- Permission assignment flow

### Phase 54 - Email Notification Infrastructure

Prepare real email sending.

Output:

- SMTP configuration
- Email service review
- Safe production configuration

### Phase 55 - Email Templates

Add professional reusable email templates.

Output:

- Password reset email template
- Task assignment email template
- User invitation email template

### Phase 56 - Notification Preferences

Allow users to control notification settings.

Output:

- Notification preferences model
- Preferences UI
- Backend preference checks

### Phase 57 - File Attachment Backend

Support file uploads.

Output:

- Attachment entity
- Upload endpoint
- Download endpoint
- File metadata storage

### Phase 58 - File Attachment Frontend

Add upload/download UI.

Output:

- Attachment list
- Upload button
- Download action
- Delete/archive action if allowed

### Phase 59 - Calendar Foundation

Prepare calendar/reminder logic.

Output:

- Calendar event model
- Reminder fields
- Task due date improvements

### Phase 60 - Calendar UI

Add frontend calendar page.

Output:

- Calendar view
- Upcoming tasks
- Due reminders

### Phase 61 - Advanced Reporting Backend

Add stronger reporting APIs.

Output:

- Date range filters
- Grouped report responses
- Manager report endpoints

### Phase 62 - Advanced Reporting Frontend

Improve reports page.

Output:

- Report filters
- Charts
- Export options
- Manager report views

### Phase 63 - Export Excel/PDF

Add professional exports.

Output:

- Excel export
- PDF export
- Report download UI

### Phase 64 - Team Dashboard

Add manager-focused team dashboard.

Output:

- Team workload
- User activity
- Task completion
- Lead/customer activity

### Phase 65 - Activity Timeline Upgrade

Unify activity history.

Output:

- Customer timeline
- Lead timeline
- Notes/tasks/audit events in one view

### Phase 66 - Search Upgrade

Add global search.

Output:

- Search endpoint
- Frontend command/global search results
- Filters by module

### Phase 67 - Testing Upgrade

Improve test coverage.

Output:

- More backend integration tests
- Security tests
- Frontend build/lint validation checklist

### Phase 68 - CI/CD Pipeline

Add automated checks.

Output:

- GitHub Actions workflow
- Backend test job
- Frontend build/lint job

### Phase 69 - Deployment Hardening

Prepare for real deployment.

Output:

- Production Docker review
- HTTPS/reverse proxy plan
- Environment variable review

### Phase 70 - Version 2 Final Review

Close Version 2.

Output:

- V2 validation
- Updated README
- Updated handover docs
- Release tag `v2.0.0`

## Version 2 Priority Order

Recommended order:

1. Permissions
2. Email
3. Files
4. Reports
5. Team dashboard
6. Global search
7. Testing and deployment hardening

## Version 2 Rule

Do not start Version 3 features until Version 2 is validated and tagged.