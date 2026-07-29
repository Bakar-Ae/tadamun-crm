INSERT INTO organizations (
    name,
    slug,
    status,
    time_zone,
    created_by_user_id
)
SELECT
    'Tadamun Business Solutions',
    'tadamun',
    'ACTIVE',
    'Africa/Mogadishu',
    u.id
FROM users u
JOIN roles r ON r.id = u.role_id
WHERE r.name = 'ADMIN'
  AND u.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM organizations WHERE slug = 'tadamun'
  )
ORDER BY u.id
LIMIT 1;

INSERT INTO organization_memberships (
    organization_id,
    user_id,
    role_id,
    status,
    joined_at,
    created_at,
    updated_at
)
SELECT
    o.id,
    u.id,
    u.role_id,
    CASE
        WHEN u.status = 'ACTIVE' THEN 'ACTIVE'
        ELSE 'INACTIVE'
    END,
    u.created_at,
    u.created_at,
    u.updated_at
FROM users u
JOIN organizations o ON o.slug = 'tadamun'
WHERE NOT EXISTS (
    SELECT 1
    FROM organization_memberships om
    WHERE om.organization_id = o.id
      AND om.user_id = u.id
);