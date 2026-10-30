CREATE TABLE screen_layouts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID         NOT NULL REFERENCES projects(id),
    screen_id    VARCHAR(256) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    layout_json  TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_screen_layouts_project_screen
        UNIQUE (project_id, screen_id)
);
