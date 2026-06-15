CREATE TABLE leads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(50),
    company_name VARCHAR(150),
    source VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    estimated_value DECIMAL(12, 2),
    assigned_to_user_id BIGINT,
    converted_customer_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_leads_assigned_user
        FOREIGN KEY (assigned_to_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_leads_converted_customer
        FOREIGN KEY (converted_customer_id)
        REFERENCES customers(id),

    CONSTRAINT chk_leads_status
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'LOST', 'CONVERTED', 'ARCHIVED'))
);

CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_assigned_to_user_id ON leads(assigned_to_user_id);
CREATE INDEX idx_leads_converted_customer_id ON leads(converted_customer_id);