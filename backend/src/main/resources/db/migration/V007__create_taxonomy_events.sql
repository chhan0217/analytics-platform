CREATE TABLE taxonomy_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     UUID         NOT NULL REFERENCES projects(id),
    event_key      VARCHAR(128) NOT NULL,
    display_name   VARCHAR(256) NOT NULL,
    description    TEXT         NULL,
    schema_version INTEGER      NOT NULL DEFAULT 1,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    is_system      BOOLEAN      NOT NULL DEFAULT false,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (project_id, event_key),
    CONSTRAINT chk_taxonomy_events_status CHECK (status IN ('ACTIVE', 'DEPRECATED', 'ARCHIVED'))
);

CREATE INDEX idx_taxonomy_events_project_id ON taxonomy_events(project_id);
