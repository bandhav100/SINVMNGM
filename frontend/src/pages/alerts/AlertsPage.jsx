import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { alertApi } from '../../api/alertApi';
import { useAuth } from '../../context/AuthContext';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  Bell,
  AlertTriangle,
  ShieldAlert,
  Search,
  RefreshCw,
  ShoppingCart,
  Package,
  CheckCircle2,
  Filter,
  ArrowRight
} from 'lucide-react';

export default function AlertsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [alerts, setAlerts] = useState([]);
  const [summary, setSummary] = useState(null);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);

  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [severityFilter, setSeverityFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');

  const [isLoading, setIsLoading] = useState(true);
  const [errorBanner, setErrorBanner] = useState(null);

  const fetchAlerts = useCallback(async () => {
    setIsLoading(true);
    setErrorBanner(null);
    try {
      const params = {
        page,
        size: pageSize
      };
      if (appliedSearch) params.search = appliedSearch;
      if (severityFilter) params.severity = severityFilter;
      if (typeFilter) params.type = typeFilter;

      const [alertsRes, summaryRes] = await Promise.all([
        alertApi.getAlerts(params),
        alertApi.getSummary()
      ]);

      setAlerts(alertsRes.content || []);
      setTotalElements(alertsRes.totalElements || 0);
      setTotalPages(alertsRes.totalPages || 0);
      setSummary(summaryRes);
    } catch (err) {
      console.error('Failed to fetch alerts:', err);
      setErrorBanner(err.response?.data?.message || 'Failed to load stock alerts.');
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, appliedSearch, severityFilter, typeFilter]);

  useEffect(() => {
    fetchAlerts();
  }, [fetchAlerts]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setAppliedSearch(search.trim());
  };

  const handleClearFilters = () => {
    setSearch('');
    setAppliedSearch('');
    setSeverityFilter('');
    setTypeFilter('');
    setPage(0);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', paddingBottom: '3rem' }}>
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 700, margin: 0, color: 'var(--color-text-main)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Bell style={{ width: '2rem', height: '2rem', color: '#f59e0b' }} />
            Inventory Alerts & Notifications
          </h1>
          <p style={{ margin: '0.25rem 0 0 0', color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>
            Active monitoring for depleted stock, threshold breaches, and required replenishment actions
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <Button
            variant="outline"
            size="sm"
            onClick={fetchAlerts}
            disabled={isLoading}
            style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}
          >
            <RefreshCw style={{ width: '0.9rem', height: '0.9rem', animation: isLoading ? 'spin 1s linear infinite' : 'none' }} />
            Refresh
          </Button>

          <Button
            variant="primary"
            size="sm"
            onClick={() => navigate('/purchases')}
            style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}
          >
            <ShoppingCart style={{ width: '0.9rem', height: '0.9rem' }} />
            Create Purchase Order
          </Button>
        </div>
      </div>

      {errorBanner && (
        <AlertBanner
          type="error"
          message={errorBanner}
          onClose={() => setErrorBanner(null)}
        />
      )}

      {/* Summary KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
        <div style={{ background: 'var(--color-bg-card)', border: '1px solid var(--color-border)', borderRadius: '0.75rem', padding: '1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>
            <span>Total Active Alerts</span>
            <Bell style={{ width: '1.25rem', height: '1.25rem', color: '#6366f1' }} />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.5rem 0 0 0', color: 'var(--color-text-main)' }}>
            {summary ? Number(summary.totalAlerts).toLocaleString() : '—'}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>Items requiring inventory attention</span>
        </div>

        <div
          onClick={() => { setSeverityFilter('CRITICAL'); setTypeFilter(''); setPage(0); }}
          style={{
            background: 'var(--color-bg-card)',
            border: summary?.criticalCount > 0 ? '1px solid #ef4444' : '1px solid var(--color-border)',
            borderRadius: '0.75rem',
            padding: '1.25rem',
            cursor: 'pointer'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: '#ef4444', fontSize: '0.85rem' }}>
            <span>Critical (Out of Stock)</span>
            <AlertTriangle style={{ width: '1.25rem', height: '1.25rem' }} />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.5rem 0 0 0', color: '#ef4444' }}>
            {summary ? Number(summary.criticalCount).toLocaleString() : '—'}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>0 units remaining in stock</span>
        </div>

        <div
          onClick={() => { setSeverityFilter('WARNING'); setTypeFilter(''); setPage(0); }}
          style={{
            background: 'var(--color-bg-card)',
            border: summary?.warningCount > 0 ? '1px solid #f59e0b' : '1px solid var(--color-border)',
            borderRadius: '0.75rem',
            padding: '1.25rem',
            cursor: 'pointer'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: '#f59e0b', fontSize: '0.85rem' }}>
            <span>Warning (Low Stock)</span>
            <ShieldAlert style={{ width: '1.25rem', height: '1.25rem' }} />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.5rem 0 0 0', color: '#f59e0b' }}>
            {summary ? Number(summary.warningCount).toLocaleString() : '—'}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>Stock breached reorder threshold</span>
        </div>
      </div>

      {/* Filters and Search Bar */}
      <div
        style={{
          background: 'var(--color-bg-card)',
          border: '1px solid var(--color-border)',
          borderRadius: '0.75rem',
          padding: '1rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem'
        }}
      >
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '0.5rem', flex: '1 1 300px' }}>
          <div style={{ position: 'relative', width: '100%' }}>
            <Search style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', width: '1rem', height: '1rem', color: 'var(--color-text-muted)' }} />
            <input
              type="text"
              placeholder="Search by product name or SKU..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{
                width: '100%',
                padding: '0.5rem 0.75rem 0.5rem 2.25rem',
                borderRadius: '0.5rem',
                border: '1px solid var(--color-border)',
                background: 'var(--color-bg-input)',
                color: 'var(--color-text-main)',
                fontSize: '0.875rem'
              }}
            />
          </div>
          <Button type="submit" variant="secondary" size="sm">
            Search
          </Button>
        </form>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
            <Filter style={{ width: '0.9rem', height: '0.9rem' }} />
            Severity:
          </div>
          <select
            value={severityFilter}
            onChange={(e) => { setSeverityFilter(e.target.value); setPage(0); }}
            style={{
              padding: '0.45rem 0.75rem',
              borderRadius: '0.5rem',
              border: '1px solid var(--color-border)',
              background: 'var(--color-bg-input)',
              color: 'var(--color-text-main)',
              fontSize: '0.85rem'
            }}
          >
            <option value="">All Severities</option>
            <option value="CRITICAL">Critical Only</option>
            <option value="WARNING">Warning Only</option>
          </select>

          <select
            value={typeFilter}
            onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}
            style={{
              padding: '0.45rem 0.75rem',
              borderRadius: '0.5rem',
              border: '1px solid var(--color-border)',
              background: 'var(--color-bg-input)',
              color: 'var(--color-text-main)',
              fontSize: '0.85rem'
            }}
          >
            <option value="">All Types</option>
            <option value="OUT_OF_STOCK">Out of Stock</option>
            <option value="LOW_STOCK">Low Stock</option>
          </select>

          {(appliedSearch || severityFilter || typeFilter) && (
            <Button variant="ghost" size="sm" onClick={handleClearFilters} style={{ fontSize: '0.8rem', color: '#ef4444' }}>
              Clear Filters
            </Button>
          )}
        </div>
      </div>

      {/* Alerts Table / Feed */}
      <div style={{ background: 'var(--color-bg-card)', border: '1px solid var(--color-border)', borderRadius: '0.75rem', overflow: 'hidden' }}>
        {isLoading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}>
            <LoadingSpinner size="lg" />
          </div>
        ) : alerts.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '4rem 1.5rem', color: 'var(--color-text-muted)' }}>
            <CheckCircle2 style={{ width: '3rem', height: '3rem', color: '#10b981', margin: '0 auto 1rem auto' }} />
            <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 600, color: 'var(--color-text-main)' }}>
              All Inventory Levels Optimal
            </h3>
            <p style={{ margin: '0.5rem 0 0 0', fontSize: '0.9rem' }}>
              No items currently require replenishment under the selected filter criteria.
            </p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ background: 'rgba(0, 0, 0, 0.02)', borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-muted)', textAlign: 'left' }}>
                  <th style={{ padding: '0.75rem 1rem' }}>Severity</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Product</th>
                  <th style={{ padding: '0.75rem 1rem' }}>SKU</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Current Stock</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Threshold</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Suggested Reorder</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Message</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'center' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((alert) => (
                  <tr key={alert.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                    <td style={{ padding: '0.85rem 1rem' }}>
                      {alert.severity === 'CRITICAL' ? (
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', padding: '0.25rem 0.6rem', borderRadius: '1rem', fontSize: '0.75rem', fontWeight: 700 }}>
                          <AlertTriangle style={{ width: '0.8rem', height: '0.8rem' }} />
                          CRITICAL
                        </span>
                      ) : (
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', background: 'rgba(245, 158, 11, 0.1)', color: '#f59e0b', padding: '0.25rem 0.6rem', borderRadius: '1rem', fontSize: '0.75rem', fontWeight: 700 }}>
                          <ShieldAlert style={{ width: '0.8rem', height: '0.8rem' }} />
                          WARNING
                        </span>
                      )}
                    </td>

                    <td style={{ padding: '0.85rem 1rem', fontWeight: 600, color: 'var(--color-text-main)' }}>
                      {alert.productName}
                      {alert.categoryName && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>{alert.categoryName}</div>
                      )}
                    </td>

                    <td style={{ padding: '0.85rem 1rem', fontFamily: 'monospace', color: 'var(--color-text-muted)' }}>
                      {alert.sku}
                    </td>

                    <td style={{ padding: '0.85rem 1rem', textAlign: 'right', fontWeight: 700, color: alert.currentStock === 0 ? '#ef4444' : '#f59e0b' }}>
                      {alert.currentStock}
                    </td>

                    <td style={{ padding: '0.85rem 1rem', textAlign: 'right', color: 'var(--color-text-muted)' }}>
                      {alert.reorderLevel}
                    </td>

                    <td style={{ padding: '0.85rem 1rem', textAlign: 'right', fontWeight: 700, color: '#4f46e5' }}>
                      +{alert.suggestedReplenishmentQuantity} units
                    </td>

                    <td style={{ padding: '0.85rem 1rem', color: 'var(--color-text-muted)', fontSize: '0.8rem', maxWidth: '300px' }}>
                      {alert.message}
                    </td>

                    <td style={{ padding: '0.85rem 1rem', textAlign: 'center' }}>
                      <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => navigate('/purchases')}
                          style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', padding: '0.3rem 0.6rem' }}
                        >
                          <ShoppingCart style={{ width: '0.75rem', height: '0.75rem' }} />
                          Reorder
                        </Button>

                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => navigate('/products')}
                          style={{ fontSize: '0.75rem', padding: '0.3rem 0.6rem' }}
                        >
                          View
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', borderTop: '1px solid var(--color-border)', fontSize: '0.85rem' }}>
            <span style={{ color: 'var(--color-text-muted)' }}>
              Showing {page * pageSize + 1} to {Math.min((page + 1) * pageSize, totalElements)} of {totalElements} alerts
            </span>

            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
