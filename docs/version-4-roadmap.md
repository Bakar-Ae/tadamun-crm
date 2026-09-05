# Tadamun CRM Version 4 Roadmap

Version 4 begins only after Version 3 is released and stable. Its focus is intelligent assistance, advanced automation, broader integrations, mobile usability, and enterprise readiness.

## Phase 91 - AI Readiness and Governance

- Provider-independent AI service boundary
- Permission-aware data access
- Consent, retention, and audit rules
- Prompt-injection and sensitive-data protections

## Phase 92 - Explainable Lead Scoring

- Configurable scoring factors
- Score history and explanations
- Qualification recommendations
- Human review and override controls

## Phase 93 - Customer Insights

- Account health indicators
- Engagement summaries
- Churn-risk signals
- Recommended follow-up actions

## Phase 94 - Predictive Sales Analytics

- Pipeline forecasting
- Conversion trends
- Forecast confidence and explanations
- Model-quality monitoring

## Phase 95 - CRM Assistant

- Permission-aware conversational search
- Record summaries
- Drafted follow-ups and task suggestions
- Explicit confirmation before data-changing actions

## Phase 96 - Visual Workflow Builder

- No-code triggers, conditions, and actions
- Workflow validation and versioning
- Execution history and failure recovery
- Templates for common CRM processes

## Phase 97 - Integration Marketplace

- Provider connector framework
- Tenant-controlled authorization
- Connector health and audit history
- Initial email, calendar, messaging, and webhook connectors

## Phase 98 - Installable Progressive Web App

Goal: make the existing frontend installable and app-like on supported desktop and mobile devices without duplicating the CRM codebase.

- Web app manifest with Tadamun name, icons, theme colors, and display mode
- Install experience for supported browsers
- Branded application icons and splash presentation
- Service worker with safe update handling
- Offline shell and explicit offline/error states; CRM writes remain online-only unless a later synchronization design is approved
- Push-notification readiness with tenant and user preference enforcement
- Responsive touch targets, mobile navigation, and device-safe layouts
- PWA installation, update, cache, accessibility, and mobile-browser tests
- Production HTTPS and caching verification

Native Android and iOS applications remain optional follow-up work. They should be approved only when user research identifies requirements a PWA cannot satisfy.

## Phase 99 - Enterprise Scale and Compliance

- Single sign-on foundations
- Tenant-aware observability
- Performance and load validation
- Security, privacy, retention, and compliance controls

## Phase 100 - Version 4 Release

- Full regression and tenant-isolation testing
- Production migration rehearsal
- Operational and user documentation
- Release audit, deployment, and `v4.0.0` tag

## Delivery Rules

- Finish and stabilize Version 3 before starting Version 4 implementation.
- AI output must be explainable and must respect existing permissions and tenant boundaries.
- No AI-generated mutation may run without explicit user confirmation.
- The PWA must not cache secrets or silently queue unsafe CRM mutations.
- Native mobile applications are not part of the Version 4 commitment unless separately approved.
- Test and commit each stable phase independently.
