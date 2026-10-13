import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import AlertBanner from '../../components/common/AlertBanner';

const ROLES = [
  { value: 'ADMIN', label: 'Administrator (Full Access)' },
  { value: 'INVENTORY_MANAGER', label: 'Inventory Manager (Products & Stock)' },
  { value: 'PURCHASE_MANAGER', label: 'Purchase Manager (Suppliers & Orders)' },
  { value: 'SALES_STAFF', label: 'Sales Staff (Orders & Product Viewing)' }
];

export default function UserFormModal({
  isOpen,
  onClose,
  onSubmit,
  user = null,
  currentUser = null,
  isSubmitting = false,
  backendErrors = null
}) {
  const isEditing = Boolean(user);
  const isSelf = isEditing && currentUser?.username?.toLowerCase() === user?.username?.toLowerCase();

  const [formData, setFormData] = useState({
    username: '',
    email: '',
    fullName: '',
    role: 'INVENTORY_MANAGER',
    password: ''
  });

  const [clientErrors, setClientErrors] = useState({});

  useEffect(() => {
    if (user) {
      setFormData({
        username: user.username || '',
        email: user.email || '',
        fullName: user.fullName || '',
        role: user.role || 'INVENTORY_MANAGER',
        password: ''
      });
    } else {
      setFormData({
        username: '',
        email: '',
        fullName: '',
        role: 'INVENTORY_MANAGER',
        password: ''
      });
    }
    setClientErrors({});
  }, [user, isOpen]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (clientErrors[name]) {
      setClientErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const validate = () => {
    const errors = {};
    if (!isEditing && !formData.username.trim()) {
      errors.username = 'Username is required';
    }
    if (!formData.email.trim()) {
      errors.email = 'Email address is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      errors.email = 'Valid email address is required';
    }
    if (!formData.fullName.trim()) {
      errors.fullName = 'Full Name is required';
    }
    if (!isEditing && (!formData.password || formData.password.length < 8)) {
      errors.password = 'Password must be at least 8 characters';
    }
    if (isEditing && formData.password && formData.password.length < 8) {
      errors.password = 'New password must be at least 8 characters';
    }
    setClientErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;

    if (isEditing) {
      const payload = {
        email: formData.email.trim(),
        fullName: formData.fullName.trim(),
        role: formData.role
      };
      if (formData.password.trim()) {
        payload.password = formData.password.trim();
      }
      onSubmit(payload);
    } else {
      onSubmit({
        username: formData.username.trim(),
        email: formData.email.trim(),
        fullName: formData.fullName.trim(),
        role: formData.role,
        password: formData.password
      });
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? `Edit User: ${user?.username}` : 'Add New Staff Account'}
      maxWidth="540px"
    >
      <form onSubmit={handleSubmit}>
        {backendErrors && typeof backendErrors === 'string' && (
          <div style={{ marginBottom: '1rem' }}>
            <AlertBanner type="error" message={backendErrors} />
          </div>
        )}

        {isSelf && (
          <div style={{ marginBottom: '1rem' }}>
            <AlertBanner
              type="info"
              message="Notice: You are editing your own account. Administrators cannot change their own role."
            />
          </div>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {!isEditing ? (
            <Input
              id="user-username"
              name="username"
              label="Username"
              placeholder="e.g. jdoe"
              value={formData.username}
              onChange={handleChange}
              error={clientErrors.username || backendErrors?.username}
              required
              disabled={isSubmitting}
              autoFocus
            />
          ) : (
            <div>
              <label style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--text-secondary)' }}>
                Username
              </label>
              <div style={{
                padding: '0.5rem 0.75rem',
                backgroundColor: 'var(--bg-secondary, #f8fafc)',
                borderRadius: '6px',
                border: '1px solid var(--border-color, #e2e8f0)',
                color: 'var(--text-primary)',
                fontWeight: '600'
              }}>
                {user?.username}
              </div>
            </div>
          )}

          <Input
            id="user-fullname"
            name="fullName"
            label="Full Name"
            placeholder="e.g. Jane Doe"
            value={formData.fullName}
            onChange={handleChange}
            error={clientErrors.fullName || backendErrors?.fullName}
            required
            disabled={isSubmitting}
            autoFocus={isEditing}
          />

          <Input
            id="user-email"
            name="email"
            label="Email Address"
            type="email"
            placeholder="e.g. jdoe@sims.local"
            value={formData.email}
            onChange={handleChange}
            error={clientErrors.email || backendErrors?.email}
            required
            disabled={isSubmitting}
          />

          <div>
            <label
              htmlFor="user-role"
              style={{
                display: 'block',
                fontSize: '0.875rem',
                fontWeight: '500',
                color: 'var(--text-primary)',
                marginBottom: '0.35rem'
              }}
            >
              Assigned Role
            </label>
            <select
              id="user-role"
              name="role"
              value={formData.role}
              onChange={handleChange}
              disabled={isSubmitting || isSelf}
              style={{
                width: '100%',
                padding: '0.625rem 0.75rem',
                borderRadius: '6px',
                border: '1px solid var(--border-color, #cbd5e1)',
                backgroundColor: isSelf ? '#f1f5f9' : '#ffffff',
                fontSize: '0.9rem',
                color: 'var(--text-primary)',
                cursor: isSelf ? 'not-allowed' : 'pointer'
              }}
            >
              {ROLES.map((r) => (
                <option key={r.value} value={r.value}>
                  {r.label}
                </option>
              ))}
            </select>
            {isSelf && (
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem', display: 'block' }}>
                Self-demotion is restricted for security. Another admin must perform role updates on your account.
              </span>
            )}
          </div>

          <Input
            id="user-password"
            name="password"
            label={isEditing ? 'New Password (leave blank to keep current)' : 'Account Password'}
            type="password"
            placeholder={isEditing ? 'Leave blank to preserve password' : 'Min. 8 characters'}
            value={formData.password}
            onChange={handleChange}
            error={clientErrors.password || backendErrors?.password}
            required={!isEditing}
            disabled={isSubmitting}
          />
        </div>

        <div style={{
          marginTop: '1.75rem',
          display: 'flex',
          justifyContent: 'flex-end',
          gap: '0.75rem'
        }}>
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Saving...' : isEditing ? 'Update User' : 'Create User'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
