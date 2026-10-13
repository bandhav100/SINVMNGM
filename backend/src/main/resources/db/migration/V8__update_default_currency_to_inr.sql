-- =============================================================================
-- Migration: V8__update_default_currency_to_inr.sql
-- Description: Update system default currency from USD to INR (Indian Rupee)
-- =============================================================================

UPDATE system_settings
SET currency = 'INR',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;
