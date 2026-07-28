CREATE TABLE saged.org_units (
    id               UUID PRIMARY KEY,
    code             VARCHAR(32)  NOT NULL UNIQUE,
    name             VARCHAR(128) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    org_id           UUID,
    source           VARCHAR(64),
    sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version          BIGINT       NOT NULL DEFAULT 0
);

INSERT INTO saged.org_units (id, code, name, updated_at, source) VALUES
    ('00000000-0000-0000-0000-000000000001', 'SEPLATI',  'Secretaria de Planejamento e Tecnologia', NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000002', 'SEMED',    'Secretaria Municipal de Educação',        NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000003', 'SEMUS',    'Secretaria Municipal de Saúde',           NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000004', 'SEMADS',   'Secretaria de Assistência Social',        NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000005', 'SEMINFRA', 'Secretaria de Infraestrutura',            NOW(), 'seed');
