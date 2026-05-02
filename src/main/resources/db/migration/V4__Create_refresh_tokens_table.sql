-- V4__Create_refresh_tokens_table.sql
-- Creates the refresh_tokens table for JWT refresh token management per PRD Section 9.5

CREATE TABLE refresh_tokens (
    id            BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    user_id       BIGINT UNSIGNED   NOT NULL,
    hashed_token  VARCHAR(64)       NOT NULL,
    expires_at    DATETIME          NOT NULL,
    revoked       BOOLEAN           NOT NULL DEFAULT FALSE,
    created_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address    VARCHAR(45)           NULL,
    user_agent    VARCHAR(255)          NULL,
    revoked_at    DATETIME              NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_hashed_token (hashed_token),
    INDEX      idx_refresh_user_id     (user_id),
    INDEX      idx_refresh_revoked     (revoked, expires_at),

    CONSTRAINT fk_refresh_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
