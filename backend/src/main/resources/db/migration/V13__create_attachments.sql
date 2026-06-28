CREATE TABLE attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(100) NOT NULL UNIQUE,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,

    customer_id BIGINT NULL,
    lead_id BIGINT NULL,
    uploaded_by_user_id BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at DATETIME NULL,
    deleted_by_user_id BIGINT NULL,

    CONSTRAINT fk_attachments_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id),

    CONSTRAINT fk_attachments_lead
        FOREIGN KEY (lead_id) REFERENCES leads(id),

    CONSTRAINT fk_attachments_uploaded_by
        FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id),

    CONSTRAINT fk_attachments_deleted_by
        FOREIGN KEY (deleted_by_user_id) REFERENCES users(id),

    CONSTRAINT chk_attachments_parent
        CHECK (
            (customer_id IS NOT NULL AND lead_id IS NULL)
            OR
            (customer_id IS NULL AND lead_id IS NOT NULL)
        ),

    CONSTRAINT chk_attachments_size
        CHECK (size_bytes > 0)
);

CREATE INDEX idx_attachments_customer
    ON attachments(customer_id, status, created_at);

CREATE INDEX idx_attachments_lead
    ON attachments(lead_id, status, created_at);

INSERT INTO permissions (name, description) VALUES
('ATTACHMENT_VIEW', 'View and download attachments'),
('ATTACHMENT_UPLOAD', 'Upload attachments'),
('ATTACHMENT_DELETE', 'Delete attachments');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name IN ('ADMIN', 'MANAGER')
  AND p.name IN (
      'ATTACHMENT_VIEW',
      'ATTACHMENT_UPLOAD',
      'ATTACHMENT_DELETE'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name IN ('SALES_REP', 'SUPPORT_STAFF')
  AND p.name IN (
      'ATTACHMENT_VIEW',
      'ATTACHMENT_UPLOAD'
  );