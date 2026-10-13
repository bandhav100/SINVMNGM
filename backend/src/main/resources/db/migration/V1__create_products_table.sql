-- V1__create_products_table.sql
-- Create products table for Smart Inventory Management System

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL CONSTRAINT uk_products_sku UNIQUE,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL CONSTRAINT chk_products_price CHECK (price >= 0),
    quantity INTEGER NOT NULL DEFAULT 0 CONSTRAINT chk_products_quantity CHECK (quantity >= 0),
    reorder_level INTEGER NOT NULL DEFAULT 10 CONSTRAINT chk_products_reorder_level CHECK (reorder_level >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_sku ON products(sku);
