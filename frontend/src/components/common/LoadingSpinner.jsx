import React from 'react';

export default function LoadingSpinner({ size = 'md', message }) {
  const dimension = size === 'sm' ? 16 : size === 'lg' ? 32 : 24;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.75rem', padding: '2rem' }}>
      <span className="loading-spinner" style={{ width: dimension, height: dimension }} />
      {message && <p style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)' }}>{message}</p>}
    </div>
  );
}
