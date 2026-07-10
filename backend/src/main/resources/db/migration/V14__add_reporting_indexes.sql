CREATE INDEX idx_customers_created_at
    ON customers(created_at);

CREATE INDEX idx_leads_created_at_status
    ON leads(created_at, status);

CREATE INDEX idx_tasks_created_at_status
    ON tasks(created_at, status);

CREATE INDEX idx_tasks_created_at_priority
    ON tasks(created_at, priority);

CREATE INDEX idx_audit_logs_action_created_at
    ON audit_logs(action, created_at);

CREATE INDEX idx_audit_logs_entity_type_created_at
    ON audit_logs(entity_type, created_at);
