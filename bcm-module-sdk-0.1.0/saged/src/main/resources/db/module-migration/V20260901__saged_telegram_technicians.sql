CREATE TABLE saged.telegram_technicians (
    id               UUID         PRIMARY KEY,
    telegram_user_id VARCHAR(128) NOT NULL UNIQUE,
    keycloak_user_id UUID         NOT NULL UNIQUE,
    display_name     VARCHAR(255),
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    org_id           UUID,
    source           VARCHAR(64),
    sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version          BIGINT       NOT NULL DEFAULT 0
);
