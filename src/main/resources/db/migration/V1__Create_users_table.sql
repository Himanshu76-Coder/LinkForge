-- V1__Create_users_table.sql
-- Creates the users table for user account management per PRD Section 9.4

CREATE TABLE users (
    id               BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    username         VARCHAR(50)       NOT NULL,
    email            VARCHAR(255)      NOT NULL,
    hashed_password  VARCHAR(255)      NOT NULL,
    created_at       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email    (email),
    UNIQUE KEY uq_users_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
