-- =============================================================================
-- V6__create_system_settings_table.sql
-- Smart Inventory Management System - Phase 8: System & Business Settings
-- =============================================================================

CREATE TABLE system_settings (
    id BIGINT PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    company_email VARCHAR(100),
    company_phone VARCHAR(50),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    default_reorder_threshold INTEGER NOT NULL DEFAULT 10
        CONSTRAINT chk_settings_reorder CHECK (default_reorder_threshold >= 0),
    low_stock_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    out_of_stock_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    require_purchase_approval BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT CONSTRAINT fk_settings_updated_by REFERENCES users(id) ON DELETE SET NULL
);

-- Seed initial default configuration (singleton row id=1)
INSERT INTO system_settings (
    id,
    company_name,
    company_email,
    company_phone,
    currency,
    timezone,
    default_reorder_threshold,
    low_stock_alerts_enabled,
    out_of_stock_alerts_enabled,
    allow_negative_stock,
    require_purchase_approval,
    updated_at
) VALUES (
    1,
    'Smart Inventory Systems',
    'admin@sims.local',
    '+1 (555) 019-2834',
    'USD',
    'UTC',
    10,
    TRUE,
    TRUE,
    FALSE,
    FALSE,
    CURRENT_TIMESTAMP
);
