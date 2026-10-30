CREATE TABLE analytics_sessions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id        UUID      NOT NULL REFERENCES projects(id),
    application_id    UUID      NOT NULL REFERENCES applications(id),
    anonymous_id      UUID      NOT NULL,
    analytics_user_id UUID      NULL REFERENCES analytics_users(id),
    started_at        TIMESTAMP NOT NULL,
    last_event_at     TIMESTAMP NOT NULL,
    ended_at          TIMESTAMP NULL
);

CREATE INDEX idx_analytics_sessions_project_id ON analytics_sessions(project_id);
CREATE INDEX idx_analytics_sessions_application_id ON analytics_sessions(application_id);
CREATE INDEX idx_analytics_sessions_analytics_user_id ON analytics_sessions(analytics_user_id);
