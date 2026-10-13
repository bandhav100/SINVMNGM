import React from 'react';

export default function CustomTooltip({ active, payload, label, formatter, valuePrefix = '', valueSuffix = '' }) {
  if (active && payload && payload.length) {
    return (
      <div
        style={{
          backgroundColor: '#0f172a',
          color: '#f8fafc',
          padding: '0.6rem 0.85rem',
          borderRadius: '8px',
          boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.25), 0 4px 6px -4px rgba(0, 0, 0, 0.2)',
          fontSize: '0.8rem',
          border: '1px solid #334155',
          lineHeight: '1.4',
          pointerEvents: 'none',
          zIndex: 100
        }}
      >
        {label && (
          <p style={{ fontWeight: '600', marginBottom: '0.35rem', color: '#94a3b8', borderBottom: '1px solid #1e293b', paddingBottom: '0.2rem' }}>
            {label}
          </p>
        )}
        {payload.map((item, index) => {
          const formattedVal = formatter
            ? formatter(item.value, item.name, item)
            : `${valuePrefix}${typeof item.value === 'number' ? item.value.toLocaleString() : item.value}${valueSuffix}`;

          return (
            <div key={`tooltip-item-${index}`} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: index > 0 ? '0.25rem' : 0 }}>
              <span
                style={{
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  backgroundColor: item.color || item.payload?.fill || '#3b82f6',
                  display: 'inline-block'
                }}
              />
              <span style={{ color: '#cbd5e1' }}>{item.name || item.dataKey}:</span>
              <span style={{ fontWeight: '700', color: '#ffffff', marginLeft: 'auto' }}>
                {formattedVal}
              </span>
            </div>
          );
        })}
      </div>
    );
  }
  return null;
}
