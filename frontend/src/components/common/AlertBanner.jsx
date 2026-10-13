import React from 'react';
import { CheckCircle2, AlertTriangle, AlertCircle, X } from 'lucide-react';

export default function AlertBanner({ type = 'success', message, onDismiss }) {
  if (!message) return null;

  const Icon = type === 'success' ? CheckCircle2 : type === 'warning' ? AlertTriangle : AlertCircle;

  return (
    <div className={`alert-banner ${type}`}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <Icon size={18} />
        <span>{message}</span>
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'currentColor' }}
          aria-label="Dismiss alert"
        >
          <X size={16} />
        </button>
      )}
    </div>
  );
}
