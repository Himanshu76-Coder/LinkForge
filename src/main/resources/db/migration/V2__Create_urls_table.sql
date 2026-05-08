-- V2__Create_urls_table.sql
-- Creates the urls table for storing shortened URLs per PRD Section 9.6

CREATE TABLE urls (

    -- Primary Identifier
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    -- Ownership
    user_id BIGINT UNSIGNED NOT NULL,

    -- URL Information
    short_code   VARCHAR(50) NOT NULL,
    original_url TEXT        NOT NULL,

    -- Metadata
    title       VARCHAR(255) NULL,
    description VARCHAR(500) NULL,

    -- Status & Configuration
    is_custom_alias BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,

    -- Analytics
    total_clicks BIGINT UNSIGNED NOT NULL DEFAULT 0,
    click_limit  BIGINT UNSIGNED NULL,

    -- Expiration
    expires_at DATETIME NULL,

    -- Timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Primary Key
    PRIMARY KEY (id),

    -- Unique Constraints
    CONSTRAINT uq_urls_short_code UNIQUE (short_code),
    CONSTRAINT uq_urls_user_original_url UNIQUE (user_id, original_url(191)),

    -- Foreign Keys
    CONSTRAINT fk_urls_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    -- Indexes
    INDEX idx_urls_user_id      (user_id),
    INDEX idx_urls_is_active    (is_active),
    INDEX idx_urls_expires_at   (expires_at),
    INDEX idx_urls_created_at   (created_at),
    INDEX idx_urls_total_clicks (total_clicks)

) 

-- Table Configuration
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

