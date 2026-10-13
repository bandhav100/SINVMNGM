-- =============================================================================
-- V5__create_sales_tables.sql
-- Smart Inventory Management System - Phase 5: Sales Management
-- =============================================================================

-- 1. Sales Orders Master Table
CREATE TABLE sales_orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL CONSTRAINT uk_sales_orders_number UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_so_status CHECK (status IN ('DRAFT', 'COMPLETED', 'CANCELLED')),
    total_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00
        CONSTRAINT chk_so_total CHECK (total_amount >= 0),
    customer_name VARCHAR(255),
    customer_contact VARCHAR(255),
    notes TEXT,
    created_by BIGINT CONSTRAINT fk_so_created_by REFERENCES users(id) ON DELETE SET NULL,
    completed_by BIGINT CONSTRAINT fk_so_completed_by REFERENCES users(id) ON DELETE SET NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_so_status ON sales_orders(status);
CREATE INDEX idx_so_created_at ON sales_orders(created_at);

-- 2. Sales Order Line Items Table
CREATE TABLE sales_order_items (
    id BIGSERIAL PRIMARY KEY,
    sales_order_id BIGINT NOT NULL CONSTRAINT fk_soi_order REFERENCES sales_orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL CONSTRAINT fk_soi_product REFERENCES products(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CONSTRAINT chk_soi_quantity CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL CONSTRAINT chk_soi_unit_price CHECK (unit_price >= 0),
    line_total NUMERIC(14, 2) NOT NULL CONSTRAINT chk_soi_line_total CHECK (line_total >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_soi_order_product UNIQUE (sales_order_id, product_id)
);

CREATE INDEX idx_soi_product_id ON sales_order_items(product_id);
