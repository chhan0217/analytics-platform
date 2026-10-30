CREATE TABLE admin_audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id UUID         NOT NULL REFERENCES admin_users(id),
    action        VARCHAR(64)  NOT NULL,
    target_type   VARCHAR(64)  NOT NULL,
    target_id     UUID         NULL,
    details       JSONB        NULL,
    occurred_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_audit_logs_admin_user_id ON admin_audit_logs(admin_user_id);
CREATE INDEX idx_admin_audit_logs_occurred_at ON admin_audit_logs(occurred_at);
