CREATE TABLE IF NOT EXISTS saged.demand_views (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    demand_id       UUID NOT NULL REFERENCES saged.demands(id) ON DELETE CASCADE,
    viewer_user_id  UUID NOT NULL,
    viewer_name     VARCHAR(255),
    viewed_at       TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0,
    UNIQUE (demand_id, viewer_user_id)
);
