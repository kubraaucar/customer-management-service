-- Accounts that sign in with a password. The administrator is created at startup
-- from the app.security.admin.* settings (see AdminAccountInitializer).
CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'VIEWER'))
);
