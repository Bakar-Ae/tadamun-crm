CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    due_date DATETIME,
    assigned_to_user_id BIGINT,
    customer_id BIGINT,
    lead_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_tasks_assigned_user
        FOREIGN KEY (assigned_to_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_tasks_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT fk_tasks_lead
        FOREIGN KEY (lead_id)
        REFERENCES leads(id),

    CONSTRAINT chk_tasks_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),

    CONSTRAINT chk_tasks_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

CREATE TABLE notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    customer_id BIGINT,
    lead_id BIGINT,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_notes_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT fk_notes_lead
        FOREIGN KEY (lead_id)
        REFERENCES leads(id),

    CONSTRAINT fk_notes_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id),

    CONSTRAINT chk_notes_owner
        CHECK (
            (customer_id IS NOT NULL AND lead_id IS NULL)
            OR
            (customer_id IS NULL AND lead_id IS NOT NULL)
        )
);

CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_assigned_to_user_id ON tasks(assigned_to_user_id);
CREATE INDEX idx_tasks_customer_id ON tasks(customer_id);
CREATE INDEX idx_tasks_lead_id ON tasks(lead_id);

CREATE INDEX idx_notes_customer_id ON notes(customer_id);
CREATE INDEX idx_notes_lead_id ON notes(lead_id);
CREATE INDEX idx_notes_created_by_user_id ON notes(created_by_user_id);