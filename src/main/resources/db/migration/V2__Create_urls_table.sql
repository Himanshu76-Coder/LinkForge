-- V2__Create_urls_table.sql
-- Creates the urls table for storing shortened URLs per PRD Section 9.6

CREATE TABLE urls (
    id               BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED   NOT NULL,
    short_code       VARCHAR(50)       NOT NULL,
    original_url     TEXT              NOT NULL,
    title            VARCHAR(255)          NULL,
    description      VARCHAR(500)          NULL,
    is_custom_alias  BOOLEAN           NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN           NOT NULL DEFAULT TRUE,
    total_clicks     BIGINT UNSIGNED   NOT NULL DEFAULT 0,
    click_limit      BIGINT UNSIGNED       NULL,
    expires_at       DATETIME              NULL,
    created_at       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_urls_short_code        (short_code),
    UNIQUE KEY uq_urls_user_original_url (user_id, original_url(768)),
    INDEX      idx_urls_user_id          (user_id),
    INDEX      idx_urls_is_active        (is_active),
    INDEX      idx_urls_expires_at       (expires_at),
    INDEX      idx_urls_created_at       (created_at),
    INDEX      idx_urls_total_clicks     (total_clicks),

    CONSTRAINT fk_urls_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
