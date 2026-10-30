CREATE TABLE project_memberships (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id UUID        NOT NULL REFERENCES admin_users(id),
    project_id    UUID        NOT NULL REFERENCES projects(id),
    role          VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at    TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE (admin_user_id, project_id),
    CONSTRAINT chk_project_memberships_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

CREATE INDEX idx_project_memberships_admin_user_id ON project_memberships(admin_user_id);
CREATE INDEX idx_project_memberships_project_id ON project_memberships(project_id);
