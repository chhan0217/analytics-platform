CREATE TABLE identity_links (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id            UUID      NOT NULL REFERENCES projects(id),
    analytics_user_id     UUID      NOT NULL REFERENCES analytics_users(id),
    anonymous_identity_id UUID      NOT NULL REFERENCES anonymous_identities(id),
    linked_at             TIMESTAMP NOT NULL DEFAULT now(),
    unlinked_at           TIMESTAMP NULL
);

CREATE INDEX idx_identity_links_analytics_user_id ON identity_links(analytics_user_id);
CREATE INDEX idx_identity_links_anonymous_identity_id ON identity_links(anonymous_identity_id);
