import React, { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import AlertBanner from '../../components/common/AlertBanner';
import { LogIn, ShieldCheck, Boxes } from 'lucide-react';

export default function LoginPage() {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const { login, setupRequired, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/dashboard';

  React.useEffect(() => {
    if (isAuthenticated) {
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, from]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!identifier.trim() || !password) {
      setError('Please enter your username/email and password.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      await login({ usernameOrEmail: identifier.trim(), password });
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message || 'Authentication failed. Please verify your credentials.');
    } finally {
      setSubmitting(false);
    }
  };

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
        width: '100%',
        maxWidth: '440px',
        backgroundColor: 'var(--card-bg, #ffffff)',
        borderRadius: '12px',
        border: '1px solid var(--border-color, #e2e8f0)',
        boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.08)',
        padding: '2.5rem 2rem'
      }}>
        {/* Brand Header */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '56px',
            height: '56px',
            borderRadius: '14px',
            background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',
            color: '#ffffff',
            marginBottom: '1rem',
            boxShadow: '0 4px 12px rgba(37, 99, 235, 0.3)'
          }}>
            <Boxes size={32} />
          </div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: '700', color: 'var(--text-primary)', margin: 0 }}>
            SIMS Portal
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.35rem' }}>
            Smart Inventory Management System
          </p>
        </div>

        {setupRequired && (
          <div style={{ marginBottom: '1.5rem' }}>
            <AlertBanner
              type="info"
              message="First-time setup detected: No system administrator account exists yet."
            />
            <div style={{ textAlign: 'center', marginTop: '0.5rem' }}>
              <Link
                to="/setup-admin"
                style={{
                  color: 'var(--primary-color, #2563eb)',
                  fontWeight: '600',
                  fontSize: '0.875rem',
                  textDecoration: 'none'
                }}
              >
                Go to Administrator Setup &rarr;
              </Link>
            </div>
          </div>
        )}

        {error && (
          <div style={{ marginBottom: '1.25rem' }}>
            <AlertBanner type="error" message={error} onClose={() => setError(null)} />
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <Input
              id="login-identifier"
              label="Username or Email"
              type="text"
              placeholder="e.g. admin or staff@sims.local"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              required
              disabled={submitting}
              autoFocus
            />

            <Input
              id="login-password"
              label="Password"
              type="password"
              placeholder="••••••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={submitting}
            />

            <Button
              type="submit"
              variant="primary"
              disabled={submitting}
              style={{
                width: '100%',
                marginTop: '0.75rem',
                padding: '0.75rem',
                fontWeight: '600',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem'
              }}
            >
              <LogIn size={18} />
              {submitting ? 'Signing in...' : 'Sign In'}
            </Button>
          </div>
        </form>

        <div style={{
          marginTop: '1.25rem',
          textAlign: 'center',
          fontSize: '0.875rem'
        }}>
          <span style={{ color: 'var(--text-secondary)' }}>Need a new company workspace? </span>
          <Link
            to="/create-inventory"
            style={{
              color: 'var(--primary-color, #2563eb)',
              fontWeight: '600',
              textDecoration: 'none'
            }}
          >
            Register Company
          </Link>
        </div>

        <div style={{
          marginTop: '1.5rem',
          paddingTop: '1.25rem',
          borderTop: '1px solid var(--border-color, #f1f5f9)',
          textAlign: 'center',
          fontSize: '0.8rem',
          color: 'var(--text-secondary)'
        }}>
          <ShieldCheck size={14} style={{ verticalAlign: 'middle', marginRight: '4px' }} />
          Protected by Role-Based Access Control & Spring Security
        </div>
      </div>
    </div>
  );
}
