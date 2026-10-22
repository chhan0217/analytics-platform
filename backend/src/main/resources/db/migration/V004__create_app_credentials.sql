CREATE TABLE app_credentials (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID        NOT NULL REFERENCES applications(id),
    key_prefix     VARCHAR(12) NOT NULL,
    key_hash       VARCHAR(64) NOT NULL UNIQUE,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP   NOT NULL DEFAULT now(),
    revoked_at     TIMESTAMP   NULL,
    CONSTRAINT chk_app_credentials_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE INDEX idx_app_credentials_application_id ON app_credentials(application_id);
CREATE INDEX idx_app_credentials_key_hash ON app_credentials(key_hash);
