import React from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../components/common/Button';
import { Package, ArrowLeft, Layers, ShieldCheck, Sparkles } from 'lucide-react';

export default function ModulePlaceholderPage({
  title,
  icon: Icon = Layers,
  phase,
  description,
  features = []
}) {
  const navigate = useNavigate();

  return (
    <div style={{ maxWidth: '800px', margin: '2rem auto' }}>
      <div className="card" style={{ padding: '2.5rem', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1.25rem' }}>
        <div style={{ width: '64px', height: '64px', borderRadius: 'var(--radius-xl)', backgroundColor: 'var(--color-primary-light)', color: 'var(--color-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Icon size={32} />
        </div>

        <div>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem', padding: '0.25rem 0.75rem', borderRadius: 'var(--radius-full)', backgroundColor: 'var(--color-surface-subtle)', color: 'var(--color-primary)', fontSize: '0.75rem', fontWeight: '600', marginBottom: '0.5rem', border: '1px solid var(--color-border)' }}>
            <Sparkles size={12} />
            <span>Planned in {phase}</span>
          </div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: '700', color: 'var(--color-text-primary)' }}>
            {title}
          </h2>
          <p style={{ color: 'var(--color-text-secondary)', maxWidth: '540px', margin: '0.5rem auto 0 auto', fontSize: '0.9375rem' }}>
            {description}
          </p>
        </div>

        {features.length > 0 && (
          <div style={{ width: '100%', maxWidth: '520px', textAlign: 'left', backgroundColor: 'var(--color-surface-subtle)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', padding: '1.25rem', marginTop: '0.5rem' }}>
            <span style={{ fontSize: '0.8125rem', fontWeight: '600', color: 'var(--color-text-secondary)', display: 'block', marginBottom: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Upcoming Module Capabilities
            </span>
            <ul style={{ listStyleType: 'none', display: 'flex', flexDirection: 'column', gap: '0.5rem', padding: 0, margin: 0 }}>
              {features.map((feature, idx) => (
                <li key={idx} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem', color: 'var(--color-text-primary)' }}>
                  <ShieldCheck size={16} color="var(--color-success)" />
                  <span>{feature}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
          <Button variant="secondary" icon={ArrowLeft} onClick={() => navigate('/products')}>
            Back to Products
          </Button>
          <Button variant="primary" icon={Package} onClick={() => navigate('/products')}>
            Manage Products
          </Button>
        </div>
      </div>
    </div>
  );
}
