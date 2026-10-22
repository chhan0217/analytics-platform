CREATE TABLE organizations (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(256) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);
