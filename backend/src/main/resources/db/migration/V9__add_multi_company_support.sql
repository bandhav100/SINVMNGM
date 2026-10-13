-- =============================================================================
-- V9__add_multi_company_support.sql
-- Smart Inventory Management System - Multi-Company / Multi-Tenant Isolation
-- =============================================================================

-- 1. Create Companies Table
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(50),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_companies_name_lower ON companies (LOWER(TRIM(name)));

-- Seed default company with id=1 for all existing data
INSERT INTO companies (id, name, email, phone, currency, timezone, created_at, updated_at)
VALUES (1, 'Smart Inventory Systems', 'admin@sims.local', '+1 (555) 019-2834', 'INR', 'Asia/Kolkata', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Advance sequence past 1
SELECT setval('companies_id_seq', (SELECT COALESCE(MAX(id), 1) FROM companies));

-- 2. Add company_id to Users Table
ALTER TABLE users ADD COLUMN IF NOT EXISTS company_id BIGINT NOT NULL DEFAULT 1 REFERENCES companies(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_users_company_id ON users(company_id);

-- 3. Add company_id to Products Table
ALTER TABLE products ADD COLUMN IF NOT EXISTS company_id BIGINT NOT NULL DEFAULT 1 REFERENCES companies(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_products_company_id ON products(company_id);

-- Make product SKU unique per company instead of globally
ALTER TABLE products DROP CONSTRAINT IF EXISTS uk_products_sku;
DROP INDEX IF EXISTS uk_products_sku;
CREATE UNIQUE INDEX IF NOT EXISTS uk_products_company_sku ON products (company_id, LOWER(TRIM(sku)));

-- 4. Add company_id to Categories Table
ALTER TABLE categories ADD COLUMN IF NOT EXISTS company_id BIGINT NOT NULL DEFAULT 1 REFERENCES companies(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_categories_company_id ON categories(company_id);

-- Make category name unique per company
DROP INDEX IF EXISTS uk_categories_name_lower;
CREATE UNIQUE INDEX IF NOT EXISTS uk_categories_company_name ON categories (company_id, LOWER(TRIM(name)));

-- 5. Add company_id to Suppliers Table
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS company_id BIGINT NOT NULL DEFAULT 1 REFERENCES companies(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_suppliers_company_id ON suppliers(company_id);

-- 6. Add company_id to Purchase Orders Table
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS company_id BIGINT NOT NULL DEFAULT 1 REFERENCES companies(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_purchase_orders_company_id ON purchase_orders(company_id);

-- Make purchase order number unique per company
ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS uk_purchase_orders_number;
DROP INDEX IF EXISTS uk_purchase_orders_number;
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_orders_company_number ON purchase_orders (company_id, order_number);

-- 7. Add company_id to Sales Orders Table
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS company_id BIGINT NOT NULL DEFAULT 1 REFERENCES companies(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_sales_orders_company_id ON sales_orders(company_id);

-- Make sales order number unique per company
ALTER TABLE sales_orders DROP CONSTRAINT IF EXISTS uk_sales_orders_number;
DROP INDEX IF EXISTS uk_sales_orders_number;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_orders_company_number ON sales_orders (company_id, order_number);

-- 8. Add company_id to Inventory Transactions Table
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS company_id BIGINT NOT NULL DEFAULT 1 REFERENCES companies(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_company_id ON inventory_transactions(company_id);

-- 9. Add company_id to Stock Adjustments Table
ALTER TABLE stock_adjustments ADD COLUMN IF NOT EXISTS company_id BIGINT NOT NULL DEFAULT 1 REFERENCES companies(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_company_id ON stock_adjustments(company_id);

-- 10. Add company_id to System Settings Table
ALTER TABLE system_settings ADD COLUMN IF NOT EXISTS company_id BIGINT NOT NULL DEFAULT 1 REFERENCES companies(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_system_settings_company_id ON system_settings(company_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_system_settings_company_id ON system_settings(company_id);
