CREATE SCHEMA IF NOT EXISTS saged;

-- Specialties (filas: Hardware=01, Redes=02, ...)
CREATE TABLE saged.specialties (
                                   id               UUID PRIMARY KEY,
                                   code             VARCHAR(32)  NOT NULL UNIQUE,
                                   name             VARCHAR(128) NOT NULL,
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

CREATE TABLE saged.assets (
                              id               UUID PRIMARY KEY,
                              asset_tag        VARCHAR(128) NOT NULL UNIQUE,
                              description      VARCHAR(512),
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

-- user_id = UUID da plataforma (sem tabela users local)
CREATE TABLE saged.user_specialties (
                                        id               UUID PRIMARY KEY,
                                        user_id          UUID         NOT NULL,
                                        specialty_id     UUID         NOT NULL REFERENCES saged.specialties(id),
                                        created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                        updated_at       TIMESTAMPTZ  NOT NULL,
                                        created_by       VARCHAR(255),
                                        updated_by       VARCHAR(255),
                                        org_id           UUID,
                                        source           VARCHAR(64),
                                        sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
                                        lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
                                        version          BIGINT       NOT NULL DEFAULT 0,
                                        UNIQUE (user_id, specialty_id)
);

-- status: A_FAZER | EM_ANDAMENTO | CONCLUIDO | INTERROMPIDO
-- department_id / requester_user_id = UUIDs lógicos (sem JOIN cross-schema)
CREATE TABLE saged.demands (
                               id                     UUID PRIMARY KEY,
                               protocol               VARCHAR(64)  NOT NULL UNIQUE,
                               title                  VARCHAR(255) NOT NULL,
                               description            TEXT         NOT NULL,
                               status                 VARCHAR(64)  NOT NULL,
                               requester_user_id      UUID         NOT NULL,
                               department_id          UUID         NOT NULL,
                               specialty_id           UUID         NOT NULL REFERENCES saged.specialties(id),
                               asset_tag              VARCHAR(128),
                               current_technical_note TEXT,
                               assignee_user_id       UUID,
                               created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                               updated_at             TIMESTAMPTZ  NOT NULL,
                               created_by             VARCHAR(255),
                               updated_by             VARCHAR(255),
                               org_id                 UUID,
                               source                 VARCHAR(64),
                               sensitivity            VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
                               lifecycle_status       VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
                               version                BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_saged_demands_department ON saged.demands (department_id);
CREATE INDEX idx_saged_demands_specialty  ON saged.demands (specialty_id);
CREATE INDEX idx_saged_demands_status     ON saged.demands (status);

CREATE TABLE saged.demand_history (
                                      id               UUID PRIMARY KEY,
                                      demand_id        UUID         NOT NULL REFERENCES saged.demands(id),
                                      action           VARCHAR(64)  NOT NULL,
                                      justification    TEXT,
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

CREATE INDEX idx_saged_demand_history_demand ON saged.demand_history (demand_id);

CREATE TABLE saged.telegram_demand_requesters (
                                                  id               UUID PRIMARY KEY,
                                                  telegram_chat_id VARCHAR(128) NOT NULL,
                                                  phone_number     VARCHAR(64)  NOT NULL,
                                                  display_name     VARCHAR(255),
                                                  department_id    UUID         NOT NULL,
                                                  is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
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

CREATE TABLE saged.telegram_requester_authorizations (
                                                         id               UUID PRIMARY KEY,
                                                         requester_id     UUID         NOT NULL REFERENCES saged.telegram_demand_requesters(id),
                                                         department_id    UUID         NOT NULL,
                                                         created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                                         updated_at       TIMESTAMPTZ  NOT NULL,
                                                         created_by       VARCHAR(255),
                                                         updated_by       VARCHAR(255),
                                                         org_id           UUID,
                                                         source           VARCHAR(64),
                                                         sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
                                                         lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
                                                         version          BIGINT       NOT NULL DEFAULT 0,
                                                         UNIQUE (requester_id, department_id)
);

CREATE TABLE saged.telegram_contacts (
                                         id               UUID PRIMARY KEY,
                                         telegram_user_id VARCHAR(128) NOT NULL UNIQUE,
                                         chat_id          VARCHAR(128) NOT NULL,
                                         phone_number     VARCHAR(64),
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

CREATE TABLE saged.bot_processed_messages (
                                              id                  UUID PRIMARY KEY,
                                              provider            VARCHAR(32)  NOT NULL DEFAULT 'TELEGRAM',
                                              external_message_id VARCHAR(128) NOT NULL,
                                              processed_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                              created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                              updated_at          TIMESTAMPTZ  NOT NULL,
                                              created_by          VARCHAR(255),
                                              updated_by          VARCHAR(255),
                                              org_id              UUID,
                                              source              VARCHAR(64),
                                              sensitivity         VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
                                              lifecycle_status    VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
                                              version             BIGINT       NOT NULL DEFAULT 0,
                                              UNIQUE (provider, external_message_id)
);