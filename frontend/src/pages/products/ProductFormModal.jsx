import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import AlertBanner from '../../components/common/AlertBanner';
import { categoryApi } from '../../api/categoryApi';

export default function ProductFormModal({
  isOpen,
  onClose,
  onSubmit,
  product = null,
  isSubmitting = false,
  backendErrors = null
}) {
  const isEditing = Boolean(product);

  const [categories, setCategories] = useState([]);
  const [isLoadingCategories, setIsLoadingCategories] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    sku: '',
    description: '',
    price: '',
    quantity: '',
    reorderLevel: '10',
    categoryId: ''
  });

  const [clientErrors, setClientErrors] = useState({});

  useEffect(() => {
    if (isOpen) {
      setIsLoadingCategories(true);
      categoryApi.getAllCategories()
        .then((cats) => setCategories(cats || []))
        .catch((err) => console.error('Failed to load categories', err))
        .finally(() => setIsLoadingCategories(false));
    }
  }, [isOpen]);

  useEffect(() => {
    if (product) {
      setFormData({
        name: product.name || '',
        sku: product.sku || '',
        description: product.description || '',
        price: product.price !== undefined ? String(product.price) : '',
        quantity: product.quantity !== undefined ? String(product.quantity) : '',
        reorderLevel: product.reorderLevel !== undefined ? String(product.reorderLevel) : '10',
        categoryId: product.categoryId !== undefined && product.categoryId !== null ? String(product.categoryId) : ''
      });
    } else {
      setFormData({
        name: '',
        sku: '',
        description: '',
        price: '',
        quantity: '',
        reorderLevel: '10',
        categoryId: ''
      });
    }
    setClientErrors({});
  }, [product, isOpen]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (clientErrors[name]) {
      setClientErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const validate = () => {
    const errors = {};
    if (!formData.name.trim()) {
      errors.name = 'Product name is required';
    }
    if (!formData.sku.trim()) {
      errors.sku = 'SKU is required';
    } else if (!/^[A-Za-z0-9_\-]+$/.test(formData.sku.trim())) {
      errors.sku = 'SKU must contain only letters, numbers, dashes, or underscores';
    }

    if (formData.price === '' || isNaN(Number(formData.price)) || Number(formData.price) < 0) {
      errors.price = 'Price must be a non-negative number';
    }

    if (formData.quantity === '' || isNaN(Number(formData.quantity)) || Number(formData.quantity) < 0 || !Number.isInteger(Number(formData.quantity))) {
      errors.quantity = 'Quantity must be a non-negative integer';
    }

    if (formData.reorderLevel !== '' && (isNaN(Number(formData.reorderLevel)) || Number(formData.reorderLevel) < 0 || !Number.isInteger(Number(formData.reorderLevel)))) {
      errors.reorderLevel = 'Reorder level must be a non-negative integer';
    }

    setClientErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;

    const payload = {
      name: formData.name.trim(),
      sku: formData.sku.trim().toUpperCase(),
      description: formData.description.trim() || null,
      price: parseFloat(formData.price),
      quantity: parseInt(formData.quantity, 10),
      reorderLevel: formData.reorderLevel !== '' ? parseInt(formData.reorderLevel, 10) : 10,
      categoryId: formData.categoryId ? Number(formData.categoryId) : null
    };

    onSubmit(payload);
  };

  const getFieldError = (fieldName) => {
    if (clientErrors[fieldName]) return clientErrors[fieldName];
    if (backendErrors && backendErrors.validationErrors && backendErrors.validationErrors[fieldName]) {
      return backendErrors.validationErrors[fieldName];
    }
    return null;
  };

  const generalError = backendErrors && !backendErrors.validationErrors ? backendErrors.message : null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? `Edit Product: ${product.name}` : 'Create New Product'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit} loading={isSubmitting}>
            {isEditing ? 'Save Changes' : 'Create Product'}
          </Button>
        </>
      }
    >
      {generalError && <AlertBanner type="error" message={generalError} />}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <Input
          label="Product Name"
          name="name"
          value={formData.name}
          onChange={handleChange}
          required
          placeholder="e.g. Ergonomic Office Chair"
          error={getFieldError('name')}
        />

        <div className="form-row">
          <Input
            label="SKU (Stock Keeping Unit)"
            name="sku"
            value={formData.sku}
            onChange={handleChange}
            required
            placeholder="e.g. CHAIR-ERGO-001"
            helperText="Alphanumeric with dashes or underscores. Must be unique."
            error={getFieldError('sku')}
          />

          <div className="form-group">
            <label htmlFor="categoryId" className="form-label">
              Category
            </label>
            <select
              id="categoryId"
              name="categoryId"
              value={formData.categoryId}
              onChange={handleChange}
              className="form-input"
              style={{ width: '100%', cursor: 'pointer' }}
              disabled={isLoadingCategories}
            >
              <option value="">— Uncategorized —</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
            {getFieldError('categoryId') && (
              <span className="form-error">{getFieldError('categoryId')}</span>
            )}
          </div>
        </div>

        <div className="form-row">
          <Input
            label="Price (₹)"
            name="price"
            type="number"
            step="0.01"
            min="0"
            value={formData.price}
            onChange={handleChange}
            required
            placeholder="0.00"
            error={getFieldError('price')}
          />

          <Input
            label="Initial Quantity"
            name="quantity"
            type="number"
            step="1"
            min="0"
            value={formData.quantity}
            onChange={handleChange}
            required
            placeholder="0"
            error={getFieldError('quantity')}
          />
        </div>

        <Input
          label="Reorder Level (Alert Threshold)"
          name="reorderLevel"
          type="number"
          step="1"
          min="0"
          value={formData.reorderLevel}
          onChange={handleChange}
          placeholder="10"
          helperText="Triggers low-stock alert when quantity falls below or equal to this."
          error={getFieldError('reorderLevel')}
        />

        <div className="form-group">
          <label htmlFor="description" className="form-label">
            Description
          </label>
          <textarea
            id="description"
            name="description"
            rows="3"
            value={formData.description}
            onChange={handleChange}
            placeholder="Detailed product information, specifications, or notes..."
            className={`form-textarea ${getFieldError('description') ? 'has-error' : ''}`}
          />
          {getFieldError('description') && (
            <span className="form-error">{getFieldError('description')}</span>
          )}
        </div>
      </form>
    </Modal>
  );
}
