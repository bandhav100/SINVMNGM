-- V7: Create stock adjustments table for audit records and authorized manual inventory corrections

CREATE TABLE stock_adjustments (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL CONSTRAINT fk_stock_adj_product REFERENCES products(id) ON DELETE RESTRICT,
    quantity_delta INTEGER NOT NULL CONSTRAINT chk_stock_adj_delta CHECK (quantity_delta != 0),
    quantity_before INTEGER NOT NULL CONSTRAINT chk_stock_adj_before CHECK (quantity_before >= 0),
    quantity_after INTEGER NOT NULL CONSTRAINT chk_stock_adj_after CHECK (quantity_after >= 0),
    reason VARCHAR(255) NOT NULL,
    notes TEXT,
    adjusted_by BIGINT CONSTRAINT fk_stock_adj_user REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_adj_product_id ON stock_adjustments(product_id);
CREATE INDEX idx_stock_adj_created_at ON stock_adjustments(created_at DESC);
