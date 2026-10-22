CREATE TABLE admin_users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(256) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    name          VARCHAR(128) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);
