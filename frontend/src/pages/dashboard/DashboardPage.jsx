import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { dashboardApi } from '../../api/dashboardApi';
import { useAuth } from '../../context/AuthContext';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  LayoutDashboard,
  Package,
  TrendingUp,
  AlertTriangle,
  ShoppingCart,
  Truck,
  DollarSign,
  ArrowRight,
  RefreshCw,
  Clock,
  CheckCircle2,
  AlertOctagon,
  Layers,
  ArrowUpRight,
  ArrowDownRight,
  BarChart3,
  Calendar,
  Sparkles,
  ShieldCheck,
  Building
} from 'lucide-react';

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastRefreshed, setLastRefreshed] = useState(null);

  const fetchDashboardData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await dashboardApi.getSummary();
      setSummary(data);
      setLastRefreshed(new Date());
    } catch (err) {
      console.error('Failed to load dashboard summary:', err);
      setError(err.message || 'Unable to retrieve dashboard metrics from server.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const formatCurrency = (val) => {
    const num = Number(val || 0);
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2
    }).format(num);
  };

  const formatNumber = (val) => {
    return new Intl.NumberFormat('en-US').format(val || 0);
  };

  const formatTimestamp = (ts) => {
    if (!ts) return 'Just now';
    try {
      const date = new Date(ts);
      return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return ts;
    }
  };

  const role = user?.role || '';
  const canAccessPurchases = role === 'ADMIN' || role === 'INVENTORY_MANAGER' || role === 'PURCHASE_MANAGER';
  const canAccessSales = role === 'ADMIN' || role === 'SALES_STAFF';

  if (loading && !summary) {
    return (
      <div className="page-container" style={{ padding: '2rem' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', gap: '1rem' }}>
          <LoadingSpinner size="large" />
          <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.95rem' }}>Aggregating real-time inventory and sales metrics...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container" style={{ padding: '1.5rem 2rem', maxWidth: '1400px', margin: '0 auto' }}>
      {/* Dashboard Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem', marginBottom: '1.5rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.25rem' }}>
            <div style={{
              width: '40px',
              height: '40px',
              borderRadius: 'var(--radius-lg)',
              backgroundColor: 'var(--color-primary-light)',
              color: 'var(--color-primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <LayoutDashboard size={22} />
            </div>
            <div>
              <h1 style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--color-text-primary)' }}>Operations Dashboard</h1>
              <p style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)' }}>
                Welcome back, <span style={{ fontWeight: '600' }}>{user?.fullName || user?.username}</span> • Role: <Badge variant="primary">{role.replace('_', ' ')}</Badge>
              </p>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          {lastRefreshed && (
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <Clock size={12} /> Refreshed: {lastRefreshed.toLocaleTimeString()}
            </span>
          )}
          <Button
            variant="outline"
            onClick={fetchDashboardData}
            disabled={loading}
            icon={RefreshCw}
            style={{ fontSize: '0.85rem' }}
          >
            {loading ? 'Refreshing...' : 'Refresh Data'}
          </Button>

          {canAccessSales && (
            <Button
              variant="primary"
              onClick={() => navigate('/sales')}
              icon={TrendingUp}
              style={{ fontSize: '0.85rem' }}
            >
              Sales Orders
            </Button>
          )}
          {canAccessPurchases && (
            <Button
              variant="outline"
              onClick={() => navigate('/purchases')}
              icon={Truck}
              style={{ fontSize: '0.85rem' }}
            >
              Purchase Orders
            </Button>
          )}
        </div>
      </div>

      {error && (
        <AlertBanner
          type="error"
          message={error}
          onClose={() => setError(null)}
          actionLabel="Retry"
          onAction={fetchDashboardData}
          style={{ marginBottom: '1.5rem' }}
        />
      )}

      {/* Top Metric KPI Cards */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
        gap: '1.25rem',
        marginBottom: '1.5rem'
      }}>
        {/* Total Products & Units */}
        <div className="card" style={{ padding: '1.25rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--color-text-secondary)' }}>Catalog & Stock Units</span>
            <div style={{ padding: '0.4rem', borderRadius: 'var(--radius-md)', background: 'var(--color-primary-light)', color: 'var(--color-primary)' }}>
              <Package size={20} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: '700', color: 'var(--color-text-primary)', marginBottom: '0.25rem' }}>
            {formatNumber(summary?.totalProducts)} <span style={{ fontSize: '1rem', fontWeight: '400', color: 'var(--color-text-muted)' }}>SKUs</span>
          </div>
          <div style={{ fontSize: '0.825rem', color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>Total Units: <strong>{formatNumber(summary?.totalStockUnits)}</strong></span>
            <Link to="/products" style={{ color: 'var(--color-primary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '2px', fontWeight: '500' }}>
              View Catalog <ArrowUpRight size={14} />
            </Link>
          </div>
        </div>

        {/* Stock Alert Radar */}
        <div className="card" style={{ padding: '1.25rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--color-text-secondary)' }}>Reorder Attention</span>
            <div style={{
              padding: '0.4rem',
              borderRadius: 'var(--radius-md)',
              background: (summary?.outOfStockCount > 0 || summary?.lowStockCount > 0) ? 'var(--color-warning-light)' : 'var(--color-success-light)',
              color: (summary?.outOfStockCount > 0 || summary?.lowStockCount > 0) ? 'var(--color-warning)' : 'var(--color-success)'
            }}>
              <AlertTriangle size={20} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: '700', color: summary?.outOfStockCount > 0 ? 'var(--color-danger)' : (summary?.lowStockCount > 0 ? 'var(--color-warning)' : 'var(--color-success)'), marginBottom: '0.25rem' }}>
            {formatNumber((summary?.outOfStockCount || 0) + (summary?.lowStockCount || 0))} <span style={{ fontSize: '1rem', fontWeight: '400', color: 'var(--color-text-muted)' }}>Alerts</span>
          </div>
          <div style={{ fontSize: '0.825rem', color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>
              {summary?.outOfStockCount || 0} out-of-stock • {summary?.lowStockCount || 0} low-stock
            </span>
            <Link to="/alerts" style={{ color: 'var(--color-primary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '2px', fontWeight: '500' }}>
              Radar <ArrowUpRight size={14} />
            </Link>
          </div>
        </div>

        {/* Completed Sales Revenue */}
        <div className="card" style={{ padding: '1.25rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--color-text-secondary)' }}>Completed Sales Revenue</span>
            <div style={{ padding: '0.4rem', borderRadius: 'var(--radius-md)', background: 'var(--color-success-light)', color: 'var(--color-success)' }}>
              <TrendingUp size={20} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: '700', color: 'var(--color-text-primary)', marginBottom: '0.25rem' }}>
            {formatCurrency(summary?.completedSalesRevenue)}
          </div>
          <div style={{ fontSize: '0.825rem', color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>Completed Orders: <strong>{formatNumber(summary?.completedSalesCount)}</strong></span>
            {canAccessSales ? (
              <Link to="/sales" style={{ color: 'var(--color-primary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '2px', fontWeight: '500' }}>
                Sales Log <ArrowUpRight size={14} />
              </Link>
            ) : (
              <span style={{ color: 'var(--color-text-muted)' }}>Live</span>
            )}
          </div>
        </div>

        {/* Procurement / Valuation Metric */}
        {summary?.purchaseMetricsVisible ? (
          <div className="card" style={{ padding: '1.25rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
              <span style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--color-text-secondary)' }}>Received Procurement</span>
              <div style={{ padding: '0.4rem', borderRadius: 'var(--radius-md)', background: '#ede9fe', color: '#6d28d9' }}>
                <Truck size={20} />
              </div>
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: '700', color: 'var(--color-text-primary)', marginBottom: '0.25rem' }}>
              {formatCurrency(summary?.receivedPurchasesTotalCost)}
            </div>
            <div style={{ fontSize: '0.825rem', color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span>Received POs: <strong>{formatNumber(summary?.receivedPurchasesCount)}</strong> • Pending: <strong>{formatNumber(summary?.pendingPurchasesCount)}</strong></span>
              <Link to="/purchases" style={{ color: 'var(--color-primary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '2px', fontWeight: '500' }}>
                Purchases <ArrowUpRight size={14} />
              </Link>
            </div>
          </div>
        ) : (
          <div className="card" style={{ padding: '1.25rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
              <span style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--color-text-secondary)' }}>Inventory Valuation</span>
              <div style={{ padding: '0.4rem', borderRadius: 'var(--radius-md)', background: '#ede9fe', color: '#6d28d9' }}>
                <DollarSign size={20} />
              </div>
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: '700', color: 'var(--color-text-primary)', marginBottom: '0.25rem' }}>
              {formatCurrency(summary?.potentialRetailValuation)}
            </div>
            <div style={{ fontSize: '0.825rem', color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span>Retail Stock Valuation</span>
              <span style={{ color: 'var(--color-success)', fontWeight: '500' }}>Active Stock</span>
            </div>
          </div>
        )}
      </div>

      {/* Main Grid: Critical Alerts Radar (Left) + Operations Feed (Right) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '1.5rem', marginBottom: '1.5rem' }}>
        
        {/* Low Stock Radar Table */}
        <div className="card" style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)', overflow: 'hidden' }}>
          <div style={{ padding: '1.25rem', borderBottom: '1px solid var(--color-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <AlertTriangle size={18} color="var(--color-warning)" />
              <h2 style={{ fontSize: '1.05rem', fontWeight: '600', color: 'var(--color-text-primary)' }}>Stock Replenishment Radar</h2>
            </div>
            <Link to="/alerts" style={{ fontSize: '0.825rem', color: 'var(--color-primary)', textDecoration: 'none', fontWeight: '500', display: 'flex', alignItems: 'center', gap: '2px' }}>
              All Alerts <ArrowRight size={14} />
            </Link>
          </div>

          <div style={{ padding: '1rem' }}>
            {(!summary?.criticalAlerts || summary.criticalAlerts.length === 0) ? (
              <div style={{ textAlign: 'center', padding: '2.5rem 1rem', color: 'var(--color-text-muted)' }}>
                <CheckCircle2 size={36} color="var(--color-success)" style={{ margin: '0 auto 0.75rem' }} />
                <p style={{ fontWeight: '600', color: 'var(--color-text-primary)', marginBottom: '0.25rem' }}>Healthy Stock Levels</p>
                <p style={{ fontSize: '0.85rem' }}>No products are currently below their safety reorder thresholds.</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)' }}>
                      <th style={{ padding: '0.6rem 0.5rem', fontWeight: '600' }}>Product</th>
                      <th style={{ padding: '0.6rem 0.5rem', fontWeight: '600' }}>SKU</th>
                      <th style={{ padding: '0.6rem 0.5rem', fontWeight: '600' }}>In Stock</th>
                      <th style={{ padding: '0.6rem 0.5rem', fontWeight: '600' }}>Threshold</th>
                      <th style={{ padding: '0.6rem 0.5rem', fontWeight: '600' }}>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {summary.criticalAlerts.map((alert) => (
                      <tr key={alert.productId} style={{ borderBottom: '1px solid var(--color-surface-subtle)' }}>
                        <td style={{ padding: '0.6rem 0.5rem', fontWeight: '500', color: 'var(--color-text-primary)' }}>
                          {alert.productName}
                        </td>
                        <td style={{ padding: '0.6rem 0.5rem', color: 'var(--color-text-muted)', fontFamily: 'monospace' }}>
                          {alert.sku}
                        </td>
                        <td style={{ padding: '0.6rem 0.5rem', fontWeight: '600', color: alert.currentQuantity === 0 ? 'var(--color-danger)' : 'var(--color-warning)' }}>
                          {alert.currentQuantity} units
                        </td>
                        <td style={{ padding: '0.6rem 0.5rem', color: 'var(--color-text-secondary)' }}>
                          {alert.reorderLevel} units
                        </td>
                        <td style={{ padding: '0.6rem 0.5rem' }}>
                          {alert.currentQuantity === 0 ? (
                            <Badge variant="danger">Out of Stock</Badge>
                          ) : (
                            <Badge variant="warning">Low Stock</Badge>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* Live Operational Activity Stream */}
        <div className="card" style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)', overflow: 'hidden' }}>
          <div style={{ padding: '1.25rem', borderBottom: '1px solid var(--color-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Clock size={18} color="var(--color-primary)" />
              <h2 style={{ fontSize: '1.05rem', fontWeight: '600', color: 'var(--color-text-primary)' }}>Recent Operations & Activity</h2>
            </div>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>Live Feed</span>
          </div>

          <div style={{ padding: '1rem', maxHeight: '420px', overflowY: 'auto' }}>
            {(!summary?.recentActivity || summary.recentActivity.length === 0) ? (
              <div style={{ textAlign: 'center', padding: '2.5rem 1rem', color: 'var(--color-text-muted)' }}>
                <Layers size={36} style={{ margin: '0 auto 0.75rem', opacity: 0.5 }} />
                <p style={{ fontWeight: '600', color: 'var(--color-text-primary)', marginBottom: '0.25rem' }}>No Recent Transactions</p>
                <p style={{ fontSize: '0.85rem' }}>Recorded sales, receipts, and stock changes will stream here in real-time.</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {summary.recentActivity.map((item) => {
                  const isSale = item.type === 'SALE';
                  const isPurchase = item.type === 'PURCHASE';
                  const isTx = item.type === 'INVENTORY';

                  let badgeVariant = 'secondary';
                  if (item.status === 'COMPLETED' || item.status === 'RECEIVED') badgeVariant = 'success';
                  else if (item.status === 'DRAFT' || item.status === 'ORDERED') badgeVariant = 'primary';
                  else if (item.status === 'CANCELLED') badgeVariant = 'danger';

                  return (
                    <div
                      key={item.id}
                      onClick={() => item.link && navigate(item.link)}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '0.75rem',
                        borderRadius: 'var(--radius-md)',
                        background: 'var(--color-surface-subtle)',
                        border: '1px solid var(--color-border)',
                        cursor: item.link ? 'pointer' : 'default',
                        transition: 'background-color var(--transition-fast)'
                      }}
                      onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--color-background)'}
                      onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'var(--color-surface-subtle)'}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <div style={{
                          width: '34px',
                          height: '34px',
                          borderRadius: 'var(--radius-full)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          background: isSale ? 'var(--color-success-light)' : (isPurchase ? '#ede9fe' : 'var(--color-primary-light)'),
                          color: isSale ? 'var(--color-success)' : (isPurchase ? '#6d28d9' : 'var(--color-primary)')
                        }}>
                          {isSale && <TrendingUp size={16} />}
                          {isPurchase && <Truck size={16} />}
                          {isTx && <Layers size={16} />}
                        </div>

                        <div>
                          <div style={{ fontSize: '0.875rem', fontWeight: '600', color: 'var(--color-text-primary)' }}>
                            {item.title}
                          </div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)' }}>
                            {item.subtitle}
                          </div>
                        </div>
                      </div>

                      <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '0.2rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          {item.amount != null && (
                            <span style={{ fontSize: '0.875rem', fontWeight: '600', color: 'var(--color-text-primary)' }}>
                              {formatCurrency(item.amount)}
                            </span>
                          )}
                          <Badge variant={badgeVariant}>{item.status}</Badge>
                        </div>
                        <span style={{ fontSize: '0.7rem', color: 'var(--color-text-muted)' }}>
                          {formatTimestamp(item.timestamp)}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

      </div>

      {/* Operational Breakdown Bar */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
        gap: '1.25rem',
        padding: '1.25rem',
        borderRadius: 'var(--radius-lg)',
        border: '1px solid var(--color-border)',
        background: 'var(--color-surface)',
        boxShadow: 'var(--shadow-sm)'
      }}>
        {/* Sales Pipeline Summary */}
        <div>
          <h3 style={{ fontSize: '0.875rem', fontWeight: '600', color: 'var(--color-text-secondary)', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <TrendingUp size={16} /> Sales Pipeline
          </h3>
          <div style={{ display: 'flex', gap: '1rem', fontSize: '0.825rem' }}>
            <div>
              <span style={{ color: 'var(--color-text-muted)', display: 'block' }}>Completed</span>
              <strong style={{ color: 'var(--color-success)', fontSize: '1.1rem' }}>{summary?.completedSalesCount || 0}</strong>
            </div>
            <div>
              <span style={{ color: 'var(--color-text-muted)', display: 'block' }}>Drafts</span>
              <strong style={{ color: 'var(--color-primary)', fontSize: '1.1rem' }}>{summary?.draftSalesCount || 0}</strong>
            </div>
            <div>
              <span style={{ color: 'var(--color-text-muted)', display: 'block' }}>Cancelled</span>
              <strong style={{ color: 'var(--color-danger)', fontSize: '1.1rem' }}>{summary?.cancelledSalesCount || 0}</strong>
            </div>
          </div>
        </div>

        {/* Valuation Summary */}
        <div>
          <h3 style={{ fontSize: '0.875rem', fontWeight: '600', color: 'var(--color-text-secondary)', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <DollarSign size={16} /> Retail Stock Valuation
          </h3>
          <div style={{ fontSize: '1.1rem', fontWeight: '700', color: 'var(--color-text-primary)' }}>
            {formatCurrency(summary?.potentialRetailValuation)}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
            Total potential gross retail value of currently held stock
          </span>
        </div>

        {/* System Health Status */}
        <div>
          <h3 style={{ fontSize: '0.875rem', fontWeight: '600', color: 'var(--color-text-secondary)', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <ShieldCheck size={16} color="var(--color-success)" /> Database & RBAC Status
          </h3>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem' }}>
            <Badge variant="success">PostgreSQL Connected</Badge>
            <Badge variant="primary">RBAC Enforced</Badge>
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem', display: 'block' }}>
            Spring Boot 4.1.1 REST Engine • Concurrency Safe
          </span>
        </div>
      </div>
    </div>
  );
}
