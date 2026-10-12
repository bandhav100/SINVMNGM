import React from 'react';

export default function Input({
  label,
  id,
  name,
  type = 'text',
  value,
  onChange,
  error,
  required = false,
  placeholder,
  helperText,
  disabled = false,
  min,
  step,
  ...props
}) {
  return (
    <div className="form-group">
      {label && (
        <label htmlFor={id || name} className="form-label">
          {label} {required && <span className="required">*</span>}
        </label>
      )}
      <input
        id={id || name}
        name={name}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        disabled={disabled}
        min={min}
        step={step}
        className={`form-input ${error ? 'has-error' : ''}`}
        {...props}
      />
      {error && <span className="form-error">{error}</span>}
      {helperText && !error && <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>{helperText}</span>}
    </div>
  );
}
