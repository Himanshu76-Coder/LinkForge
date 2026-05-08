-- V3__Create_click_logs_table.sql
-- Creates the click_logs table for tracking URL click analytics

CREATE TABLE click_logs (

    -- Primary Identifier
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    -- Ownership
    url_id BIGINT UNSIGNED NOT NULL,

    -- Click Data
    clicked_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45)      NULL,
    user_agent VARCHAR(255)     NULL,
    referrer   VARCHAR(500)     NULL,

    -- Primary Key
    PRIMARY KEY (id),

    -- Foreign Keys
    CONSTRAINT fk_click_logs_url
        FOREIGN KEY (url_id)
        REFERENCES urls (id)
        ON DELETE CASCADE,

    -- Indexes
    INDEX idx_click_logs_url_id     (url_id),
    INDEX idx_click_logs_clicked_at (clicked_at),

    -- Composite index for analytics queries that filter by url_id and clicked_at together
    INDEX idx_click_logs_url_clicked (url_id, clicked_at)

)

-- Table Configuration
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
