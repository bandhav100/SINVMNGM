import React from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import { Calendar, Tag, Layers, AlertCircle, IndianRupee, Edit } from 'lucide-react';

export default function ProductDetailsModal({ isOpen, onClose, product, onEdit }) {
  if (!product) return null;

  const isOutOfStock = product.quantity === 0;
  const isLowStock = !isOutOfStock && product.quantity <= product.reorderLevel;

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString();
    } catch {
      return dateStr;
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Product Details"
      maxWidth="600px"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          {onEdit && (
            <Button
              variant="primary"
              icon={Edit}
              onClick={() => {
                onClose();
                onEdit(product);
              }}
            >
              Edit Product
            </Button>
          )}
        </>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--color-text-primary)' }}>
              {product.name}
            </h3>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginTop: '0.25rem', flexWrap: 'wrap' }}>
              <span style={{ fontSize: '0.875rem', color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                <Tag size={14} />
                SKU: <strong>{product.sku}</strong>
              </span>
              {product.categoryName ? (
                <Badge variant="primary">{product.categoryName}</Badge>
              ) : (
                <Badge variant="neutral">Uncategorized</Badge>
              )}
            </div>
          </div>

          <div>
            {isOutOfStock ? (
              <Badge variant="danger">Out of Stock</Badge>
            ) : isLowStock ? (
              <Badge variant="warning">Low Stock</Badge>
            ) : (
              <Badge variant="success">In Stock</Badge>
            )}
          </div>
        </div>

        {product.description && (
          <div style={{ padding: '0.75rem 1rem', backgroundColor: 'var(--color-surface-subtle)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem' }}>
            <span style={{ fontWeight: '600', color: 'var(--color-text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Description</span>
            <p style={{ color: 'var(--color-text-primary)', whiteSpace: 'pre-wrap' }}>{product.description}</p>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
          <div style={{ border: '1px solid var(--color-border)', padding: '0.75rem', borderRadius: 'var(--radius-md)' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <IndianRupee size={13} /> Unit Price
            </span>
            <span style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--color-text-primary)', display: 'block', marginTop: '0.25rem' }}>
              ₹{Number(product.price).toFixed(2)}
            </span>
          </div>

          <div style={{ border: '1px solid var(--color-border)', padding: '0.75rem', borderRadius: 'var(--radius-md)' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <Layers size={13} /> Current Stock
            </span>
            <span style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--color-text-primary)', display: 'block', marginTop: '0.25rem' }}>
              {product.quantity} units
            </span>
          </div>

          <div style={{ border: '1px solid var(--color-border)', padding: '0.75rem', borderRadius: 'var(--radius-md)' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <AlertCircle size={13} /> Reorder Level
            </span>
            <span style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--color-text-primary)', display: 'block', marginTop: '0.25rem' }}>
              {product.reorderLevel} units
            </span>
          </div>
        </div>

        <div style={{ borderTop: '1px solid var(--color-border)', paddingTop: '0.75rem', display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
            <Calendar size={13} /> Created: {formatDate(product.createdAt)}
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
            <Calendar size={13} /> Last Updated: {formatDate(product.updatedAt)}
          </span>
        </div>
      </div>
    </Modal>
  );
}
