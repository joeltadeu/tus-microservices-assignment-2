INSERT INTO user_roles (user_id, role_id)
VALUES (1, (SELECT id FROM roles WHERE name = 'ROLE_HEALTHCARE_ADMIN')),
       (2, (SELECT id FROM roles WHERE name = 'ROLE_DOCTOR')),
       (3, (SELECT id FROM roles WHERE name = 'ROLE_PATIENT'));