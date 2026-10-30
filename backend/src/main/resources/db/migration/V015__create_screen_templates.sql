CREATE TABLE screen_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID         NOT NULL REFERENCES projects(id),
    application_id  UUID         NOT NULL REFERENCES applications(id),
    screen_id       VARCHAR(256) NOT NULL,
    app_version     VARCHAR(64)  NOT NULL,
    viewport_width  INTEGER      NOT NULL,
    viewport_height INTEGER      NOT NULL,
    wireframe       JSONB        NOT NULL,
    captured_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_screen_templates_version
        UNIQUE (project_id, application_id, screen_id, app_version)
);

CREATE INDEX idx_screen_templates_lookup
    ON screen_templates (project_id, application_id, app_version);
