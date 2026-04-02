-- ── Seed users ─────────────────────────────────────────────────
-- Passwords are BCrypt(cost=12). Values shown in comments — change before production.

-- admin@pms.ie  /  Admin@1234!
INSERT INTO users (email, password_hash, domain_id)
VALUES ('admin@pms.ie',
        '$2a$12$RV2kJZDxhksjRWMDZJHbNOQF5pjgXQ9X9rTflGl7CrJMp5A9n0hGG',
        NULL);

-- dr.smith@pms.ie  /  Doctor@1234!   (domain_id = 1 → ms-doctor row id=1)
INSERT INTO users (email, password_hash, domain_id)
VALUES ('dr.smith@pms.ie',
        '$2a$12$XtSuMK8k3bCyqQ6lW8fYhOJ0mOiMVvVlGqB8Z7pRXeAAyXEOhnZrC',
        1);

-- john.doe@pms.ie  /  Patient@1234!  (domain_id = 1 → ms-patient row id=1)
INSERT INTO users (email, password_hash, domain_id)
VALUES ('john.doe@pms.ie',
        '$2a$12$L9kP2YtVqB3mNxJ8dFgHuOEwZs4iKlRvXcA7nWM6eTbCpQjYaDIK.',
        1);