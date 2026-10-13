-- =============================================================================
-- V4__create_purchases_and_inventory_ledger_tables.sql
-- Smart Inventory Management System - Phase 4: Purchase Management & Ledger
-- =============================================================================

-- 1. Purchase Orders Master Table
CREATE TABLE purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL CONSTRAINT uk_purchase_orders_number UNIQUE,
    supplier_id BIGINT NOT NULL CONSTRAINT fk_po_supplier REFERENCES suppliers(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' 
        CONSTRAINT chk_po_status CHECK (status IN ('DRAFT', 'ORDERED', 'RECEIVED', 'CANCELLED')),
    total_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00 CONSTRAINT chk_po_total CHECK (total_amount >= 0),
    expected_delivery_date DATE,
    notes TEXT,
    created_by BIGINT CONSTRAINT fk_po_created_by REFERENCES users(id) ON DELETE SET NULL,
    ordered_at TIMESTAMP WITH TIME ZONE,
    received_at TIMESTAMP WITH TIME ZONE,
    received_by BIGINT CONSTRAINT fk_po_received_by REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_po_supplier_id ON purchase_orders(supplier_id);
CREATE INDEX idx_po_status ON purchase_orders(status);
CREATE INDEX idx_po_created_at ON purchase_orders(created_at);

-- 2. Purchase Order Line Items Table
CREATE TABLE purchase_order_items (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL CONSTRAINT fk_poi_order REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL CONSTRAINT fk_poi_product REFERENCES products(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CONSTRAINT chk_poi_quantity CHECK (quantity > 0),
    unit_cost NUMERIC(12, 2) NOT NULL CONSTRAINT chk_poi_unit_cost CHECK (unit_cost >= 0),
    total_cost NUMERIC(14, 2) NOT NULL CONSTRAINT chk_poi_total_cost CHECK (total_cost >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_poi_order_product UNIQUE (purchase_order_id, product_id)
);

-- uk_poi_order_product already indexes purchase_order_id as leading column; index product_id for lookups
CREATE INDEX idx_poi_product_id ON purchase_order_items(product_id);

-- 3. Inventory Transactions Ledger
CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL CONSTRAINT fk_inv_txn_product REFERENCES products(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(30) NOT NULL 
        CONSTRAINT chk_inv_txn_type CHECK (transaction_type IN ('PURCHASE_RECEIPT', 'SALE_DEDUCTION', 'MANUAL_ADJUSTMENT')),
    quantity_delta INTEGER NOT NULL,
    quantity_before INTEGER NOT NULL CONSTRAINT chk_inv_before CHECK (quantity_before >= 0),
    quantity_after INTEGER NOT NULL CONSTRAINT chk_inv_after CHECK (quantity_after >= 0),
    reference_type VARCHAR(50) NOT NULL,
    reference_id BIGINT NOT NULL,
    performed_by BIGINT CONSTRAINT fk_inv_txn_user REFERENCES users(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inv_txn_reference UNIQUE (reference_type, reference_id, product_id)
);

-- uk_inv_txn_reference already indexes (reference_type, reference_id) as leading columns; index product_id for product movement history
CREATE INDEX idx_inv_txn_product_id ON inventory_transactions(product_id);
