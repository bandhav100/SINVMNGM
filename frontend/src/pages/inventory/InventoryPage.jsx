import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { inventoryApi } from '../../api/inventoryApi';
import { categoryApi } from '../../api/categoryApi';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Modal from '../../components/common/Modal';
import {
  Layers,
  Search,
  Filter,
  PlusCircle,
  MinusCircle,
  AlertTriangle,
  CheckCircle2,
  AlertOctagon,
  TrendingDown,
  TrendingUp,
  RefreshCw,
  History,
  FileText,
  Sliders,
  IndianRupee,
  Package,
  ChevronLeft,
  ChevronRight,
  ShieldAlert,
  ArrowUpDown
} from 'lucide-react';

const COMMON_REASONS = [
  'Cycle Count Audit Discrepancy',
  'Damaged or Defective Goods Disposed',
  'Found Unrecorded Physical Stock',
  'Supplier Return / RTV',
  'Internal Sample / Store Demonstration',
  'Shrinkage / Unaccounted Loss',
  'Corrective Data Entry'
];

export default function InventoryPage() {
  const { user } = useAuth();
  const role = user?.role || '';
  const canAdjust = role === 'ADMIN' || role === 'INVENTORY_MANAGER';

  // Active sub-view tab: 'stock', 'transactions', 'adjustments'
  const [activeTab, setActiveTab] = useState('stock');

  // Summary KPIs
  const [summary, setSummary] = useState(null);
  const [loadingSummary, setLoadingSummary] = useState(false);

  // Stock Table State
  const [stockItems, setStockItems] = useState([]);
  const [stockPage, setStockPage] = useState(0);
  const [stockTotalPages, setStockTotalPages] = useState(1);
  const [stockTotalElements, setStockTotalElements] = useState(0);
  const [loadingStock, setLoadingStock] = useState(false);

  // Filters for Stock
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [stockStatus, setStockStatus] = useState('ALL');
  const [categories, setCategories] = useState([]);

  // Transaction Ledger State
  const [transactions, setTransactions] = useState([]);
  const [txPage, setTxPage] = useState(0);
  const [txTotalPages, setTxTotalPages] = useState(1);
  const [txTotalElements, setTxTotalElements] = useState(0);
  const [txType, setTxType] = useState('');
  const [txSearch, setTxSearch] = useState('');
  const [loadingTx, setLoadingTx] = useState(false);

  // Stock Adjustments Audit State
  const [adjustments, setAdjustments] = useState([]);
  const [adjPage, setAdjPage] = useState(0);
  const [adjTotalPages, setAdjTotalPages] = useState(1);
  const [loadingAdj, setLoadingAdj] = useState(false);

  // Modal State for Stock Adjustment
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [adjDeltaSign, setAdjDeltaSign] = useState(1); // 1 for increase, -1 for decrease
  const [adjMagnitude, setAdjMagnitude] = useState('');
  const [adjReasonSelect, setAdjReasonSelect] = useState(COMMON_REASONS[0]);
  const [adjCustomReason, setAdjCustomReason] = useState('');
  const [adjNotes, setAdjNotes] = useState('');
  const [modalSubmitting, setModalSubmitting] = useState(false);
  const [modalError, setModalError] = useState(null);
  const [bannerSuccess, setBannerSuccess] = useState(null);
  const [pageError, setPageError] = useState(null);

  // Fetch Categories for dropdown
  useEffect(() => {
    (categoryApi.getAllCategories || categoryApi.getAll)()
      .then(res => setCategories(Array.isArray(res) ? res : (res?.data || [])))
      .catch(err => console.error('Failed to load categories:', err));
  }, []);

  // Fetch Summary
  const fetchSummary = useCallback(async () => {
    try {
      setLoadingSummary(true);
      const res = await inventoryApi.getSummary();
      setSummary(res);
    } catch (err) {
      console.error('Failed to fetch inventory summary:', err);
    } finally {
      setLoadingSummary(false);
    }
  }, []);

  // Fetch Stock
  const fetchStock = useCallback(async () => {
    try {
      setLoadingStock(true);
      setPageError(null);
      const params = {
        page: stockPage,
        size: 15,
        status: stockStatus,
        search: search.trim() || undefined,
        categoryId: categoryId || undefined
      };
      const res = await inventoryApi.getStock(params);
      setStockItems(res.content || []);
      setStockTotalPages(res.totalPages || 1);
      setStockTotalElements(res.totalElements || 0);
    } catch (err) {
      setPageError(err.message || 'Failed to load stock levels');
    } finally {
      setLoadingStock(false);
    }
  }, [stockPage, stockStatus, search, categoryId]);

  // Fetch Transactions
  const fetchTransactions = useCallback(async () => {
    try {
      setLoadingTx(true);
      const params = {
        page: txPage,
        size: 15,
        transactionType: txType || undefined,
        search: txSearch.trim() || undefined
      };
      const res = await inventoryApi.getTransactions(params);
      setTransactions(res.content || []);
      setTxTotalPages(res.totalPages || 1);
      setTxTotalElements(res.totalElements || 0);
    } catch (err) {
      console.error('Failed to load transactions:', err);
    } finally {
      setLoadingTx(false);
    }
  }, [txPage, txType, txSearch]);

  // Fetch Adjustments
  const fetchAdjustments = useCallback(async () => {
    try {
      setLoadingAdj(true);
      const params = { page: adjPage, size: 15 };
      const res = await inventoryApi.getAdjustments(params);
      setAdjustments(res.content || []);
      setAdjTotalPages(res.totalPages || 1);
    } catch (err) {
      console.error('Failed to load adjustments:', err);
    } finally {
      setLoadingAdj(false);
    }
  }, [adjPage]);

  // Initial load
  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  useEffect(() => {
    if (activeTab === 'stock') {
      fetchStock();
    } else if (activeTab === 'transactions') {
      fetchTransactions();
    } else if (activeTab === 'adjustments') {
      fetchAdjustments();
    }
  }, [activeTab, fetchStock, fetchTransactions, fetchAdjustments]);

  // Open adjustment modal
  const handleOpenAdjustModal = (product = null) => {
    setSelectedProduct(product);
    setAdjDeltaSign(1);
    setAdjMagnitude('');
    setAdjReasonSelect(COMMON_REASONS[0]);
    setAdjCustomReason('');
    setAdjNotes('');
    setModalError(null);
    setIsModalOpen(true);
  };

  // Submit stock adjustment
  const handleAdjustmentSubmit = async (e) => {
    e.preventDefault();
    setModalError(null);

    if (!selectedProduct) {
      setModalError('Please select a product to adjust.');
      return;
    }

    const mag = parseInt(adjMagnitude, 10);
    if (isNaN(mag) || mag <= 0) {
      setModalError('Please enter a valid positive quantity to adjust.');
      return;
    }

    const calculatedDelta = adjDeltaSign * mag;
    const finalReason = adjReasonSelect === 'Other'
      ? adjCustomReason.trim()
      : adjReasonSelect;

    if (!finalReason) {
      setModalError('Reason for stock adjustment is mandatory.');
      return;
    }

    const resultingStock = (selectedProduct.quantity || 0) + calculatedDelta;
    if (resultingStock < 0) {
      setModalError(`Insufficient stock: adjustment of ${calculatedDelta} would leave negative stock (${resultingStock}). Current stock is ${selectedProduct.quantity}.`);
      return;
    }

    try {
      setModalSubmitting(true);
      const payload = {
        productId: selectedProduct.id,
        quantityDelta: calculatedDelta,
        reason: finalReason,
        notes: adjNotes.trim() || undefined
      };
      const res = await inventoryApi.adjustStock(payload);
      setIsModalOpen(false);
      setBannerSuccess(`Stock for "${res.productName}" updated: ${res.quantityBefore} → ${res.quantityAfter} units (${res.quantityDelta > 0 ? '+' : ''}${res.quantityDelta}).`);

      // Refresh data
      fetchSummary();
      if (activeTab === 'stock') fetchStock();
      if (activeTab === 'transactions') fetchTransactions();
      if (activeTab === 'adjustments') fetchAdjustments();
    } catch (err) {
      setModalError(err.message || 'Failed to submit stock adjustment.');
    } finally {
      setModalSubmitting(false);
    }
  };

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(Number(val || 0));
  };

  const formatTimestamp = (ts) => {
    if (!ts) return '—';
    try {
      return new Date(ts).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return ts;
    }
  };

  return (
    <div className="page-container" style={{ padding: '1.5rem 2rem', maxWidth: '1400px', margin: '0 auto' }}>
      {/* Page Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
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
            <Layers size={22} />
          </div>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--color-text-primary)' }}>Inventory & Stock Ledger</h1>
            <p style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)' }}>
              Real-time stock control, safety thresholds, and immutable ledger audit records
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Button
            variant="outline"
            onClick={() => {
              fetchSummary();
              if (activeTab === 'stock') fetchStock();
              if (activeTab === 'transactions') fetchTransactions();
              if (activeTab === 'adjustments') fetchAdjustments();
            }}
            icon={RefreshCw}
            style={{ fontSize: '0.85rem' }}
          >
            Refresh
          </Button>

          {canAdjust && (
            <Button
              variant="primary"
              onClick={() => handleOpenAdjustModal(null)}
              icon={Sliders}
              style={{ fontSize: '0.85rem' }}
            >
              Adjust Stock
            </Button>
          )}
        </div>
      </div>

      {bannerSuccess && (
        <AlertBanner
          type="success"
          message={bannerSuccess}
          onClose={() => setBannerSuccess(null)}
          style={{ marginBottom: '1.25rem' }}
        />
      )}

      {pageError && (
        <AlertBanner
          type="error"
          message={pageError}
          onClose={() => setPageError(null)}
          style={{ marginBottom: '1.25rem' }}
        />
      )}

      {/* Summary KPI Tiles */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '1rem',
        marginBottom: '1.5rem'
      }}>
        <div className="card" style={{ padding: '1rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: '500', marginBottom: '0.25rem' }}>Total Products</div>
          <div style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--color-text-primary)' }}>
            {summary ? Number(summary.totalProducts).toLocaleString() : '—'} <span style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', fontWeight: 'normal' }}>SKUs</span>
          </div>
        </div>

        <div className="card" style={{ padding: '1rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: '500', marginBottom: '0.25rem' }}>Total Units in Stock</div>
          <div style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--color-primary)' }}>
            {summary ? Number(summary.totalStockUnits).toLocaleString() : '—'}
          </div>
        </div>

        <div className="card" style={{ padding: '1rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: '500', marginBottom: '0.25rem' }}>Low Stock Warning</div>
          <div style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--color-warning)' }}>
            {summary ? Number(summary.lowStockCount).toLocaleString() : '—'}
          </div>
        </div>

        <div className="card" style={{ padding: '1rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: '500', marginBottom: '0.25rem' }}>Out of Stock</div>
          <div style={{ fontSize: '1.5rem', fontWeight: '700', color: summary?.outOfStockCount > 0 ? 'var(--color-danger)' : 'var(--color-success)' }}>
            {summary ? Number(summary.outOfStockCount).toLocaleString() : '—'}
          </div>
        </div>

        <div className="card" style={{ padding: '1rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: '500', marginBottom: '0.25rem' }}>Total Valuation</div>
          <div style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--color-text-primary)' }}>
            {summary ? formatCurrency(summary.totalRetailValue) : '—'}
          </div>
        </div>
      </div>

      {/* View Switcher Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', borderBottom: '1px solid var(--color-border)', marginBottom: '1.25rem' }}>
        <button
          onClick={() => setActiveTab('stock')}
          style={{
            padding: '0.65rem 1.25rem',
            border: 'none',
            background: 'none',
            fontSize: '0.9rem',
            fontWeight: activeTab === 'stock' ? '600' : '500',
            color: activeTab === 'stock' ? 'var(--color-primary)' : 'var(--color-text-secondary)',
            borderBottom: activeTab === 'stock' ? '2px solid var(--color-primary)' : '2px solid transparent',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.4rem'
          }}
        >
          <Package size={16} /> Stock Levels ({stockTotalElements})
        </button>

        <button
          onClick={() => setActiveTab('transactions')}
          style={{
            padding: '0.65rem 1.25rem',
            border: 'none',
            background: 'none',
            fontSize: '0.9rem',
            fontWeight: activeTab === 'transactions' ? '600' : '500',
            color: activeTab === 'transactions' ? 'var(--color-primary)' : 'var(--color-text-secondary)',
            borderBottom: activeTab === 'transactions' ? '2px solid var(--color-primary)' : '2px solid transparent',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.4rem'
          }}
        >
          <History size={16} /> Transaction Ledger ({txTotalElements})
        </button>

        <button
          onClick={() => setActiveTab('adjustments')}
          style={{
            padding: '0.65rem 1.25rem',
            border: 'none',
            background: 'none',
            fontSize: '0.9rem',
            fontWeight: activeTab === 'adjustments' ? '600' : '500',
            color: activeTab === 'adjustments' ? 'var(--color-primary)' : 'var(--color-text-secondary)',
            borderBottom: activeTab === 'adjustments' ? '2px solid var(--color-primary)' : '2px solid transparent',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.4rem'
          }}
        >
          <FileText size={16} /> Adjustment Audit Log
        </button>
      </div>

      {/* Tab 1: Current Stock Levels */}
      {activeTab === 'stock' && (
        <div>
          {/* Filters */}
          <div style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '0.75rem',
            marginBottom: '1rem',
            padding: '0.75rem 1rem',
            background: 'var(--color-surface)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)'
          }}>
            <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.75rem', flex: 1 }}>
              <div style={{ position: 'relative', minWidth: '240px' }}>
                <Search size={16} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-muted)' }} />
                <input
                  type="text"
                  placeholder="Filter SKU or product name..."
                  value={search}
                  onChange={(e) => { setSearch(e.target.value); setStockPage(0); }}
                  style={{
                    width: '100%',
                    padding: '0.45rem 0.75rem 0.45rem 2.2rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--color-border)',
                    fontSize: '0.85rem'
                  }}
                />
              </div>

              <select
                value={categoryId}
                onChange={(e) => { setCategoryId(e.target.value); setStockPage(0); }}
                style={{
                  padding: '0.45rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                  fontSize: '0.85rem',
                  background: 'var(--color-surface)'
                }}
              >
                <option value="">All Categories</option>
                {categories.map(cat => (
                  <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
              </select>

              <select
                value={stockStatus}
                onChange={(e) => { setStockStatus(e.target.value); setStockPage(0); }}
                style={{
                  padding: '0.45rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                  fontSize: '0.85rem',
                  background: 'var(--color-surface)'
                }}
              >
                <option value="ALL">All Stock Statuses</option>
                <option value="IN_STOCK">Adequate Stock</option>
                <option value="LOW_STOCK">Low Stock (≤ Reorder Level)</option>
                <option value="OUT_OF_STOCK">Out of Stock (0 units)</option>
              </select>
            </div>

            {(search || categoryId || stockStatus !== 'ALL') && (
              <Button
                variant="outline"
                onClick={() => { setSearch(''); setCategoryId(''); setStockStatus('ALL'); setStockPage(0); }}
                style={{ fontSize: '0.8rem', padding: '0.4rem 0.75rem' }}
              >
                Reset Filters
              </Button>
            )}
          </div>

          {/* Table */}
          <div className="card" style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', overflow: 'hidden' }}>
            {loadingStock ? (
              <div style={{ padding: '3rem', textAlign: 'center' }}>
                <LoadingSpinner />
                <p style={{ marginTop: '0.75rem', color: 'var(--color-text-secondary)', fontSize: '0.85rem' }}>Loading inventory items...</p>
              </div>
            ) : stockItems.length === 0 ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
                <Package size={36} style={{ margin: '0 auto 0.75rem', opacity: 0.5 }} />
                <p style={{ fontWeight: '600', color: 'var(--color-text-primary)' }}>No Products Found</p>
                <p style={{ fontSize: '0.85rem' }}>No inventory items matched your selected filter criteria.</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--color-border)', background: 'var(--color-surface-subtle)', color: 'var(--color-text-secondary)' }}>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Product & SKU</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Category</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Price</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Stock Level</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Safety Threshold</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Status</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Valuation</th>
                      {canAdjust && <th style={{ padding: '0.75rem 1rem', fontWeight: '600', textAlign: 'right' }}>Action</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {stockItems.map(item => {
                      const isOos = item.quantity === 0;
                      const isLow = !isOos && item.quantity <= item.reorderLevel;

                      return (
                        <tr key={item.id} style={{ borderBottom: '1px solid var(--color-border)', transition: 'background-color var(--transition-fast)' }}>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <div style={{ fontWeight: '600', color: 'var(--color-text-primary)' }}>{item.name}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontFamily: 'monospace' }}>{item.sku}</div>
                          </td>
                          <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-secondary)' }}>
                            {item.categoryName}
                          </td>
                          <td style={{ padding: '0.75rem 1rem', fontWeight: '500' }}>
                            {formatCurrency(item.price)}
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <span style={{ fontWeight: '700', color: isOos ? 'var(--color-danger)' : (isLow ? 'var(--color-warning)' : 'var(--color-text-primary)') }}>
                              {Number(item.quantity).toLocaleString()} units
                            </span>
                          </td>
                          <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-secondary)' }}>
                            {item.reorderLevel} units
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            {isOos ? (
                              <Badge variant="danger">Out of Stock</Badge>
                            ) : isLow ? (
                              <Badge variant="warning">Low Stock</Badge>
                            ) : (
                              <Badge variant="success">In Stock</Badge>
                            )}
                          </td>
                          <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-secondary)', fontWeight: '500' }}>
                            {formatCurrency(item.potentialRetailValue)}
                          </td>
                          {canAdjust && (
                            <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                              <Button
                                variant="outline"
                                onClick={() => handleOpenAdjustModal(item)}
                                style={{ fontSize: '0.75rem', padding: '0.3rem 0.65rem' }}
                              >
                                Adjust
                              </Button>
                            </td>
                          )}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}

            {/* Pagination */}
            {stockTotalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', borderTop: '1px solid var(--color-border)', fontSize: '0.85rem' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>Page {stockPage + 1} of {stockTotalPages} ({stockTotalElements} total)</span>
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                  <Button
                    variant="outline"
                    disabled={stockPage === 0}
                    onClick={() => setStockPage(prev => Math.max(0, prev - 1))}
                    style={{ padding: '0.3rem 0.6rem', fontSize: '0.8rem' }}
                  >
                    <ChevronLeft size={14} /> Previous
                  </Button>
                  <Button
                    variant="outline"
                    disabled={stockPage >= stockTotalPages - 1}
                    onClick={() => setStockPage(prev => prev + 1)}
                    style={{ padding: '0.3rem 0.6rem', fontSize: '0.8rem' }}
                  >
                    Next <ChevronRight size={14} />
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab 2: Transaction Ledger */}
      {activeTab === 'transactions' && (
        <div>
          {/* Ledger Filters */}
          <div style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            gap: '0.75rem',
            marginBottom: '1rem',
            padding: '0.75rem 1rem',
            background: 'var(--color-surface)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)'
          }}>
            <div style={{ position: 'relative', minWidth: '240px' }}>
              <Search size={16} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-muted)' }} />
              <input
                type="text"
                placeholder="Search SKU, product or note..."
                value={txSearch}
                onChange={(e) => { setTxSearch(e.target.value); setTxPage(0); }}
                style={{
                  width: '100%',
                  padding: '0.45rem 0.75rem 0.45rem 2.2rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                  fontSize: '0.85rem'
                }}
              />
            </div>

            <select
              value={txType}
              onChange={(e) => { setTxType(e.target.value); setTxPage(0); }}
              style={{
                padding: '0.45rem 0.75rem',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                fontSize: '0.85rem',
                background: 'var(--color-surface)'
              }}
            >
              <option value="">All Movement Types</option>
              <option value="PURCHASE_RECEIPT">Purchase Receipt (Inbound)</option>
              <option value="SALE_DEDUCTION">Sale Deduction (Outbound)</option>
              <option value="MANUAL_ADJUSTMENT">Manual Adjustment</option>
            </select>
          </div>

          <div className="card" style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', overflow: 'hidden' }}>
            {loadingTx ? (
              <div style={{ padding: '3rem', textAlign: 'center' }}>
                <LoadingSpinner />
                <p style={{ marginTop: '0.75rem', color: 'var(--color-text-secondary)', fontSize: '0.85rem' }}>Loading transaction ledger...</p>
              </div>
            ) : transactions.length === 0 ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
                <History size={36} style={{ margin: '0 auto 0.75rem', opacity: 0.5 }} />
                <p style={{ fontWeight: '600', color: 'var(--color-text-primary)' }}>No Transaction Entries</p>
                <p style={{ fontSize: '0.85rem' }}>Stock receipts, sales deductions, and adjustments will be logged here.</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--color-border)', background: 'var(--color-surface-subtle)', color: 'var(--color-text-secondary)' }}>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Timestamp</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Product</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Type</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Delta</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Before → After</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Reference</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Performed By</th>
                      <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Audit Notes</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map(tx => {
                      const isPositive = tx.quantityDelta > 0;
                      let badgeVariant = 'secondary';
                      if (tx.transactionType === 'PURCHASE_RECEIPT') badgeVariant = 'primary';
                      else if (tx.transactionType === 'SALE_DEDUCTION') badgeVariant = 'success';
                      else if (tx.transactionType === 'MANUAL_ADJUSTMENT') badgeVariant = 'warning';

                      return (
                        <tr key={tx.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                          <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-muted)', whiteSpace: 'nowrap' }}>
                            {formatTimestamp(tx.createdAt)}
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <div style={{ fontWeight: '600', color: 'var(--color-text-primary)' }}>{tx.productName}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontFamily: 'monospace' }}>{tx.sku}</div>
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <Badge variant={badgeVariant}>{tx.transactionType.replace('_', ' ')}</Badge>
                          </td>
                          <td style={{ padding: '0.75rem 1rem', fontWeight: '700', color: isPositive ? 'var(--color-success)' : 'var(--color-danger)' }}>
                            {isPositive ? `+${tx.quantityDelta}` : tx.quantityDelta}
                          </td>
                          <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-secondary)', fontFamily: 'monospace' }}>
                            {tx.quantityBefore} → {tx.quantityAfter}
                          </td>
                          <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>
                            {tx.referenceType} #{tx.referenceId}
                          </td>
                          <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-secondary)' }}>
                            {tx.performedByUsername}
                          </td>
                          <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-secondary)', maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={tx.notes}>
                            {tx.notes || '—'}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}

            {txTotalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', borderTop: '1px solid var(--color-border)', fontSize: '0.85rem' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>Page {txPage + 1} of {txTotalPages}</span>
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                  <Button
                    variant="outline"
                    disabled={txPage === 0}
                    onClick={() => setTxPage(prev => Math.max(0, prev - 1))}
                    style={{ padding: '0.3rem 0.6rem', fontSize: '0.8rem' }}
                  >
                    <ChevronLeft size={14} /> Previous
                  </Button>
                  <Button
                    variant="outline"
                    disabled={txPage >= txTotalPages - 1}
                    onClick={() => setTxPage(prev => prev + 1)}
                    style={{ padding: '0.3rem 0.6rem', fontSize: '0.8rem' }}
                  >
                    Next <ChevronRight size={14} />
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab 3: Adjustments Audit Log */}
      {activeTab === 'adjustments' && (
        <div className="card" style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-lg)', background: 'var(--color-surface)', overflow: 'hidden' }}>
          {loadingAdj ? (
            <div style={{ padding: '3rem', textAlign: 'center' }}>
              <LoadingSpinner />
              <p style={{ marginTop: '0.75rem', color: 'var(--color-text-secondary)', fontSize: '0.85rem' }}>Loading adjustment audit log...</p>
            </div>
          ) : adjustments.length === 0 ? (
            <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
              <FileText size={36} style={{ margin: '0 auto 0.75rem', opacity: 0.5 }} />
              <p style={{ fontWeight: '600', color: 'var(--color-text-primary)' }}>No Manual Adjustments Recorded</p>
              <p style={{ fontSize: '0.85rem' }}>Authorized physical adjustments will appear here with audit timestamps.</p>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--color-border)', background: 'var(--color-surface-subtle)', color: 'var(--color-text-secondary)' }}>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Timestamp</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Product</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Delta</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Stock Change</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Mandatory Reason</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Audit Notes</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Adjusted By</th>
                  </tr>
                </thead>
                <tbody>
                  {adjustments.map(adj => {
                    const isPositive = adj.quantityDelta > 0;
                    return (
                      <tr key={adj.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                        <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-muted)', whiteSpace: 'nowrap' }}>
                          {formatTimestamp(adj.createdAt)}
                        </td>
                        <td style={{ padding: '0.75rem 1rem' }}>
                          <div style={{ fontWeight: '600', color: 'var(--color-text-primary)' }}>{adj.productName}</div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontFamily: 'monospace' }}>{adj.sku}</div>
                        </td>
                        <td style={{ padding: '0.75rem 1rem', fontWeight: '700', color: isPositive ? 'var(--color-success)' : 'var(--color-danger)' }}>
                          {isPositive ? `+${adj.quantityDelta}` : adj.quantityDelta}
                        </td>
                        <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-secondary)', fontFamily: 'monospace' }}>
                          {adj.quantityBefore} → {adj.quantityAfter} units
                        </td>
                        <td style={{ padding: '0.75rem 1rem', fontWeight: '500', color: 'var(--color-text-primary)' }}>
                          {adj.reason}
                        </td>
                        <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-secondary)' }}>
                          {adj.notes || '—'}
                        </td>
                        <td style={{ padding: '0.75rem 1rem', color: 'var(--color-text-secondary)' }}>
                          {adj.adjustedByUsername}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {adjTotalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', borderTop: '1px solid var(--color-border)', fontSize: '0.85rem' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Page {adjPage + 1} of {adjTotalPages}</span>
              <div style={{ display: 'flex', gap: '0.4rem' }}>
                <Button
                  variant="outline"
                  disabled={adjPage === 0}
                  onClick={() => setAdjPage(prev => Math.max(0, prev - 1))}
                  style={{ padding: '0.3rem 0.6rem', fontSize: '0.8rem' }}
                >
                  <ChevronLeft size={14} /> Previous
                </Button>
                <Button
                  variant="outline"
                  disabled={adjPage >= adjTotalPages - 1}
                  onClick={() => setAdjPage(prev => prev + 1)}
                  style={{ padding: '0.3rem 0.6rem', fontSize: '0.8rem' }}
                >
                  Next <ChevronRight size={14} />
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Stock Adjustment Modal */}
      {isModalOpen && (
        <Modal
          isOpen={isModalOpen}
          onClose={() => !modalSubmitting && setIsModalOpen(false)}
          title="Authorized Stock Adjustment"
        >
          <form onSubmit={handleAdjustmentSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {modalError && (
              <AlertBanner
                type="error"
                message={modalError}
                onClose={() => setModalError(null)}
              />
            )}

            {/* Product Selection */}
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '600', marginBottom: '0.35rem', color: 'var(--color-text-primary)' }}>
                Select Product *
              </label>
              {selectedProduct ? (
                <div style={{
                  padding: '0.75rem',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--color-surface-subtle)',
                  border: '1px solid var(--color-border)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}>
                  <div>
                    <strong style={{ color: 'var(--color-text-primary)' }}>{selectedProduct.name}</strong>
                    <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>SKU: {selectedProduct.sku}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <span style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', display: 'block' }}>Current Stock:</span>
                    <strong style={{ fontSize: '1.1rem', color: 'var(--color-text-primary)' }}>{selectedProduct.quantity} units</strong>
                  </div>
                </div>
              ) : (
                <select
                  required
                  onChange={(e) => {
                    const prod = stockItems.find(p => p.id === Number(e.target.value));
                    setSelectedProduct(prod || null);
                  }}
                  style={{
                    width: '100%',
                    padding: '0.55rem 0.75rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--color-border)',
                    fontSize: '0.875rem',
                    background: 'var(--color-surface)'
                  }}
                >
                  <option value="">-- Choose product --</option>
                  {stockItems.map(p => (
                    <option key={p.id} value={p.id}>{p.name} ({p.sku}) - Current: {p.quantity} units</option>
                  ))}
                </select>
              )}
            </div>

            {/* Adjustment Type & Quantity */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '600', marginBottom: '0.35rem', color: 'var(--color-text-primary)' }}>
                  Operation Type *
                </label>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    type="button"
                    onClick={() => setAdjDeltaSign(1)}
                    style={{
                      flex: 1,
                      padding: '0.5rem',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid',
                      borderColor: adjDeltaSign === 1 ? 'var(--color-success)' : 'var(--color-border)',
                      background: adjDeltaSign === 1 ? 'var(--color-success-light)' : 'var(--color-surface)',
                      color: adjDeltaSign === 1 ? 'var(--color-success)' : 'var(--color-text-secondary)',
                      fontWeight: '600',
                      fontSize: '0.8rem',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '0.25rem'
                    }}
                  >
                    <PlusCircle size={14} /> Add (+)
                  </button>
                  <button
                    type="button"
                    onClick={() => setAdjDeltaSign(-1)}
                    style={{
                      flex: 1,
                      padding: '0.5rem',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid',
                      borderColor: adjDeltaSign === -1 ? 'var(--color-danger)' : 'var(--color-border)',
                      background: adjDeltaSign === -1 ? 'var(--color-danger-light)' : 'var(--color-surface)',
                      color: adjDeltaSign === -1 ? 'var(--color-danger)' : 'var(--color-text-secondary)',
                      fontWeight: '600',
                      fontSize: '0.8rem',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '0.25rem'
                    }}
                  >
                    <MinusCircle size={14} /> Deduct (-)
                  </button>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '600', marginBottom: '0.35rem', color: 'var(--color-text-primary)' }}>
                  Units to Adjust *
                </label>
                <input
                  type="number"
                  min="1"
                  required
                  placeholder="e.g. 5"
                  value={adjMagnitude}
                  onChange={(e) => setAdjMagnitude(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '0.55rem 0.75rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--color-border)',
                    fontSize: '0.875rem'
                  }}
                />
              </div>
            </div>

            {/* Resulting Stock Preview */}
            {selectedProduct && adjMagnitude && (
              <div style={{
                padding: '0.65rem 0.85rem',
                borderRadius: 'var(--radius-md)',
                background: (Number(selectedProduct.quantity) + (adjDeltaSign * Number(adjMagnitude)) < 0) ? 'var(--color-danger-light)' : 'var(--color-surface-subtle)',
                border: '1px solid var(--color-border)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                fontSize: '0.85rem'
              }}>
                <span>Projected New Stock:</span>
                <strong style={{
                  color: (Number(selectedProduct.quantity) + (adjDeltaSign * Number(adjMagnitude)) < 0) ? 'var(--color-danger)' : 'var(--color-text-primary)',
                  fontSize: '1rem'
                }}>
                  {Number(selectedProduct.quantity) + (adjDeltaSign * Number(adjMagnitude))} units
                </strong>
              </div>
            )}

            {/* Mandatory Reason */}
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '600', marginBottom: '0.35rem', color: 'var(--color-text-primary)' }}>
                Mandatory Reason *
              </label>
              <select
                value={adjReasonSelect}
                onChange={(e) => setAdjReasonSelect(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.55rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                  fontSize: '0.875rem',
                  background: 'var(--color-surface)',
                  marginBottom: adjReasonSelect === 'Other' ? '0.5rem' : '0'
                }}
              >
                {COMMON_REASONS.map(r => (
                  <option key={r} value={r}>{r}</option>
                ))}
                <option value="Other">Other (Specify Below)</option>
              </select>

              {adjReasonSelect === 'Other' && (
                <input
                  type="text"
                  required
                  placeholder="Specify exact adjustment reason..."
                  value={adjCustomReason}
                  onChange={(e) => setAdjCustomReason(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '0.55rem 0.75rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--color-border)',
                    fontSize: '0.875rem'
                  }}
                />
              )}
            </div>

            {/* Optional Audit Notes */}
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '600', marginBottom: '0.35rem', color: 'var(--color-text-primary)' }}>
                Audit Notes (Optional)
              </label>
              <textarea
                rows="2"
                placeholder="Additional audit notes, supervisor reference, or context..."
                value={adjNotes}
                onChange={(e) => setAdjNotes(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.55rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                  fontSize: '0.875rem',
                  fontFamily: 'inherit'
                }}
              />
            </div>

            {/* Buttons */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <Button
                type="button"
                variant="outline"
                disabled={modalSubmitting}
                onClick={() => setIsModalOpen(false)}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                disabled={modalSubmitting}
              >
                {modalSubmitting ? 'Recording Adjustment...' : 'Confirm Adjustment'}
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
