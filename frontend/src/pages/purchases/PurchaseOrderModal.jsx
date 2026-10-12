import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import AlertBanner from '../../components/common/AlertBanner';
import { supplierApi } from '../../api/supplierApi';
import { productApi } from '../../api/productApi';
import { Plus, Trash2, ShoppingCart, Calculator, AlertCircle } from 'lucide-react';

export default function PurchaseOrderModal({
  isOpen,
  onClose,
  onSubmit,
  initialData = null,
  isSubmitting = false,
  backendErrors = null
}) {
  const isEditing = Boolean(initialData);

  const [supplierId, setSupplierId] = useState('');
  const [expectedDeliveryDate, setExpectedDeliveryDate] = useState('');
  const [notes, setNotes] = useState('');
  const [items, setItems] = useState([
    { productId: '', quantity: 1, unitCost: '' }
  ]);

  const [suppliers, setSuppliers] = useState([]);
  const [products, setProducts] = useState([]);
  const [isLoadingDropdowns, setIsLoadingDropdowns] = useState(false);
  const [clientErrors, setClientErrors] = useState({});

  useEffect(() => {
    if (!isOpen) return;

    const loadDropdownData = async () => {
      setIsLoadingDropdowns(true);
      try {
        const [suppliersRes, productsRes] = await Promise.all([
          supplierApi.getSuppliers({ size: 100, active: true }),
          productApi.getProducts({ size: 100 })
        ]);
        setSuppliers(suppliersRes.content || []);
        setProducts(productsRes.content || []);
      } catch (err) {
        console.error('Failed to load dropdown options:', err);
      } finally {
        setIsLoadingDropdowns(false);
      }
    };

    loadDropdownData();

    if (initialData) {
      setSupplierId(initialData.supplierId || '');
      setExpectedDeliveryDate(initialData.expectedDeliveryDate || '');
      setNotes(initialData.notes || '');
      if (initialData.items && initialData.items.length > 0) {
        setItems(
          initialData.items.map((i) => ({
            productId: String(i.productId),
            quantity: i.quantity,
            unitCost: String(i.unitCost)
          }))
        );
      } else {
        setItems([{ productId: '', quantity: 1, unitCost: '' }]);
      }
    } else {
      setSupplierId('');
      setExpectedDeliveryDate('');
      setNotes('');
      setItems([{ productId: '', quantity: 1, unitCost: '' }]);
    }
    setClientErrors({});
  }, [isOpen, initialData]);

  const handleAddItem = () => {
    setItems((prev) => [...prev, { productId: '', quantity: 1, unitCost: '' }]);
  };

  const handleRemoveItem = (index) => {
    if (items.length <= 1) return;
    setItems((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleItemChange = (index, field, value) => {
    setItems((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], [field]: value };

      // Auto-populate price if product changed and unitCost is empty
      if (field === 'productId') {
        const foundProd = products.find((p) => String(p.id) === String(value));
        if (foundProd && (!next[index].unitCost || next[index].unitCost === '')) {
          next[index].unitCost = String(foundProd.price || '');
        }
      }
      return next;
    });
  };

  const calculateItemTotal = (item) => {
    const qty = parseInt(item.quantity, 10) || 0;
    const cost = parseFloat(item.unitCost) || 0;
    return (qty * cost).toFixed(2);
  };

  const calculateGrandTotal = () => {
    const total = items.reduce((sum, item) => {
      const qty = parseInt(item.quantity, 10) || 0;
      const cost = parseFloat(item.unitCost) || 0;
      return sum + qty * cost;
    }, 0);
    return total.toFixed(2);
  };

  const validate = () => {
    const errors = {};
    if (!supplierId) {
      errors.supplierId = 'Supplier is required';
    }

    if (!items || items.length === 0) {
      errors.items = 'At least one item is required';
    } else {
      const seenProductIds = new Set();
      items.forEach((item, idx) => {
        if (!item.productId) {
          errors[`item_${idx}_product`] = 'Product selection required';
        } else if (seenProductIds.has(item.productId)) {
          errors[`item_${idx}_product`] = 'Duplicate product in order';
        } else {
          seenProductIds.add(item.productId);
        }

        const qty = parseInt(item.quantity, 10);
        if (isNaN(qty) || qty <= 0) {
          errors[`item_${idx}_qty`] = 'Quantity must be ≥ 1';
        }

        const cost = parseFloat(item.unitCost);
        if (isNaN(cost) || cost < 0) {
          errors[`item_${idx}_cost`] = 'Unit cost must be ≥ 0';
        }
      });
    }

    setClientErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSave = (submitDirectly = false) => {
    if (!validate()) return;

    const payload = {
      supplierId: parseInt(supplierId, 10),
      expectedDeliveryDate: expectedDeliveryDate || null,
      notes: notes || null,
      status: submitDirectly ? 'ORDERED' : 'DRAFT',
      items: items.map((item) => ({
        productId: parseInt(item.productId, 10),
        quantity: parseInt(item.quantity, 10),
        unitCost: parseFloat(item.unitCost)
      }))
    };

    onSubmit(payload);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? 'Edit Draft Purchase Order' : 'Create Purchase Order'}
      maxWidth="750px"
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {backendErrors && (
          <AlertBanner
            type="error"
            message={
              typeof backendErrors === 'string'
                ? backendErrors
                : 'Failed to save purchase order. Please check input values.'
            }
          />
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <div>
            <label className="form-label" style={{ fontWeight: 600, display: 'block', marginBottom: '0.375rem' }}>
              Supplier *
            </label>
            <select
              className={`form-input ${clientErrors.supplierId ? 'input-error' : ''}`}
              value={supplierId}
              onChange={(e) => setSupplierId(e.target.value)}
              disabled={isLoadingDropdowns || isSubmitting}
              style={{ width: '100%', padding: '0.625rem' }}
            >
              <option value="">Select an active supplier...</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} {s.contactPerson ? `(${s.contactPerson})` : ''}
                </option>
              ))}
            </select>
            {clientErrors.supplierId && (
              <span style={{ color: 'var(--color-danger)', fontSize: '0.75rem', marginTop: '0.25rem', display: 'block' }}>
                {clientErrors.supplierId}
              </span>
            )}
          </div>

          <div>
            <Input
              label="Expected Delivery Date"
              type="date"
              value={expectedDeliveryDate}
              onChange={(e) => setExpectedDeliveryDate(e.target.value)}
              disabled={isSubmitting}
            />
          </div>
        </div>

        <div>
          <label className="form-label" style={{ fontWeight: 600, display: 'block', marginBottom: '0.375rem' }}>
            Order Notes (Optional)
          </label>
          <textarea
            className="form-input"
            rows="2"
            placeholder="Add internal procurement notes, shipping terms, or references..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            disabled={isSubmitting}
            style={{ width: '100%', padding: '0.625rem', fontFamily: 'inherit' }}
          />
        </div>

        {/* Line Items Section */}
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
            <h4 style={{ margin: 0, fontSize: '0.9375rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <ShoppingCart size={16} /> Purchase Order Items ({items.length})
            </h4>
            <Button
              type="button"
              variant="secondary"
              size="sm"
              icon={Plus}
              onClick={handleAddItem}
              disabled={isSubmitting}
            >
              Add Item
            </Button>
          </div>

          <div style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
              <thead style={{ backgroundColor: 'var(--color-bg-secondary)', borderBottom: '1px solid var(--color-border)' }}>
                <tr>
                  <th style={{ padding: '0.5rem 0.75rem', textAlign: 'left', fontWeight: 600 }}>Product *</th>
                  <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right', width: '110px', fontWeight: 600 }}>Quantity *</th>
                  <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right', width: '130px', fontWeight: 600 }}>Unit Cost (₹) *</th>
                  <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right', width: '110px', fontWeight: 600 }}>Subtotal</th>
                  <th style={{ padding: '0.5rem 0.75rem', textAlign: 'center', width: '50px' }}></th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, idx) => (
                  <tr key={idx} style={{ borderBottom: idx < items.length - 1 ? '1px solid var(--color-border)' : 'none' }}>
                    <td style={{ padding: '0.5rem 0.75rem' }}>
                      <select
                        className={`form-input ${clientErrors[`item_${idx}_product`] ? 'input-error' : ''}`}
                        value={item.productId}
                        onChange={(e) => handleItemChange(idx, 'productId', e.target.value)}
                        disabled={isLoadingDropdowns || isSubmitting}
                        style={{ width: '100%', padding: '0.375rem 0.5rem', fontSize: '0.8125rem' }}
                      >
                        <option value="">Select product...</option>
                        {products.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.name} ({p.sku}) - Current stock: {p.quantity}
                          </option>
                        ))}
                      </select>
                      {clientErrors[`item_${idx}_product`] && (
                        <span style={{ color: 'var(--color-danger)', fontSize: '0.6875rem', display: 'block' }}>
                          {clientErrors[`item_${idx}_product`]}
                        </span>
                      )}
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem' }}>
                      <input
                        type="number"
                        min="1"
                        className={`form-input ${clientErrors[`item_${idx}_qty`] ? 'input-error' : ''}`}
                        value={item.quantity}
                        onChange={(e) => handleItemChange(idx, 'quantity', e.target.value)}
                        disabled={isSubmitting}
                        style={{ width: '100%', textAlign: 'right', padding: '0.375rem 0.5rem', fontSize: '0.8125rem' }}
                      />
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem' }}>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        placeholder="0.00"
                        className={`form-input ${clientErrors[`item_${idx}_cost`] ? 'input-error' : ''}`}
                        value={item.unitCost}
                        onChange={(e) => handleItemChange(idx, 'unitCost', e.target.value)}
                        disabled={isSubmitting}
                        style={{ width: '100%', textAlign: 'right', padding: '0.375rem 0.5rem', fontSize: '0.8125rem' }}
                      />
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem', textAlign: 'right', fontWeight: 600, color: 'var(--color-text-main)' }}>
                      ₹{calculateItemTotal(item)}
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem', textAlign: 'center' }}>
                      <button
                        type="button"
                        onClick={() => handleRemoveItem(idx)}
                        disabled={items.length <= 1 || isSubmitting}
                        style={{
                          background: 'none',
                          border: 'none',
                          cursor: items.length > 1 ? 'pointer' : 'not-allowed',
                          color: items.length > 1 ? 'var(--color-danger)' : 'var(--color-text-muted)',
                          padding: '0.25rem',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center'
                        }}
                        title={items.length <= 1 ? 'Order must contain at least one item' : 'Remove line item'}
                      >
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Order Grand Total Banner */}
        <div
          style={{
            backgroundColor: 'var(--color-bg-secondary)',
            padding: '0.875rem 1.25rem',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            border: '1px solid var(--color-border)'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--color-text-muted)', fontSize: '0.875rem' }}>
            <Calculator size={18} />
            <span>Calculated Order Value</span>
          </div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--color-primary)' }}>
            ₹{calculateGrandTotal()}
          </div>
        </div>

        {/* Footer Actions */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          {!isEditing && (
            <Button
              variant="outline"
              onClick={() => handleSave(true)}
              loading={isSubmitting}
              disabled={isSubmitting}
            >
              Submit Directly (Ordered)
            </Button>
          )}
          <Button
            variant="primary"
            onClick={() => handleSave(false)}
            loading={isSubmitting}
            disabled={isSubmitting}
          >
            {isEditing ? 'Save Changes' : 'Save as Draft'}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
