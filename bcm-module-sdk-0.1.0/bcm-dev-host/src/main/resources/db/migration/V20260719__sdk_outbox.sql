CREATE SCHEMA IF NOT EXISTS sdk;

CREATE TABLE IF NOT EXISTS sdk.outbox_event (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(128) NOT NULL,
    aggregate_id    VARCHAR(64)  NOT NULL,
    event_type      VARCHAR(128) NOT NULL,
    payload         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
