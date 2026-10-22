CREATE TABLE analytics_events (
    id                   UUID PRIMARY KEY,
    project_id           UUID         NOT NULL REFERENCES projects(id),
    application_id       UUID         NOT NULL REFERENCES applications(id),
    session_id           UUID         NOT NULL,
    analytics_user_id    UUID         NULL REFERENCES analytics_users(id),
    anonymous_id         UUID         NOT NULL,
    event_key            VARCHAR(128) NOT NULL,
    event_schema_version INTEGER      NOT NULL DEFAULT 1,
    properties           JSONB        NOT NULL DEFAULT '{}',
    context              JSONB        NOT NULL DEFAULT '{}',
    occurred_at          TIMESTAMP    NOT NULL,
    received_at          TIMESTAMP    NOT NULL DEFAULT now(),
    sequence_number      INTEGER      NULL
);

CREATE INDEX idx_analytics_events_project_occurred
    ON analytics_events(project_id, occurred_at);
CREATE INDEX idx_analytics_events_application_occurred
    ON analytics_events(application_id, occurred_at);
CREATE INDEX idx_analytics_events_project_session_occurred
    ON analytics_events(project_id, session_id, occurred_at);
CREATE INDEX idx_analytics_events_project_user_occurred
    ON analytics_events(project_id, analytics_user_id, occurred_at);
CREATE INDEX idx_analytics_events_project_event_key
    ON analytics_events(project_id, event_key);
