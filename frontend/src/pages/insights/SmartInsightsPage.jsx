import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { insightApi } from '../../api/insightApi';
import { useAuth } from '../../context/AuthContext';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  Brain,
  TrendingUp,
  TrendingDown,
  RefreshCw,
  Calendar,
  AlertTriangle,
  Package,
  Layers,
  DollarSign,
  ShoppingCart,
  ArrowRight,
  Info,
  CheckCircle,
  Clock,
  ShieldAlert
} from 'lucide-react';

export default function SmartInsightsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const userRole = user?.role;

  const canViewFinancials = userRole === 'ADMIN' || userRole === 'INVENTORY_MANAGER' || userRole === 'PURCHASE_MANAGER';
  const canViewReorderRecs = userRole === 'ADMIN' || userRole === 'INVENTORY_MANAGER' || userRole === 'PURCHASE_MANAGER';

  // State
  const [selectedDays, setSelectedDays] = useState(30);
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');
  const [isCustomDate, setIsCustomDate] = useState(false);

  const [overview, setOverview] = useState(null);
  const [fastMoving, setFastMoving] = useState([]);
  const [slowMoving, setSlowMoving] = useState([]);
  const [purchaseVsSales, setPurchaseVsSales] = useState(null);
  const [reorderRecs, setReorderRecs] = useState([]);
  const [stockValuation, setStockValuation] = useState(null);

  const [activeTab, setActiveTab] = useState('velocity'); // 'velocity', 'reorder', 'valuation'
  const [isLoading, setIsLoading] = useState(true);
  const [errorBanner, setErrorBanner] = useState(null);

  // Load data
  const loadData = useCallback(async () => {
    setIsLoading(true);
    setErrorBanner(null);

    const queryParams = isCustomDate && customStartDate && customEndDate
      ? { startDate: customStartDate, endDate: customEndDate }
      : { days: selectedDays };

    try {
      const promises = [
        insightApi.getOverview(),
        insightApi.getFastMovingProducts({ ...queryParams, limit: 10 }),
        insightApi.getSlowMovingProducts({ ...queryParams, limit: 10 }),
        insightApi.getPurchaseVsSales(queryParams)
      ];

      if (canViewReorderRecs) {
        promises.push(insightApi.getReorderRecommendations());
      }

      if (canViewFinancials) {
        promises.push(insightApi.getStockValuation({ page: 0, size: 10 }));
      }

      const results = await Promise.all(promises);

      setOverview(results[0]);
      setFastMoving(results[1]);
      setSlowMoving(results[2]);
      setPurchaseVsSales(results[3]);

      let idx = 4;
      if (canViewReorderRecs) {
        setReorderRecs(results[idx++]);
      }
      if (canViewFinancials) {
        setStockValuation(results[idx]);
      }
    } catch (err) {
      console.error('Failed to load insights data:', err);
      setErrorBanner(err.response?.data?.message || 'Failed to load smart inventory analytics.');
    } finally {
      setIsLoading(false);
    }
  }, [selectedDays, isCustomDate, customStartDate, customEndDate, canViewFinancials, canViewReorderRecs]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handlePeriodChange = (days) => {
    setIsCustomDate(false);
    setSelectedDays(days);
  };

  const handleApplyCustomDates = (e) => {
    e.preventDefault();
    if (!customStartDate || !customEndDate) return;
    if (customStartDate > customEndDate) {
      setErrorBanner('Start date cannot be after end date.');
      return;
    }
    setIsCustomDate(true);
    loadData();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', paddingBottom: '3rem' }}>
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 700, margin: 0, color: 'var(--color-text-main)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Brain style={{ width: '2rem', height: '2rem', color: '#6366f1' }} />
            Smart Inventory Insights
          </h1>
          <p style={{ margin: '0.25rem 0 0 0', color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>
            Velocity intelligence, demand analytics, replenishment recommendations, and stockout warnings
          </p>
        </div>

        {/* Time Window Selector & Refresh */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', background: 'var(--color-bg-card)', padding: '0.25rem', borderRadius: '0.5rem', border: '1px solid var(--color-border)' }}>
            {[7, 30, 90].map((d) => (
              <button
                key={d}
                type="button"
                onClick={() => handlePeriodChange(d)}
                style={{
                  padding: '0.35rem 0.75rem',
                  fontSize: '0.85rem',
                  fontWeight: selectedDays === d && !isCustomDate ? 600 : 400,
                  borderRadius: '0.375rem',
                  border: 'none',
                  background: selectedDays === d && !isCustomDate ? '#4f46e5' : 'transparent',
                  color: selectedDays === d && !isCustomDate ? '#ffffff' : 'var(--color-text-muted)',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease'
                }}
              >
                {d} Days
              </button>
            ))}
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            disabled={isLoading}
            style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}
          >
            <RefreshCw style={{ width: '0.9rem', height: '0.9rem', animation: isLoading ? 'spin 1s linear infinite' : 'none' }} />
            Refresh
          </Button>
        </div>
      </div>

      {/* Date Filter Bar */}
      <form
        onSubmit={handleApplyCustomDates}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          flexWrap: 'wrap',
          background: 'var(--color-bg-card)',
          padding: '0.75rem 1rem',
          borderRadius: '0.5rem',
          border: '1px solid var(--color-border)',
          fontSize: '0.85rem'
        }}
      >
        <span style={{ color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.35rem', fontWeight: 500 }}>
          <Calendar style={{ width: '0.95rem', height: '0.95rem' }} />
          Custom Range:
        </span>
        <input
          type="date"
          value={customStartDate}
          onChange={(e) => setCustomStartDate(e.target.value)}
          style={{
            padding: '0.35rem 0.5rem',
            borderRadius: '0.375rem',
            border: '1px solid var(--color-border)',
            background: 'var(--color-bg-input)',
            color: 'var(--color-text-main)',
            fontSize: '0.85rem'
          }}
        />
        <span style={{ color: 'var(--color-text-muted)' }}>to</span>
        <input
          type="date"
          value={customEndDate}
          onChange={(e) => setCustomEndDate(e.target.value)}
          style={{
            padding: '0.35rem 0.5rem',
            borderRadius: '0.375rem',
            border: '1px solid var(--color-border)',
            background: 'var(--color-bg-input)',
            color: 'var(--color-text-main)',
            fontSize: '0.85rem'
          }}
        />
        <Button type="submit" variant="secondary" size="sm">
          Apply
        </Button>
        {isCustomDate && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => handlePeriodChange(30)}
            style={{ fontSize: '0.8rem', color: '#ef4444' }}
          >
            Reset to 30 Days
          </Button>
        )}
      </form>

      {errorBanner && (
        <AlertBanner
          type="error"
          message={errorBanner}
          onClose={() => setErrorBanner(null)}
        />
      )}

      {/* KPI Overview Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
        <div style={{ background: 'var(--color-bg-card)', border: '1px solid var(--color-border)', borderRadius: '0.75rem', padding: '1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>
            <span>Active Products</span>
            <Package style={{ width: '1.25rem', height: '1.25rem', color: '#6366f1' }} />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.5rem 0 0 0', color: 'var(--color-text-main)' }}>
            {overview ? Number(overview.totalProducts).toLocaleString() : '—'}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>In product catalog</span>
        </div>

        <div style={{ background: 'var(--color-bg-card)', border: '1px solid var(--color-border)', borderRadius: '0.75rem', padding: '1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>
            <span>Units in Stock</span>
            <Layers style={{ width: '1.25rem', height: '1.25rem', color: '#10b981' }} />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.5rem 0 0 0', color: 'var(--color-text-main)' }}>
            {overview ? Number(overview.totalStockUnits).toLocaleString() : '—'}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>Total available units</span>
        </div>

        <div
          onClick={() => navigate('/alerts')}
          style={{
            background: 'var(--color-bg-card)',
            border: overview?.outOfStockCount > 0 ? '1px solid #ef4444' : '1px solid var(--color-border)',
            borderRadius: '0.75rem',
            padding: '1.25rem',
            cursor: 'pointer',
            transition: 'transform 0.15s ease'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: '#ef4444', fontSize: '0.85rem' }}>
            <span>Out of Stock</span>
            <AlertTriangle style={{ width: '1.25rem', height: '1.25rem' }} />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.5rem 0 0 0', color: '#ef4444' }}>
            {overview ? Number(overview.outOfStockCount).toLocaleString() : '—'}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>0 units remaining (Click to view)</span>
        </div>

        <div
          onClick={() => navigate('/alerts')}
          style={{
            background: 'var(--color-bg-card)',
            border: overview?.lowStockCount > 0 ? '1px solid #f59e0b' : '1px solid var(--color-border)',
            borderRadius: '0.75rem',
            padding: '1.25rem',
            cursor: 'pointer',
            transition: 'transform 0.15s ease'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: '#f59e0b', fontSize: '0.85rem' }}>
            <span>Low Stock</span>
            <ShieldAlert style={{ width: '1.25rem', height: '1.25rem' }} />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.5rem 0 0 0', color: '#f59e0b' }}>
            {overview ? Number(overview.lowStockCount).toLocaleString() : '—'}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>At or below reorder level</span>
        </div>

        <div style={{ background: 'var(--color-bg-card)', border: '1px solid var(--color-border)', borderRadius: '0.75rem', padding: '1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>
            <span>Retail Valuation</span>
            <DollarSign style={{ width: '1.25rem', height: '1.25rem', color: '#3b82f6' }} />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.5rem 0 0 0', color: '#3b82f6' }}>
            ${overview && overview.totalPotentialRetailValue != null ? Number(overview.totalPotentialRetailValue).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00'}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>Based on catalog selling price</span>
        </div>
      </div>

      {/* Purchase vs Sales Comparison Banner */}
      {purchaseVsSales && (
        <div
          style={{
            background: 'var(--color-bg-card)',
            border: '1px solid var(--color-border)',
            borderRadius: '0.75rem',
            padding: '1.25rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
            <div>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 600, margin: 0, color: 'var(--color-text-main)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <TrendingUp style={{ width: '1.2rem', height: '1.2rem', color: '#10b981' }} />
                Purchase vs Sales Performance ({purchaseVsSales.periodStart} to {purchaseVsSales.periodEnd})
              </h2>
              <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                Comparing received inventory from purchases against completed sales deductions
              </span>
            </div>

            <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
              <div style={{ textAlign: 'right' }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>Net Stock Delta</span>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: purchaseVsSales.netUnitsDelta >= 0 ? '#10b981' : '#ef4444' }}>
                  {purchaseVsSales.netUnitsDelta > 0 ? `+${purchaseVsSales.netUnitsDelta}` : purchaseVsSales.netUnitsDelta} units
                </div>
              </div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
            <div style={{ background: 'rgba(16, 185, 129, 0.05)', border: '1px solid rgba(16, 185, 129, 0.2)', borderRadius: '0.5rem', padding: '1rem' }}>
              <span style={{ fontSize: '0.8rem', color: '#10b981', fontWeight: 600 }}>PURCHASE RECEIPTS</span>
              <div style={{ fontSize: '1.35rem', fontWeight: 700, color: 'var(--color-text-main)', marginTop: '0.25rem' }}>
                {Number(purchaseVsSales.purchasedUnits).toLocaleString()} units
              </div>
              <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                Spend: ${Number(purchaseVsSales.purchaseCostTotal).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </span>
            </div>

            <div style={{ background: 'rgba(99, 102, 241, 0.05)', border: '1px solid rgba(99, 102, 241, 0.2)', borderRadius: '0.5rem', padding: '1rem' }}>
              <span style={{ fontSize: '0.8rem', color: '#6366f1', fontWeight: 600 }}>COMPLETED SALES</span>
              <div style={{ fontSize: '1.35rem', fontWeight: 700, color: 'var(--color-text-main)', marginTop: '0.25rem' }}>
                {Number(purchaseVsSales.soldUnits).toLocaleString()} units
              </div>
              <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                Revenue: ${Number(purchaseVsSales.salesRevenueTotal).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', borderBottom: '1px solid var(--color-border)', paddingBottom: '0.5rem' }}>
        <button
          type="button"
          onClick={() => setActiveTab('velocity')}
          style={{
            padding: '0.5rem 1rem',
            borderRadius: '0.5rem',
            border: 'none',
            background: activeTab === 'velocity' ? '#4f46e5' : 'transparent',
            color: activeTab === 'velocity' ? '#ffffff' : 'var(--color-text-muted)',
            fontWeight: 600,
            fontSize: '0.9rem',
            cursor: 'pointer'
          }}
        >
          Product Velocity (Fast & Slow Moving)
        </button>

        {canViewReorderRecs && (
          <button
            type="button"
            onClick={() => setActiveTab('reorder')}
            style={{
              padding: '0.5rem 1rem',
              borderRadius: '0.5rem',
              border: 'none',
              background: activeTab === 'reorder' ? '#4f46e5' : 'transparent',
              color: activeTab === 'reorder' ? '#ffffff' : 'var(--color-text-muted)',
              fontWeight: 600,
              fontSize: '0.9rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.35rem'
            }}
          >
            Reorder Recommendations
            {reorderRecs.length > 0 && (
              <span style={{ background: '#ef4444', color: '#fff', fontSize: '0.7rem', padding: '0.1rem 0.4rem', borderRadius: '1rem' }}>
                {reorderRecs.length}
              </span>
            )}
          </button>
        )}

        {canViewFinancials && (
          <button
            type="button"
            onClick={() => setActiveTab('valuation')}
            style={{
              padding: '0.5rem 1rem',
              borderRadius: '0.5rem',
              border: 'none',
              background: activeTab === 'valuation' ? '#4f46e5' : 'transparent',
              color: activeTab === 'valuation' ? '#ffffff' : 'var(--color-text-muted)',
              fontWeight: 600,
              fontSize: '0.9rem',
              cursor: 'pointer'
            }}
          >
            Stock Valuation
          </button>
        )}
      </div>

      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '3rem' }}>
          <LoadingSpinner size="lg" />
        </div>
      ) : (
        <>
          {/* Velocity Tab */}
          {activeTab === 'velocity' && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(450px, 1fr))', gap: '1.5rem' }}>
              {/* Fast-Moving Products */}
              <div style={{ background: 'var(--color-bg-card)', border: '1px solid var(--color-border)', borderRadius: '0.75rem', padding: '1.25rem' }}>
                <h3 style={{ margin: '0 0 1rem 0', fontSize: '1rem', fontWeight: 600, color: 'var(--color-text-main)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <TrendingUp style={{ width: '1.1rem', height: '1.1rem', color: '#10b981' }} />
                  Fast-Moving Products (Top Sellers)
                </h3>

                {fastMoving.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>
                    No completed sales recorded during this time period.
                  </div>
                ) : (
                  <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                      <thead>
                        <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-muted)', textAlign: 'left' }}>
                          <th style={{ padding: '0.5rem 0.75rem' }}>Rank</th>
                          <th style={{ padding: '0.5rem 0.75rem' }}>Product</th>
                          <th style={{ padding: '0.5rem 0.75rem' }}>SKU</th>
                          <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Units Sold</th>
                          <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Revenue</th>
                        </tr>
                      </thead>
                      <tbody>
                        {fastMoving.map((item, idx) => (
                          <tr key={item.productId} style={{ borderBottom: '1px solid var(--color-border)' }}>
                            <td style={{ padding: '0.65rem 0.75rem', fontWeight: 600, color: idx < 3 ? '#10b981' : 'var(--color-text-muted)' }}>
                              #{idx + 1}
                            </td>
                            <td style={{ padding: '0.65rem 0.75rem', fontWeight: 500, color: 'var(--color-text-main)' }}>
                              {item.productName}
                              {item.categoryName && (
                                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>{item.categoryName}</div>
                              )}
                            </td>
                            <td style={{ padding: '0.65rem 0.75rem', fontFamily: 'monospace', color: 'var(--color-text-muted)' }}>
                              {item.sku}
                            </td>
                            <td style={{ padding: '0.65rem 0.75rem', textAlign: 'right', fontWeight: 700, color: '#10b981' }}>
                              {Number(item.unitsSold).toLocaleString()}
                            </td>
                            <td style={{ padding: '0.65rem 0.75rem', textAlign: 'right', fontWeight: 600, color: 'var(--color-text-main)' }}>
                              ${Number(item.totalRevenue).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Slow-Moving Products */}
              <div style={{ background: 'var(--color-bg-card)', border: '1px solid var(--color-border)', borderRadius: '0.75rem', padding: '1.25rem' }}>
                <h3 style={{ margin: '0 0 1rem 0', fontSize: '1rem', fontWeight: 600, color: 'var(--color-text-main)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <TrendingDown style={{ width: '1.1rem', height: '1.1rem', color: '#f59e0b' }} />
                  Slow-Moving & Dead-Stock Items
                </h3>

                {slowMoving.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>
                    No products identified as slow moving.
                  </div>
                ) : (
                  <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                      <thead>
                        <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-muted)', textAlign: 'left' }}>
                          <th style={{ padding: '0.5rem 0.75rem' }}>Product</th>
                          <th style={{ padding: '0.5rem 0.75rem' }}>Status</th>
                          <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Stock</th>
                          <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Period Sales</th>
                        </tr>
                      </thead>
                      <tbody>
                        {slowMoving.map((item) => (
                          <tr key={item.productId} style={{ borderBottom: '1px solid var(--color-border)' }}>
                            <td style={{ padding: '0.65rem 0.75rem', fontWeight: 500, color: 'var(--color-text-main)' }}>
                              {item.productName}
                              <div style={{ fontSize: '0.75rem', fontFamily: 'monospace', color: 'var(--color-text-muted)' }}>{item.sku}</div>
                            </td>
                            <td style={{ padding: '0.65rem 0.75rem' }}>
                              {item.classification === 'NEVER_SOLD' ? (
                                <Badge variant="secondary" style={{ fontSize: '0.7rem' }}>Never Sold</Badge>
                              ) : item.classification === 'NO_RECENT_SALES' ? (
                                <Badge variant="warning" style={{ fontSize: '0.7rem' }}>No Recent Sales</Badge>
                              ) : (
                                <Badge variant="info" style={{ fontSize: '0.7rem' }}>Slow Moving</Badge>
                              )}
                            </td>
                            <td style={{ padding: '0.65rem 0.75rem', textAlign: 'right', fontWeight: 600, color: 'var(--color-text-main)' }}>
                              {item.currentStock}
                            </td>
                            <td style={{ padding: '0.65rem 0.75rem', textAlign: 'right', color: 'var(--color-text-muted)' }}>
                              {item.unitsSoldInPeriod} units
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Reorder Recommendations Tab */}
          {activeTab === 'reorder' && canViewReorderRecs && (
            <div style={{ background: 'var(--color-bg-card)', border: '1px solid var(--color-border)', borderRadius: '0.75rem', padding: '1.25rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600, color: 'var(--color-text-main)' }}>
                    Policy-Based Reorder Recommendations
                  </h3>
                  <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                    Standard replenishment formula restoring stock to double the configured safety threshold
                  </span>
                </div>

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

              {reorderRecs.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '3rem 1rem', color: 'var(--color-text-muted)' }}>
                  <CheckCircle style={{ width: '2.5rem', height: '2.5rem', color: '#10b981', margin: '0 auto 0.75rem auto' }} />
                  <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--color-text-main)' }}>All Stock Levels are Healthy</div>
                  <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.85rem' }}>No products have breached their configured reorder thresholds.</p>
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-muted)', textAlign: 'left' }}>
                        <th style={{ padding: '0.5rem 0.75rem' }}>Product</th>
                        <th style={{ padding: '0.5rem 0.75rem' }}>SKU</th>
                        <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Current Stock</th>
                        <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Reorder Level</th>
                        <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Suggested Quantity</th>
                        <th style={{ padding: '0.5rem 0.75rem' }}>Recommendation Reason</th>
                      </tr>
                    </thead>
                    <tbody>
                      {reorderRecs.map((rec) => (
                        <tr key={rec.productId} style={{ borderBottom: '1px solid var(--color-border)' }}>
                          <td style={{ padding: '0.75rem', fontWeight: 600, color: 'var(--color-text-main)' }}>
                            {rec.productName}
                            {rec.categoryName && (
                              <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>{rec.categoryName}</div>
                            )}
                          </td>
                          <td style={{ padding: '0.75rem', fontFamily: 'monospace', color: 'var(--color-text-muted)' }}>
                            {rec.sku}
                          </td>
                          <td style={{ padding: '0.75rem', textAlign: 'right', fontWeight: 700, color: rec.currentStock === 0 ? '#ef4444' : '#f59e0b' }}>
                            {rec.currentStock}
                          </td>
                          <td style={{ padding: '0.75rem', textAlign: 'right', color: 'var(--color-text-muted)' }}>
                            {rec.reorderLevel}
                          </td>
                          <td style={{ padding: '0.75rem', textAlign: 'right', fontWeight: 700, color: '#4f46e5' }}>
                            +{rec.suggestedReplenishmentQuantity} units
                          </td>
                          <td style={{ padding: '0.75rem', color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>
                            {rec.recommendationReason}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* Stock Valuation Tab */}
          {activeTab === 'valuation' && canViewFinancials && stockValuation && (
            <div style={{ background: 'var(--color-bg-card)', border: '1px solid var(--color-border)', borderRadius: '0.75rem', padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600, color: 'var(--color-text-main)' }}>
                  Inventory Valuation Report
                </h3>
                <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                  Catalog-based stock valuation breakdown across current inventory units
                </span>
              </div>

              {/* Notice Banner */}
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.75rem', background: 'rgba(59, 130, 246, 0.08)', border: '1px solid rgba(59, 130, 246, 0.25)', borderRadius: '0.5rem', padding: '0.75rem 1rem', fontSize: '0.85rem', color: '#1e40af' }}>
                <Info style={{ width: '1.2rem', height: '1.2rem', flexShrink: 0, marginTop: '0.1rem', color: '#3b82f6' }} />
                <div>
                  <span style={{ fontWeight: 600 }}>Cost Valuation Notice: </span>
                  {stockValuation.explanation}
                </div>
              </div>

              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-muted)', textAlign: 'left' }}>
                      <th style={{ padding: '0.5rem 0.75rem' }}>Product</th>
                      <th style={{ padding: '0.5rem 0.75rem' }}>SKU</th>
                      <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Units in Stock</th>
                      <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Selling Price</th>
                      <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Total Potential Retail Value</th>
                    </tr>
                  </thead>
                  <tbody>
                    {stockValuation.products?.content?.map((item) => (
                      <tr key={item.productId} style={{ borderBottom: '1px solid var(--color-border)' }}>
                        <td style={{ padding: '0.75rem', fontWeight: 500, color: 'var(--color-text-main)' }}>
                          {item.productName}
                        </td>
                        <td style={{ padding: '0.75rem', fontFamily: 'monospace', color: 'var(--color-text-muted)' }}>
                          {item.sku}
                        </td>
                        <td style={{ padding: '0.75rem', textAlign: 'right', fontWeight: 600, color: 'var(--color-text-main)' }}>
                          {item.quantity}
                        </td>
                        <td style={{ padding: '0.75rem', textAlign: 'right', color: 'var(--color-text-muted)' }}>
                          ${Number(item.unitSellingPrice).toFixed(2)}
                        </td>
                        <td style={{ padding: '0.75rem', textAlign: 'right', fontWeight: 700, color: '#3b82f6' }}>
                          ${Number(item.totalRetailValue).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
