import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import AlertBanner from '../../components/common/AlertBanner';

export default function SupplierFormModal({
  isOpen,
  onClose,
  onSubmit,
  supplier = null,
  isSubmitting = false,
  backendErrors = null
}) {
  const isEditing = Boolean(supplier);

  const [formData, setFormData] = useState({
    name: '',
    contactPerson: '',
    email: '',
    phone: '',
    address: '',
    active: true
  });

  const [clientErrors, setClientErrors] = useState({});

  useEffect(() => {
    if (supplier) {
      setFormData({
        name: supplier.name || '',
        contactPerson: supplier.contactPerson || '',
        email: supplier.email || '',
        phone: supplier.phone || '',
        address: supplier.address || '',
        active: supplier.active !== undefined ? supplier.active : true
      });
    } else {
      setFormData({
        name: '',
        contactPerson: '',
        email: '',
        phone: '',
        address: '',
        active: true
      });
    }
    setClientErrors({});
  }, [supplier, isOpen]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
    if (clientErrors[name]) {
      setClientErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const validate = () => {
    const errors = {};
    if (!formData.name.trim()) {
      errors.name = 'Company or vendor name is required';
    } else if (formData.name.trim().length > 100) {
      errors.name = 'Company name cannot exceed 100 characters';
    }

    if (formData.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      errors.email = 'Please provide a valid email address';
    }

    if (formData.phone.trim() && formData.phone.trim().length > 20) {
      errors.phone = 'Phone number cannot exceed 20 characters';
    }

    if (formData.address.trim() && formData.address.trim().length > 255) {
      errors.address = 'Address cannot exceed 255 characters';
    }

    setClientErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;

    const payload = {
      name: formData.name.trim(),
      contactPerson: formData.contactPerson.trim() || null,
      email: formData.email.trim() || null,
      phone: formData.phone.trim() || null,
      address: formData.address.trim() || null,
      active: formData.active
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
      title={isEditing ? `Edit Supplier: ${supplier.name}` : 'Add New Supplier'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit} loading={isSubmitting}>
            {isEditing ? 'Save Changes' : 'Create Supplier'}
          </Button>
        </>
      }
    >
      {generalError && <AlertBanner type="error" message={generalError} />}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <Input
          label="Company / Vendor Name"
          name="name"
          value={formData.name}
          onChange={handleChange}
          required
          placeholder="e.g. Apex Tech Supplies Pvt Ltd"
          error={getFieldError('name')}
        />

        <div className="form-row">
          <Input
            label="Contact Person"
            name="contactPerson"
            value={formData.contactPerson}
            onChange={handleChange}
            placeholder="e.g. Rajesh Kumar"
            error={getFieldError('contactPerson')}
          />

          <Input
            label="Email Address"
            name="email"
            type="email"
            value={formData.email}
            onChange={handleChange}
            placeholder="e.g. procurement@apexsupplies.com"
            error={getFieldError('email')}
          />
        </div>

        <div className="form-row">
          <Input
            label="Phone Number"
            name="phone"
            value={formData.phone}
            onChange={handleChange}
            placeholder="e.g. +91 98765 43210"
            error={getFieldError('phone')}
          />

          <div className="form-group" style={{ display: 'flex', alignItems: 'center', height: '100%', paddingTop: '1.75rem' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', cursor: 'pointer', fontSize: '0.875rem', fontWeight: '500' }}>
              <input
                type="checkbox"
                name="active"
                checked={formData.active}
                onChange={handleChange}
                style={{ width: '1.125rem', height: '1.125rem', accentColor: 'var(--color-primary)' }}
              />
              <span>Active Partner (Available for POs)</span>
            </label>
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="address" className="form-label">
            Office / Warehouse Address
          </label>
          <textarea
            id="address"
            name="address"
            rows="3"
            value={formData.address}
            onChange={handleChange}
            placeholder="Street address, city, state, postal code..."
            className={`form-textarea ${getFieldError('address') ? 'has-error' : ''}`}
          />
          {getFieldError('address') && (
            <span className="form-error">{getFieldError('address')}</span>
          )}
        </div>
      </form>
    </Modal>
  );
}
