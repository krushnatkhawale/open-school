CREATE TABLE IF NOT EXISTS classification_record (
    id                UUID PRIMARY KEY,
    chat_id           BIGINT NOT NULL,
    original_text     TEXT,
    communication_type VARCHAR(50) NOT NULL,
    image_description TEXT,
    image_content_type  VARCHAR(50),
    image_data        BYTEA,
    tags              TEXT,
    content_type      VARCHAR(20),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE classification_record ADD COLUMN IF NOT EXISTS content_type VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_classification_record_chat_id ON classification_record (chat_id);
CREATE INDEX IF NOT EXISTS idx_classification_record_communication_type ON classification_record (communication_type);
CREATE INDEX IF NOT EXISTS idx_classification_record_created_at ON classification_record (created_at);

CREATE TABLE IF NOT EXISTS school (
    id                UUID PRIMARY KEY,
    name              VARCHAR(255),
    city              VARCHAR(255),
    registered_number VARCHAR(255),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS app_user (
    id                 UUID PRIMARY KEY,
    telegram_chat_id   BIGINT NOT NULL UNIQUE,
    role               VARCHAR(20) NOT NULL,
    school_id          UUID REFERENCES school (id),
    onboarding_status  VARCHAR(30) NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    onboarded_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_app_user_telegram_chat_id ON app_user (telegram_chat_id);
CREATE INDEX IF NOT EXISTS idx_app_user_school_id ON app_user (school_id);

CREATE TABLE IF NOT EXISTS extraction_rule (
    rule_key   VARCHAR(50) PRIMARY KEY,
    rules      TEXT,
    enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE
);
