import React from 'react';

export default function ChartCard({ title, subtitle, icon: Icon, iconColor = 'var(--color-primary)', children, action }) {
  return (
    <div
      className="card"
      style={{
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-lg)',
        background: 'var(--color-surface)',
        boxShadow: 'var(--shadow-sm)',
        padding: '1.25rem',
        display: 'flex',
        flexDirection: 'column',
        gap: '1rem',
        minHeight: '340px'
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          {Icon && (
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-surface-subtle)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: iconColor
              }}
            >
              <Icon size={18} />
            </div>
          )}
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: '600', color: 'var(--color-text-primary)', margin: 0 }}>
              {title}
            </h3>
            {subtitle && (
              <p style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', margin: '0.15rem 0 0 0' }}>
                {subtitle}
              </p>
            )}
          </div>
        </div>
        {action && <div>{action}</div>}
      </div>

      <div style={{ flex: 1, minHeight: '230px', width: '100%', position: 'relative' }}>
        {children}
      </div>
    </div>
  );
}
