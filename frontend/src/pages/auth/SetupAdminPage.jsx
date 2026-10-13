import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import AlertBanner from '../../components/common/AlertBanner';
import { ShieldAlert, UserCheck, CheckCircle2, Lock } from 'lucide-react';

export default function SetupAdminPage() {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    fullName: '',
    password: '',
    confirmPassword: ''
  });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const { setupAdmin, setupRequired } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (formData.password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match. Please re-enter.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      await setupAdmin({
        username: formData.username.trim(),
        email: formData.email.trim(),
        fullName: formData.fullName.trim(),
        password: formData.password
      });
      navigate('/products', { replace: true });
    } catch (err) {
      setError(err.message || 'Administrator setup failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (setupRequired === false) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'var(--bg-primary)',
        padding: '1.5rem'
      }}>
        <div style={{
          maxWidth: '460px',
          width: '100%',
          backgroundColor: 'var(--card-bg, #ffffff)',
          borderRadius: '12px',
          padding: '2.5rem',
          textAlign: 'center',
          boxShadow: '0 10px 25px -5px rgba(0,0,0,0.08)',
          border: '1px solid var(--border-color, #e2e8f0)'
        }}>
          <div style={{
            display: 'inline-flex',
            padding: '12px',
            borderRadius: '50%',
            backgroundColor: '#ecfdf5',
            color: '#059669',
            marginBottom: '1rem'
          }}>
            <CheckCircle2 size={40} />
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: '700', marginBottom: '0.5rem' }}>
            Setup Already Completed
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
            The primary system administrator account has already been initialized. Public administrator setup is permanently disabled.
          </p>
          <Link to="/login">
            <Button variant="primary" style={{ width: '100%' }}>
              Proceed to Sign In
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: 'var(--bg-primary)',
      padding: '2rem 1.5rem'
    }}>
      <div style={{
        width: '100%',
        maxWidth: '520px',
        backgroundColor: 'var(--card-bg, #ffffff)',
        borderRadius: '12px',
        border: '1px solid var(--border-color, #e2e8f0)',
        boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.08)',
        padding: '2.5rem'
      }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '56px',
            height: '56px',
            borderRadius: '14px',
            background: 'linear-gradient(135deg, #10b981 0%, #047857 100%)',
            color: '#ffffff',
            marginBottom: '1rem',
            boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)'
          }}>
            <Lock size={30} />
          </div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: '700', color: 'var(--text-primary)', margin: 0 }}>
            Initial Administrator Setup
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '0.35rem' }}>
            Configure the primary master account for Smart Inventory Management System.
          </p>
        </div>

        <div style={{ marginBottom: '1.5rem' }}>
          <AlertBanner
            type="info"
            message="Security Notice: This is a one-time setup workflow. Once the primary administrator account is created, public setup is permanently locked and further administrators can only be created by an active admin."
          />
        </div>

        {error && (
          <div style={{ marginBottom: '1.25rem' }}>
            <AlertBanner type="error" message={error} onClose={() => setError(null)} />
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <Input
              id="setup-full-name"
              name="fullName"
              label="Administrator Full Name"
              type="text"
              placeholder="e.g. System Administrator"
              value={formData.fullName}
              onChange={handleChange}
              required
              disabled={submitting}
              autoFocus
            />

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <Input
                id="setup-username"
                name="username"
                label="Username"
                type="text"
                placeholder="e.g. admin"
                value={formData.username}
                onChange={handleChange}
                required
                disabled={submitting}
              />

              <Input
                id="setup-email"
                name="email"
                label="Email Address"
                type="email"
                placeholder="admin@sims.local"
                value={formData.email}
                onChange={handleChange}
                required
                disabled={submitting}
              />
            </div>

            <Input
              id="setup-password"
              name="password"
              label="Master Password (Min. 8 characters)"
              type="password"
              placeholder="••••••••••••"
              value={formData.password}
              onChange={handleChange}
              required
              disabled={submitting}
            />

            <Input
              id="setup-confirm-password"
              name="confirmPassword"
              label="Confirm Password"
              type="password"
              placeholder="••••••••••••"
              value={formData.confirmPassword}
              onChange={handleChange}
              required
              disabled={submitting}
            />

            <Button
              type="submit"
              variant="primary"
              disabled={submitting}
              style={{
                width: '100%',
                marginTop: '1rem',
                padding: '0.75rem',
                fontWeight: '600',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                background: 'linear-gradient(135deg, #10b981 0%, #047857 100%)'
              }}
            >
              <UserCheck size={18} />
              {submitting ? 'Initializing Master Administrator...' : 'Create Master Administrator'}
            </Button>
          </div>
        </form>

        <div style={{
          marginTop: '1.75rem',
          textAlign: 'center',
          fontSize: '0.85rem'
        }}>
          <span style={{ color: 'var(--text-secondary)' }}>Already have an account? </span>
          <Link
            to="/login"
            style={{
              color: 'var(--primary-color, #2563eb)',
              fontWeight: '600',
              textDecoration: 'none'
            }}
          >
            Sign In here
          </Link>
        </div>
      </div>
    </div>
  );
}
