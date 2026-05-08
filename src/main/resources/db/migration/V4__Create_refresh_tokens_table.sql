-- V4__Create_refresh_tokens_table.sql
-- Creates the refresh_tokens table for JWT refresh token management

CREATE TABLE refresh_tokens (

    -- Primary Identifier
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    -- Ownership
    user_id BIGINT UNSIGNED NOT NULL,

    -- Token Data
    hashed_token VARCHAR(255) NOT NULL,

    -- Status & Lifecycle
    expires_at DATETIME NOT NULL,
    revoked    BOOLEAN  NOT NULL DEFAULT FALSE,
    revoked_at DATETIME     NULL,

    -- Client Metadata
    ip_address VARCHAR(45)  NULL,
    user_agent VARCHAR(255) NULL,

    -- Timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Primary Key
    PRIMARY KEY (id),

    -- Unique Constraints
    CONSTRAINT uq_refresh_hashed_token UNIQUE (hashed_token),

    -- Foreign Keys
    CONSTRAINT fk_refresh_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    -- Indexes
    INDEX idx_refresh_user_id (user_id),

    -- Composite index for efficient lookup of active/expired tokens per user
    INDEX idx_refresh_revoked (revoked, expires_at)

)

-- Table Configuration
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

