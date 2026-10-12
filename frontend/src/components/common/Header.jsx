import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Database, ShieldCheck, LogOut, User as UserIcon } from 'lucide-react';
import Badge from './Badge';

const roleBadgeVariant = {
  ADMIN: 'primary',
  INVENTORY_MANAGER: 'info',
  PURCHASE_MANAGER: 'warning',
  SALES_STAFF: 'success'
};

const roleDisplayName = {
  ADMIN: 'Administrator',
  INVENTORY_MANAGER: 'Inventory Manager',
  PURCHASE_MANAGER: 'Purchase Manager',
  SALES_STAFF: 'Sales Staff'
};

export default function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <header className="header">
      <div className="header-title-container">
        <h2 className="page-title">Smart Inventory Management System</h2>
      </div>

      <div className="header-actions" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8125rem', color: 'var(--color-success)', backgroundColor: 'var(--color-success-light)', padding: '0.25rem 0.75rem', borderRadius: 'var(--radius-full)', border: '1px solid var(--color-success-border)' }}>
          <Database size={14} />
          <span>PostgreSQL Active</span>
        </div>

        {user && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            paddingLeft: '0.75rem',
            borderLeft: '1px solid var(--border-color, #e2e8f0)'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <div style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                backgroundColor: 'var(--color-primary-light, #eff6ff)',
                color: 'var(--color-primary, #2563eb)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: '600',
                fontSize: '0.85rem'
              }}>
                <UserIcon size={16} />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', lineHeight: '1.2' }}>
                <span style={{ fontSize: '0.875rem', fontWeight: '600', color: 'var(--text-primary)' }}>
                  {user.fullName || user.username}
                </span>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                  <Badge variant={roleBadgeVariant[user.role] || 'info'}>
                    {roleDisplayName[user.role] || user.role}
                  </Badge>
                </span>
              </div>
            </div>

            <button
              onClick={handleLogout}
              title="Sign Out"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.35rem',
                padding: '0.4rem 0.75rem',
                borderRadius: '6px',
                border: '1px solid var(--border-color, #e2e8f0)',
                backgroundColor: '#ffffff',
                color: '#ef4444',
                fontSize: '0.8125rem',
                fontWeight: '500',
                cursor: 'pointer',
                transition: 'all 0.15s ease'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = '#fef2f2';
                e.currentTarget.style.borderColor = '#fca5a5';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = '#ffffff';
                e.currentTarget.style.borderColor = 'var(--border-color, #e2e8f0)';
              }}
            >
              <LogOut size={14} />
              <span>Sign Out</span>
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
