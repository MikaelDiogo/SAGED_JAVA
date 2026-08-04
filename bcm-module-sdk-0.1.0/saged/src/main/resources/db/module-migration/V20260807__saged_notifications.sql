CREATE TABLE IF NOT EXISTS saged.system_notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id   UUID,
    message             TEXT NOT NULL,
    type                VARCHAR(64) NOT NULL,
    is_read             BOOLEAN NOT NULL DEFAULT false,
    demand_id           UUID,
    demand_protocol     VARCHAR(64),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notif_recipient ON saged.system_notifications(recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_notif_demand ON saged.system_notifications(demand_id);
