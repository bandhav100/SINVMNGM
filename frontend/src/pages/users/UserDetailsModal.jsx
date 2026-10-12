import React from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import { User, Mail, Shield, Calendar, CheckCircle2, XCircle } from 'lucide-react';

const roleBadgeVariant = {
  ADMIN: 'primary',
  INVENTORY_MANAGER: 'info',
  PURCHASE_MANAGER: 'warning',
  SALES_STAFF: 'success'
};

const roleDescriptions = {
  ADMIN: 'Full system administration, user management, and system settings.',
  INVENTORY_MANAGER: 'Products, categories, and inventory stock management operations.',
  PURCHASE_MANAGER: 'Suppliers, purchase orders, and supplier relationship management.',
  SALES_STAFF: 'Sales order workflows and product catalog viewing.'
};

export default function UserDetailsModal({ isOpen, onClose, user, onEdit }) {
  if (!user) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`User Details: ${user.username}`}
      maxWidth="500px"
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '1rem',
          padding: '1rem',
          backgroundColor: 'var(--bg-secondary, #f8fafc)',
          borderRadius: '8px',
          border: '1px solid var(--border-color, #e2e8f0)'
        }}>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '50%',
            backgroundColor: 'var(--color-primary-light, #eff6ff)',
            color: 'var(--color-primary, #2563eb)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <User size={24} />
          </div>
          <div>
            <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: '600' }}>{user.fullName}</h3>
            <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>@{user.username}</span>
          </div>
          <div style={{ marginLeft: 'auto' }}>
            {user.active ? (
              <Badge variant="success">Active</Badge>
            ) : (
              <Badge variant="danger">Deactivated</Badge>
            )}
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '0.85rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.9rem' }}>
            <Mail size={18} style={{ color: 'var(--text-secondary)' }} />
            <span style={{ color: 'var(--text-secondary)', width: '90px' }}>Email:</span>
            <span style={{ fontWeight: '500' }}>{user.email}</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.9rem' }}>
            <Shield size={18} style={{ color: 'var(--text-secondary)' }} />
            <span style={{ color: 'var(--text-secondary)', width: '90px' }}>Role:</span>
            <Badge variant={roleBadgeVariant[user.role] || 'info'}>{user.role}</Badge>
          </div>

          <div style={{
            marginLeft: '2.2rem',
            padding: '0.5rem 0.75rem',
            backgroundColor: '#f1f5f9',
            borderRadius: '6px',
            fontSize: '0.8rem',
            color: 'var(--text-secondary)'
          }}>
            {roleDescriptions[user.role] || 'Standard user access.'}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.9rem' }}>
            <Calendar size={18} style={{ color: 'var(--text-secondary)' }} />
            <span style={{ color: 'var(--text-secondary)', width: '90px' }}>Created:</span>
            <span>{user.createdAt ? new Date(user.createdAt).toLocaleString() : 'N/A'}</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.9rem' }}>
            <Calendar size={18} style={{ color: 'var(--text-secondary)' }} />
            <span style={{ color: 'var(--text-secondary)', width: '90px' }}>Updated:</span>
            <span>{user.updatedAt ? new Date(user.updatedAt).toLocaleString() : 'N/A'}</span>
          </div>
        </div>

        <div style={{
          marginTop: '1.25rem',
          display: 'flex',
          justifyContent: 'flex-end',
          gap: '0.75rem'
        }}>
          <Button variant="outline" onClick={onClose}>
            Close
          </Button>
          {onEdit && (
            <Button
              variant="primary"
              onClick={() => {
                onClose();
                onEdit(user);
              }}
            >
              Edit User
            </Button>
          )}
        </div>
      </div>
    </Modal>
  );
}
