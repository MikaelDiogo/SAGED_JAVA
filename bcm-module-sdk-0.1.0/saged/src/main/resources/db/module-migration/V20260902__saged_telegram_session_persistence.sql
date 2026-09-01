CREATE TABLE saged.telegram_sessions (
    telegram_user_id VARCHAR(128) PRIMARY KEY,
    state            VARCHAR(32)  NOT NULL,
    specialty_code   VARCHAR(32),
    specialty_name   VARCHAR(128),
    title            VARCHAR(255),
    expires_at       TIMESTAMPTZ  NOT NULL
);

CREATE TABLE saged.telegram_link_codes (
    code             VARCHAR(6)   PRIMARY KEY,
    telegram_user_id VARCHAR(128) NOT NULL,
    expires_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_telegram_sessions_expires ON saged.telegram_sessions (expires_at);
CREATE INDEX idx_telegram_link_codes_expires ON saged.telegram_link_codes (expires_at);
