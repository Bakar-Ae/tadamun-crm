CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_permission_id
    ON role_permissions(permission_id);

INSERT INTO permissions (name, description) VALUES
('USER_VIEW', 'View users'),
('USER_CREATE', 'Create users'),
('USER_UPDATE', 'Update users'),
('USER_DEACTIVATE', 'Deactivate users'),
('USER_ROLE_CHANGE', 'Change user roles'),

('CUSTOMER_VIEW', 'View customers'),
('CUSTOMER_CREATE', 'Create customers'),
('CUSTOMER_UPDATE', 'Update customers'),
('CUSTOMER_ARCHIVE', 'Archive customers'),

('LEAD_VIEW', 'View leads'),
('LEAD_CREATE', 'Create leads'),
('LEAD_UPDATE', 'Update leads'),
('LEAD_CONVERT', 'Convert leads into customers'),
('LEAD_ARCHIVE', 'Archive leads'),

('CONTACT_VIEW', 'View contacts'),
('CONTACT_CREATE', 'Create contacts'),
('CONTACT_UPDATE', 'Update contacts'),
('CONTACT_ARCHIVE', 'Archive contacts'),

('TASK_VIEW', 'View tasks'),
('TASK_CREATE', 'Create tasks'),
('TASK_UPDATE', 'Update tasks'),
('TASK_ASSIGN', 'Assign tasks'),
('TASK_COMPLETE', 'Complete tasks'),

('NOTE_VIEW', 'View notes'),
('NOTE_CREATE', 'Create notes'),
('NOTE_UPDATE', 'Update notes'),

('DASHBOARD_VIEW', 'View dashboard'),
('REPORT_VIEW', 'View reports'),
('REPORT_EXPORT', 'Export reports'),
('AUDIT_LOG_VIEW', 'View audit logs'),
('PERMISSION_MANAGE', 'Manage role permissions');

-- Administrators receive every permission.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

-- Managers receive CRM, task, and reporting permissions.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.name IN (
      'USER_VIEW',
      'CUSTOMER_VIEW', 'CUSTOMER_CREATE', 'CUSTOMER_UPDATE', 'CUSTOMER_ARCHIVE',
      'LEAD_VIEW', 'LEAD_CREATE', 'LEAD_UPDATE', 'LEAD_CONVERT', 'LEAD_ARCHIVE',
      'CONTACT_VIEW', 'CONTACT_CREATE', 'CONTACT_UPDATE', 'CONTACT_ARCHIVE',
      'TASK_VIEW', 'TASK_CREATE', 'TASK_UPDATE', 'TASK_ASSIGN', 'TASK_COMPLETE',
      'NOTE_VIEW', 'NOTE_CREATE', 'NOTE_UPDATE',
      'DASHBOARD_VIEW', 'REPORT_VIEW', 'REPORT_EXPORT'
  );

-- Sales representatives receive daily sales permissions.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SALES_REP'
  AND p.name IN (
      'CUSTOMER_VIEW', 'CUSTOMER_CREATE', 'CUSTOMER_UPDATE',
      'LEAD_VIEW', 'LEAD_CREATE', 'LEAD_UPDATE', 'LEAD_CONVERT',
      'CONTACT_VIEW', 'CONTACT_CREATE', 'CONTACT_UPDATE',
      'TASK_VIEW', 'TASK_CREATE', 'TASK_UPDATE', 'TASK_COMPLETE',
      'NOTE_VIEW', 'NOTE_CREATE', 'NOTE_UPDATE',
      'DASHBOARD_VIEW'
  );

-- Support staff receive customer support permissions.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPPORT_STAFF'
  AND p.name IN (
      'CUSTOMER_VIEW', 'CUSTOMER_UPDATE',
      'CONTACT_VIEW', 'CONTACT_CREATE', 'CONTACT_UPDATE',
      'TASK_VIEW', 'TASK_CREATE', 'TASK_UPDATE', 'TASK_COMPLETE',
      'NOTE_VIEW', 'NOTE_CREATE', 'NOTE_UPDATE',
      'DASHBOARD_VIEW'
  );