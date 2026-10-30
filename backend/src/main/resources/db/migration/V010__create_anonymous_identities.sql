CREATE TABLE anonymous_identities (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID      NOT NULL REFERENCES projects(id),
    anonymous_id UUID      NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (project_id, anonymous_id)
);

CREATE INDEX idx_anonymous_identities_project_id ON anonymous_identities(project_id);
