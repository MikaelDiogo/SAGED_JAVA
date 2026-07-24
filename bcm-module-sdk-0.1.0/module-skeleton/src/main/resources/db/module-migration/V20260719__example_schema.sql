-- Template migration. Rename schema to your module id (English) when you replace example.
CREATE SCHEMA IF NOT EXISTS example;

CREATE TABLE IF NOT EXISTS example.item (
    id               UUID PRIMARY KEY,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    org_id           UUID,
    source           VARCHAR(64),
    sensitivity      VARCHAR(32) NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version          BIGINT NOT NULL DEFAULT 0,
    title            VARCHAR(255) NOT NULL
);
