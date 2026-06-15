INSERT INTO users (full_name, email, password_hash, role_id, status)
SELECT
    'System Administrator',
    'admin@crm.com',
    '$2a$10$54zLRLz3s4FSrF/FUhv3E.oeKUaCmd6h23xJH4t633sfyJPCDGTwW',
    id,
    'ACTIVE'
FROM roles
WHERE name = 'ADMIN';