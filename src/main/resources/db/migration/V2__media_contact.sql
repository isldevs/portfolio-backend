CREATE TABLE contact
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(254) NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    message    TEXT         NOT NULL CHECK (length(message) <= 10000),
    status     VARCHAR(16)  NOT NULL DEFAULT 'UNREAD' CHECK (status IN ('UNREAD', 'READ', 'RESOLVED', 'ARCHIVED')),
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_contact_status_created ON contact (status, created_at DESC);

CREATE TABLE media
(
    id            UUID PRIMARY KEY,
    storage_key   VARCHAR(100) NOT NULL UNIQUE,
    original_name VARCHAR(180) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size          BIGINT       NOT NULL CHECK (size > 0 AND size <= 10485760),
    visibility    VARCHAR(10)  NOT NULL DEFAULT 'PUBLIC' CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_media_created ON media (created_at DESC);
