-- V3__Create_click_logs_table.sql
-- Creates the click_logs table for tracking URL click analytics per PRD Section 9.7

CREATE TABLE click_logs (
    id          BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    url_id      BIGINT UNSIGNED   NOT NULL,
    clicked_at  DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address  VARCHAR(45)           NULL,
    user_agent  VARCHAR(255)          NULL,
    referrer    VARCHAR(500)          NULL,
    country     VARCHAR(2)            NULL,

    PRIMARY KEY (id),
    INDEX idx_click_logs_url_id     (url_id),
    INDEX idx_click_logs_clicked_at (clicked_at),
    INDEX idx_click_logs_country    (country),

    CONSTRAINT fk_click_logs_url
        FOREIGN KEY (url_id) REFERENCES urls (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
