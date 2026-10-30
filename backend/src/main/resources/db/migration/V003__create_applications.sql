CREATE TABLE applications (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     UUID         NOT NULL REFERENCES projects(id),
    name           VARCHAR(256) NOT NULL,
    platform       VARCHAR(20)  NOT NULL,
    app_identifier VARCHAR(256) NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_applications_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB'))
);

CREATE INDEX idx_applications_project_id ON applications(project_id);
