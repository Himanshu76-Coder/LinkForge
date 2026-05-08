-- V1__Create_users_table.sql
-- Creates the users table for user account management

CREATE TABLE users (

    -- Primary Identifier
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    -- Account Credentials
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,

    -- Timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Primary Key
    PRIMARY KEY (id),

    -- Unique Constraints
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)

)

-- Table Configuration
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

