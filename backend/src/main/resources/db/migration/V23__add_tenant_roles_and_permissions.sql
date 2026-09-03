INSERT INTO roles (name, description, data_scope)
SELECT
    'OWNER',
    'Organization owner with full workspace administration access',
    'ALL'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'OWNER'
);

INSERT INTO permissions (name, description) VALUES
('ORGANIZATION_VIEW', 'View organization settings'),
('ORGANIZATION_UPDATE', 'Update organization settings'),
('MEMBERSHIP_VIEW', 'View organization members and invitations'),
('MEMBERSHIP_INVITE', 'Invite organization members'),
('MEMBERSHIP_UPDATE', 'Update organization memberships and invitations'),
('MEMBERSHIP_DEACTIVATE', 'Deactivate organization memberships'),
('TEAM_MANAGE', 'Manage organization teams');

INSERT INTO role_permissions (role_id, permission_id)
SELECT owner_role.id, permission.id
FROM roles owner_role
CROSS JOIN permissions permission
WHERE owner_role.name = 'OWNER'
  AND permission.name <> 'PERMISSION_MANAGE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name = 'ADMIN'
  AND permission.name IN (
      'ORGANIZATION_VIEW',
      'ORGANIZATION_UPDATE',
      'MEMBERSHIP_VIEW',
      'MEMBERSHIP_INVITE',
      'MEMBERSHIP_UPDATE',
      'MEMBERSHIP_DEACTIVATE',
      'TEAM_MANAGE'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name = 'MANAGER'
  AND permission.name IN (
      'ORGANIZATION_VIEW',
      'MEMBERSHIP_VIEW',
      'TEAM_MANAGE'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('SALES_REP', 'SUPPORT_STAFF')
  AND permission.name = 'ORGANIZATION_VIEW';

UPDATE organization_memberships membership
JOIN organizations organization
  ON organization.id = membership.organization_id
JOIN roles owner_role
  ON owner_role.name = 'OWNER'
SET membership.role_id = owner_role.id
WHERE membership.user_id = organization.created_by_user_id
  AND membership.status = 'ACTIVE';
