CREATE TABLE taxonomy_properties (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    taxonomy_event_id UUID         NOT NULL REFERENCES taxonomy_events(id) ON DELETE CASCADE,
    property_key      VARCHAR(128) NOT NULL,
    value_type        VARCHAR(20)  NOT NULL,
    required          BOOLEAN      NOT NULL DEFAULT false,
    description       TEXT         NULL,
    UNIQUE (taxonomy_event_id, property_key),
    CONSTRAINT chk_taxonomy_properties_value_type CHECK (
        value_type IN ('STRING', 'INTEGER', 'FLOAT', 'BOOLEAN', 'ARRAY', 'OBJECT')
    )
);

CREATE INDEX idx_taxonomy_properties_taxonomy_event_id ON taxonomy_properties(taxonomy_event_id);
