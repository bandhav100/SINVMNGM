import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import AlertBanner from '../../components/common/AlertBanner';
import {
  Building2,
  Lock,
  ArrowRight,
  Sparkles,
  ShieldCheck,
  Briefcase,
  UserCheck,
  Phone,
  Mail,
  ArrowLeft
} from 'lucide-react';

export default function CreateInventoryPage() {
  const { registerCompany } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    companyName: '',
    fullName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: ''
  });

  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.companyName.trim()) {
      setError('Company name is required to establish your inventory workspace.');
      return;
    }

    if (!formData.fullName.trim()) {
      setError('Administrator full name is required.');
      return;
    }

    if (!formData.email.trim()) {
      setError('Business email is required.');
      return;
    }

    if (formData.password.length < 8) {
      setError('Administrator password must be at least 8 characters long.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match. Please re-enter.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      await registerCompany({
        companyName: formData.companyName.trim(),
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim() || undefined,
        password: formData.password
      });

      navigate('/dashboard', { replace: true });
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to create company workspace. Please try again.';
      setError(msg);
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
      backgroundColor: '#0b0f19',
      padding: '2.5rem 1.5rem',
      color: '#ffffff'
    }}>
      <div style={{
        width: '100%',
        maxWidth: '560px',
        backgroundColor: '#111827',
        borderRadius: '16px',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.6)',
        padding: '2.75rem 2.25rem'
      }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '60px',
            height: '60px',
            borderRadius: '14px',
            background: 'linear-gradient(135deg, #6366f1 0%, #3b82f6 100%)',
            color: '#ffffff',
            marginBottom: '1rem',
            boxShadow: '0 6px 18px rgba(99, 102, 241, 0.4)'
          }}>
            <Building2 size={32} />
          </div>

          <h1 style={{ fontSize: '1.75rem', fontWeight: '800', letterSpacing: '-0.025em', margin: 0 }}>
            Register New Company
          </h1>

          <p style={{ color: '#94a3b8', fontSize: '0.9rem', marginTop: '0.4rem', lineHeight: '1.5' }}>
            Establish an independent inventory workspace for your organization with its own dedicated administrator.
          </p>
        </div>

        {error && (
          <div style={{ marginBottom: '1.25rem' }}>
            <AlertBanner type="error" message={error} onClose={() => setError(null)} />
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {/* Section 1: Company Profile */}
            <div style={{
              padding: '1.25rem',
              backgroundColor: 'rgba(255, 255, 255, 0.03)',
              borderRadius: '10px',
              border: '1px solid rgba(255, 255, 255, 0.06)'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                <Briefcase size={16} style={{ color: '#818cf8' }} />
                <span style={{ fontSize: '0.85rem', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.05em', color: '#c7d2fe' }}>
                  Company Profile
                </span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                <Input
                  id="create-company-name"
                  name="companyName"
                  label="Company Name"
                  type="text"
                  placeholder="e.g. Acme Corporation or Nova Tech"
                  value={formData.companyName}
                  onChange={handleChange}
                  required
                  disabled={submitting}
                  autoFocus
                />

                <Input
                  id="create-company-phone"
                  name="phone"
                  label="Phone Number (Optional)"
                  type="tel"
                  placeholder="e.g. +1 (555) 019-2834"
                  value={formData.phone}
                  onChange={handleChange}
                  disabled={submitting}
                />
              </div>
            </div>

            {/* Section 2: Administrator Profile */}
            <div style={{
              padding: '1.25rem',
              backgroundColor: 'rgba(255, 255, 255, 0.03)',
              borderRadius: '10px',
              border: '1px solid rgba(255, 255, 255, 0.06)'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                <UserCheck size={16} style={{ color: '#818cf8' }} />
                <span style={{ fontSize: '0.85rem', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.05em', color: '#c7d2fe' }}>
                  Administrator Credentials
                </span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                <Input
                  id="create-admin-fullname"
                  name="fullName"
                  label="Administrator Full Name"
                  type="text"
                  placeholder="e.g. Sarah Connor"
                  value={formData.fullName}
                  onChange={handleChange}
                  required
                  disabled={submitting}
                />

                <Input
                  id="create-admin-email"
                  name="email"
                  label="Business Email"
                  type="email"
                  placeholder="sarah@acme.com"
                  value={formData.email}
                  onChange={handleChange}
                  required
                  disabled={submitting}
                />

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <Input
                    id="create-admin-password"
                    name="password"
                    label="Password (min 8 chars)"
                    type="password"
                    placeholder="••••••••••••"
                    value={formData.password}
                    onChange={handleChange}
                    required
                    disabled={submitting}
                  />

                  <Input
                    id="create-admin-confirmpassword"
                    name="confirmPassword"
                    label="Confirm Password"
                    type="password"
                    placeholder="••••••••••••"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    required
                    disabled={submitting}
                  />
                </div>
              </div>
            </div>

            <Button
              type="submit"
              variant="primary"
              disabled={submitting}
              style={{
                width: '100%',
                padding: '0.85rem',
                fontSize: '1rem',
                fontWeight: '700',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                background: 'linear-gradient(135deg, #6366f1 0%, #4338ca 100%)',
                boxShadow: '0 4px 16px rgba(99, 102, 241, 0.4)'
              }}
            >
              <Sparkles size={18} />
              {submitting ? 'Registering Company Workspace...' : 'Register Company Workspace'}
            </Button>
          </div>
        </form>

        <div style={{
          marginTop: '1.75rem',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '0.75rem',
          fontSize: '0.875rem'
        }}>
          <div>
            <span style={{ color: '#94a3b8' }}>Already have a company account? </span>
            <Link
              to="/login"
              style={{
                color: '#818cf8',
                fontWeight: '600',
                textDecoration: 'none'
              }}
            >
              Sign In here
            </Link>
          </div>

          <Link
            to="/"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              color: '#64748b',
              textDecoration: 'none',
              fontSize: '0.8rem'
            }}
          >
            <ArrowLeft size={14} />
            <span>Return to Landing Page</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
