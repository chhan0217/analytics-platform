CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID         NOT NULL REFERENCES organizations(id),
    name            VARCHAR(256) NOT NULL,
    unregistered_event_policy VARCHAR(20) NOT NULL DEFAULT 'PERMISSIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_organization_id ON projects(organization_id);
