CREATE TABLE analytics_users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id       UUID         NOT NULL REFERENCES projects(id),
    external_user_id VARCHAR(256) NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_analytics_users_project_external
    ON analytics_users(project_id, external_user_id)
    WHERE external_user_id IS NOT NULL;

CREATE INDEX idx_analytics_users_project_id ON analytics_users(project_id);
