-- V3__create_users_and_roles_tables.sql
-- Create users table for authentication and Role-Based Access Control (RBAC)

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF'))
);

-- Case-insensitive unique indexes for username and email
CREATE UNIQUE INDEX uk_users_username_lower ON users (LOWER(TRIM(username)));
CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(TRIM(email)));

-- Lookup and filter indexes
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);
