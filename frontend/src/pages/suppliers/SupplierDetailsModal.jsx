import React from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import { Truck, User, Mail, Phone, MapPin, Calendar, Edit } from 'lucide-react';

export default function SupplierDetailsModal({ isOpen, onClose, supplier, onEdit }) {
  if (!supplier) return null;

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
      title="Supplier Profile"
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
                onEdit(supplier);
              }}
            >
              Edit Supplier
            </Button>
          )}
        </>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--color-text-primary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Truck size={20} style={{ color: 'var(--color-primary)' }} />
              {supplier.name}
            </h3>
            {supplier.contactPerson && (
              <p style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', gap: '0.375rem', marginTop: '0.25rem' }}>
                <User size={14} />
                <span>Contact: <strong>{supplier.contactPerson}</strong></span>
              </p>
            )}
          </div>

          <div>
            {supplier.active ? (
              <Badge variant="success">Active Vendor</Badge>
            ) : (
              <Badge variant="neutral">Inactive</Badge>
            )}
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1rem' }}>
          <div style={{ border: '1px solid var(--color-border)', padding: '0.875rem', borderRadius: 'var(--radius-md)' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
              <Mail size={13} /> Email Address
            </span>
            <span style={{ fontSize: '0.9375rem', fontWeight: '600', color: 'var(--color-text-primary)', display: 'block', marginTop: '0.25rem' }}>
              {supplier.email ? (
                <a href={`mailto:${supplier.email}`} style={{ color: 'var(--color-primary)', textDecoration: 'none' }}>
                  {supplier.email}
                </a>
              ) : (
                '—'
              )}
            </span>
          </div>

          <div style={{ border: '1px solid var(--color-border)', padding: '0.875rem', borderRadius: 'var(--radius-md)' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
              <Phone size={13} /> Phone Number
            </span>
            <span style={{ fontSize: '0.9375rem', fontWeight: '600', color: 'var(--color-text-primary)', display: 'block', marginTop: '0.25rem' }}>
              {supplier.phone ? (
                <a href={`tel:${supplier.phone}`} style={{ color: 'var(--color-primary)', textDecoration: 'none' }}>
                  {supplier.phone}
                </a>
              ) : (
                '—'
              )}
            </span>
          </div>
        </div>

        {supplier.address && (
          <div style={{ padding: '0.875rem 1rem', backgroundColor: 'var(--color-surface-subtle)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem' }}>
            <span style={{ fontWeight: '600', color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', gap: '0.375rem', marginBottom: '0.25rem' }}>
              <MapPin size={14} /> Address
            </span>
            <p style={{ color: 'var(--color-text-primary)', whiteSpace: 'pre-wrap' }}>{supplier.address}</p>
          </div>
        )}

        <div style={{ borderTop: '1px solid var(--color-border)', paddingTop: '0.75rem', display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
            <Calendar size={13} /> Created: {formatDate(supplier.createdAt)}
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
            <Calendar size={13} /> Last Updated: {formatDate(supplier.updatedAt)}
          </span>
        </div>
      </div>
    </Modal>
  );
}
