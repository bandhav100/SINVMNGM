import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { reportApi } from '../../api/reportApi';
import { categoryApi } from '../../api/categoryApi';
import { supplierApi } from '../../api/supplierApi';
import { useAuth } from '../../context/AuthContext';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  FileBarChart,
  Package,
  TrendingUp,
  ShoppingCart,
  ArrowRightLeft,
  DollarSign,
  Calendar,
  Filter,
  Search,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Layers,
  Building,
  User,
  ArrowUpRight,
  ArrowDownLeft,
  Info,
  ChevronLeft,
  ChevronRight,
  Sparkles
} from 'lucide-react';

export default function ReportsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Role permissions
  const role = user?.role || '';
  const canAccessSales = role === 'ADMIN' || role === 'INVENTORY_MANAGER' || role === 'SALES_STAFF';
  const canAccessPurchases = role === 'ADMIN' || role === 'INVENTORY_MANAGER' || role === 'PURCHASE_MANAGER';
  const canAccessMovements = role === 'ADMIN' || role === 'INVENTORY_MANAGER' || role === 'PURCHASE_MANAGER';
  const canAccessValuation = role === 'ADMIN' || role === 'INVENTORY_MANAGER' || role === 'PURCHASE_MANAGER';
  const canAccessInventory = true; // All 4 roles

  // Active tab selection
  const defaultTab = canAccessInventory ? 'inventory' : (canAccessSales ? 'sales' : 'purchases');
  const [activeTab, setActiveTab] = useState(defaultTab);

  // Categories & Suppliers lists for dropdown filters
  const [categories, setCategories] = useState([]);
  const [suppliers, setSuppliers] = useState([]);

  // Common Filter states
  const [datePreset, setDatePreset] = useState('30'); // '7', '30', '90', '180', 'custom'
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize] = useState(15);

  // Tab-specific filters
  const [selectedCategory, setSelectedCategory] = useState('');
  const [inventoryStatus, setInventoryStatus] = useState('ALL');
  const [purchaseStatus, setPurchaseStatus] = useState('');
  const [selectedSupplier, setSelectedSupplier] = useState('');
  const [movementType, setMovementType] = useState('');

  // Report Data States
  const [reportData, setReportData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorBanner, setErrorBanner] = useState(null);

  // Load auxiliary filter options (categories and suppliers)
  useEffect(() => {
    async function loadAuxOptions() {
      try {
        const [catsRes, suppsRes] = await Promise.allSettled([
          categoryApi.getAllCategories(),
          supplierApi.getActiveSuppliers()
        ]);
        if (catsRes.status === 'fulfilled' && Array.isArray(catsRes.value)) {
          setCategories(catsRes.value);
        }
        if (suppsRes.status === 'fulfilled' && Array.isArray(suppsRes.value)) {
          setSuppliers(suppsRes.value);
        }
      } catch (err) {
        console.error('Failed to load filter options:', err);
      }
    }
    loadAuxOptions();
  }, []);

  // Fetch report data
  const fetchReport = useCallback(async () => {
    setIsLoading(true);
    setErrorBanner(null);

    try {
      const baseParams = {
        page,
        size: pageSize
      };

      if (datePreset === 'custom') {
        if (startDate) baseParams.startDate = startDate;
        if (endDate) baseParams.endDate = endDate;
      } else {
        baseParams.days = parseInt(datePreset, 10) || 30;
      }

      if (appliedSearch) {
        baseParams.search = appliedSearch;
      }

      let data = null;

      switch (activeTab) {
        case 'inventory': {
          const invParams = {
            ...baseParams,
            status: inventoryStatus || 'ALL'
          };
          if (selectedCategory) invParams.categoryId = selectedCategory;
          data = await reportApi.getInventoryReport(invParams);
          break;
        }
        case 'sales': {
          data = await reportApi.getSalesReport(baseParams);
          break;
        }
        case 'purchases': {
          const purParams = { ...baseParams };
          if (purchaseStatus) purParams.status = purchaseStatus;
          if (selectedSupplier) purParams.supplierId = selectedSupplier;
          data = await reportApi.getPurchaseReport(purParams);
          break;
        }
        case 'movements': {
          const movParams = { ...baseParams };
          if (movementType) movParams.transactionType = movementType;
          data = await reportApi.getInventoryMovementReport(movParams);
          break;
        }
        case 'valuation': {
          const valParams = {
            page,
            size: pageSize
          };
          if (selectedCategory) valParams.categoryId = selectedCategory;
          data = await reportApi.getStockValuationReport(valParams);
          break;
        }
        default:
          break;
      }

      setReportData(data);
    } catch (err) {
      console.error('Error fetching report:', err);
      const msg = err.response?.data?.message || err.message || 'Failed to load report data.';
      setErrorBanner(msg);
      setReportData(null);
    } finally {
      setIsLoading(false);
    }
  }, [
    activeTab,
    page,
    pageSize,
    datePreset,
    startDate,
    endDate,
    appliedSearch,
    selectedCategory,
    inventoryStatus,
    purchaseStatus,
    selectedSupplier,
    movementType
  ]);

  useEffect(() => {
    fetchReport();
  }, [fetchReport]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setAppliedSearch(search.trim());
  };

  const handleClearSearch = () => {
    setSearch('');
    setAppliedSearch('');
    setPage(0);
  };

  const handleTabChange = (newTab) => {
    setActiveTab(newTab);
    setPage(0);
    setSearch('');
    setAppliedSearch('');
    setErrorBanner(null);
  };

  const formatCurrency = (val) => {
    if (val === null || val === undefined) return '₹0.00';
    const num = typeof val === 'number' ? val : parseFloat(val);
    if (isNaN(num)) return '₹0.00';
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(num);
  };

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });
    } catch {
      return isoString;
    }
  };

  const formatDateTime = (isoString) => {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-blue-50 text-blue-600 rounded-xl border border-blue-100 shadow-sm">
              <FileBarChart className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Business & Inventory Reports</h1>
              <p className="text-sm text-gray-500">
                Audited operational analytics, stock valuation breakdown, and verified transactional ledger.
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchReport}
            disabled={isLoading}
            className="flex items-center gap-2"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
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

      {/* Report Categories Tabs */}
      <div className="border-b border-gray-200">
        <nav className="flex space-x-2 overflow-x-auto pb-1" aria-label="Report Categories">
          {canAccessInventory && (
            <button
              onClick={() => handleTabChange('inventory')}
              className={`flex items-center gap-2 py-3 px-4 rounded-lg font-medium text-sm transition-all whitespace-nowrap ${
                activeTab === 'inventory'
                  ? 'bg-blue-50 text-blue-700 shadow-sm border border-blue-200'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
              }`}
            >
              <Package className="w-4 h-4" />
              Inventory Report
            </button>
          )}

          {canAccessSales && (
            <button
              onClick={() => handleTabChange('sales')}
              className={`flex items-center gap-2 py-3 px-4 rounded-lg font-medium text-sm transition-all whitespace-nowrap ${
                activeTab === 'sales'
                  ? 'bg-blue-50 text-blue-700 shadow-sm border border-blue-200'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
              }`}
            >
              <TrendingUp className="w-4 h-4" />
              Sales Report
            </button>
          )}

          {canAccessPurchases && (
            <button
              onClick={() => handleTabChange('purchases')}
              className={`flex items-center gap-2 py-3 px-4 rounded-lg font-medium text-sm transition-all whitespace-nowrap ${
                activeTab === 'purchases'
                  ? 'bg-blue-50 text-blue-700 shadow-sm border border-blue-200'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
              }`}
            >
              <ShoppingCart className="w-4 h-4" />
              Purchase Report
            </button>
          )}

          {canAccessMovements && (
            <button
              onClick={() => handleTabChange('movements')}
              className={`flex items-center gap-2 py-3 px-4 rounded-lg font-medium text-sm transition-all whitespace-nowrap ${
                activeTab === 'movements'
                  ? 'bg-blue-50 text-blue-700 shadow-sm border border-blue-200'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
              }`}
            >
              <ArrowRightLeft className="w-4 h-4" />
              Stock Movements
            </button>
          )}

          {canAccessValuation && (
            <button
              onClick={() => handleTabChange('valuation')}
              className={`flex items-center gap-2 py-3 px-4 rounded-lg font-medium text-sm transition-all whitespace-nowrap ${
                activeTab === 'valuation'
                  ? 'bg-blue-50 text-blue-700 shadow-sm border border-blue-200'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
              }`}
            >
              <DollarSign className="w-4 h-4" />
              Stock Valuation
            </button>
          )}
        </nav>
      </div>

      {/* Filter Toolbar Card */}
      <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          {/* Left: Date Presets & Date Pickers (for tabs with date ranges) */}
          {(activeTab === 'sales' || activeTab === 'purchases' || activeTab === 'movements') && (
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5" /> Date Range:
              </span>
              <div className="inline-flex rounded-lg border border-gray-200 p-0.5 bg-gray-50 text-xs">
                {['7', '30', '90', '180'].map((d) => (
                  <button
                    key={d}
                    type="button"
                    onClick={() => {
                      setDatePreset(d);
                      setPage(0);
                    }}
                    className={`px-2.5 py-1 rounded-md font-medium transition-all ${
                      datePreset === d
                        ? 'bg-white text-blue-600 shadow-sm font-semibold'
                        : 'text-gray-600 hover:text-gray-900'
                    }`}
                  >
                    Last {d}d
                  </button>
                ))}
                <button
                  type="button"
                  onClick={() => {
                    setDatePreset('custom');
                    setPage(0);
                  }}
                  className={`px-2.5 py-1 rounded-md font-medium transition-all ${
                    datePreset === 'custom'
                      ? 'bg-white text-blue-600 shadow-sm font-semibold'
                      : 'text-gray-600 hover:text-gray-900'
                  }`}
                >
                  Custom
                </button>
              </div>

              {datePreset === 'custom' && (
                <div className="flex items-center gap-2 text-xs">
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => {
                      setStartDate(e.target.value);
                      setPage(0);
                    }}
                    className="border border-gray-300 rounded-lg px-2.5 py-1 text-gray-700 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                  <span className="text-gray-400">to</span>
                  <input
                    type="date"
                    value={endDate}
                    onChange={(e) => {
                      setEndDate(e.target.value);
                      setPage(0);
                    }}
                    className="border border-gray-300 rounded-lg px-2.5 py-1 text-gray-700 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>
              )}
            </div>
          )}

          {/* Right: Search Box */}
          <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 ml-auto w-full sm:w-auto">
            <div className="relative flex-1 sm:w-64">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder={
                  activeTab === 'sales'
                    ? 'Search order or customer...'
                    : activeTab === 'purchases'
                    ? 'Search PO or supplier...'
                    : 'Search by name or SKU...'
                }
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full pl-9 pr-8 py-1.5 text-xs border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
              {search && (
                <button
                  type="button"
                  onClick={handleClearSearch}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-xs"
                >
                  ×
                </button>
              )}
            </div>
            <Button type="submit" variant="secondary" size="sm" className="text-xs px-3 py-1.5">
              Filter
            </Button>
          </form>
        </div>

        {/* Tab-Specific Secondary Filters */}
        <div className="flex flex-wrap items-center gap-3 pt-3 border-t border-gray-100 text-xs">
          <span className="text-gray-500 font-medium flex items-center gap-1">
            <Filter className="w-3.5 h-3.5" /> Filters:
          </span>

          {/* Category Filter for Inventory & Valuation */}
          {(activeTab === 'inventory' || activeTab === 'valuation') && (
            <div className="flex items-center gap-1.5">
              <label className="text-gray-600">Category:</label>
              <select
                value={selectedCategory}
                onChange={(e) => {
                  setSelectedCategory(e.target.value);
                  setPage(0);
                }}
                className="border border-gray-300 rounded-md px-2 py-1 text-xs bg-white text-gray-700 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="">All Categories</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Status Filter for Inventory */}
          {activeTab === 'inventory' && (
            <div className="flex items-center gap-1.5">
              <label className="text-gray-600">Stock Status:</label>
              <select
                value={inventoryStatus}
                onChange={(e) => {
                  setInventoryStatus(e.target.value);
                  setPage(0);
                }}
                className="border border-gray-300 rounded-md px-2 py-1 text-xs bg-white text-gray-700 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="ALL">All Stock Statuses</option>
                <option value="IN_STOCK">In Stock (Healthy)</option>
                <option value="LOW_STOCK">Low Stock (≤ Reorder)</option>
                <option value="OUT_OF_STOCK">Out of Stock (Zero)</option>
              </select>
            </div>
          )}

          {/* Status & Supplier Filter for Purchases */}
          {activeTab === 'purchases' && (
            <>
              <div className="flex items-center gap-1.5">
                <label className="text-gray-600">PO Status:</label>
                <select
                  value={purchaseStatus}
                  onChange={(e) => {
                    setPurchaseStatus(e.target.value);
                    setPage(0);
                  }}
                  className="border border-gray-300 rounded-md px-2 py-1 text-xs bg-white text-gray-700 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                >
                  <option value="">All Statuses</option>
                  <option value="RECEIVED">RECEIVED (Completed)</option>
                  <option value="ORDERED">ORDERED (In Transit)</option>
                  <option value="DRAFT">DRAFT</option>
                  <option value="CANCELLED">CANCELLED</option>
                </select>
              </div>

              <div className="flex items-center gap-1.5">
                <label className="text-gray-600">Supplier:</label>
                <select
                  value={selectedSupplier}
                  onChange={(e) => {
                    setSelectedSupplier(e.target.value);
                    setPage(0);
                  }}
                  className="border border-gray-300 rounded-md px-2 py-1 text-xs bg-white text-gray-700 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                >
                  <option value="">All Suppliers</option>
                  {suppliers.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>
            </>
          )}

          {/* Transaction Type Filter for Movements */}
          {activeTab === 'movements' && (
            <div className="flex items-center gap-1.5">
              <label className="text-gray-600">Transaction Type:</label>
              <select
                value={movementType}
                onChange={(e) => {
                  setMovementType(e.target.value);
                  setPage(0);
                }}
                className="border border-gray-300 rounded-md px-2 py-1 text-xs bg-white text-gray-700 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="">All Transaction Types</option>
                <option value="PURCHASE_RECEIPT">PURCHASE_RECEIPT (+ Stock Influx)</option>
                <option value="SALE_FULFILLMENT">SALE_FULFILLMENT (- Stock Depletion)</option>
                <option value="ADJUSTMENT">ADJUSTMENT</option>
                <option value="RETURN">RETURN</option>
              </select>
            </div>
          )}

          {/* Reset Filters button if any are applied */}
          {(appliedSearch || selectedCategory || (inventoryStatus && inventoryStatus !== 'ALL') || purchaseStatus || selectedSupplier || movementType) && (
            <button
              onClick={() => {
                setSearch('');
                setAppliedSearch('');
                setSelectedCategory('');
                setInventoryStatus('ALL');
                setPurchaseStatus('');
                setSelectedSupplier('');
                setMovementType('');
                setPage(0);
              }}
              className="text-blue-600 hover:text-blue-800 underline ml-auto text-xs"
            >
              Reset filters
            </button>
          )}
        </div>
      </div>

      {/* Main Content Area */}
      {isLoading ? (
        <div className="py-20 flex flex-col items-center justify-center">
          <LoadingSpinner size="lg" />
          <p className="mt-4 text-sm text-gray-500 font-medium">Aggregating database report metrics...</p>
        </div>
      ) : !reportData ? (
        <div className="text-center py-16 bg-white rounded-xl border border-gray-200">
          <AlertTriangle className="w-10 h-10 text-amber-500 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-gray-900">No report data returned</h3>
          <p className="text-sm text-gray-500 mt-1">Please try adjusting your filters or date range.</p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* ========================================================= */}
          {/* TAB 1: INVENTORY REPORT */}
          {/* ========================================================= */}
          {activeTab === 'inventory' && (
            <>
              {/* Summary KPI Cards */}
              <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Total Products</span>
                    <Package className="w-4 h-4 text-blue-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-gray-900">
                    {reportData.summary?.totalProducts || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Catalog SKUs</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Stock Units</span>
                    <Layers className="w-4 h-4 text-indigo-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-gray-900">
                    {reportData.summary?.totalStockUnits || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Total units on hand</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Healthy Stock</span>
                    <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-emerald-600">
                    {reportData.summary?.inStockCount || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">&gt; Reorder threshold</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Low Stock</span>
                    <AlertTriangle className="w-4 h-4 text-amber-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-amber-600">
                    {reportData.summary?.lowStockCount || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">≤ Reorder threshold</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Out of Stock</span>
                    <AlertTriangle className="w-4 h-4 text-red-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-red-600">
                    {reportData.summary?.outOfStockCount || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Immediate action needed</div>
                </div>
              </div>

              {/* Items Table */}
              <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                  <h3 className="font-semibold text-gray-900 text-sm">Product Inventory List</h3>
                  <span className="text-xs text-gray-500">
                    Showing {reportData.items?.content?.length || 0} of {reportData.items?.totalElements || 0} records
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-gray-50 text-gray-600 text-xs font-semibold uppercase border-b border-gray-200">
                      <tr>
                        <th className="px-6 py-3">Product Name & SKU</th>
                        <th className="px-6 py-3">Category</th>
                        <th className="px-6 py-3 text-right">Current Stock</th>
                        <th className="px-6 py-3 text-right">Reorder Level</th>
                        <th className="px-6 py-3 text-right">Selling Price</th>
                        <th className="px-6 py-3 text-center">Status</th>
                        <th className="px-6 py-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {reportData.items?.content?.length === 0 ? (
                        <tr>
                          <td colSpan="7" className="px-6 py-12 text-center text-gray-500">
                            No inventory items found matching current filters.
                          </td>
                        </tr>
                      ) : (
                        reportData.items?.content?.map((item) => (
                          <tr key={item.productId} className="hover:bg-gray-50 transition-colors">
                            <td className="px-6 py-3.5">
                              <div className="font-medium text-gray-900">{item.productName}</div>
                              <div className="text-xs font-mono text-gray-500">{item.sku}</div>
                            </td>
                            <td className="px-6 py-3.5 text-gray-600">
                              {item.categoryName || 'Uncategorized'}
                            </td>
                            <td className="px-6 py-3.5 text-right font-medium">
                              <span
                                className={
                                  item.currentStock === 0
                                    ? 'text-red-600 font-bold'
                                    : item.currentStock <= item.reorderLevel
                                    ? 'text-amber-600 font-semibold'
                                    : 'text-gray-900'
                                }
                              >
                                {item.currentStock}
                              </span>
                            </td>
                            <td className="px-6 py-3.5 text-right text-gray-500">
                              {item.reorderLevel}
                            </td>
                            <td className="px-6 py-3.5 text-right text-gray-900 font-medium">
                              {formatCurrency(item.price)}
                            </td>
                            <td className="px-6 py-3.5 text-center">
                              {item.stockStatus === 'OUT_OF_STOCK' ? (
                                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800 border border-red-200">
                                  Out of Stock
                                </span>
                              ) : item.stockStatus === 'LOW_STOCK' ? (
                                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200">
                                  Low Stock
                                </span>
                              ) : (
                                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
                                  In Stock
                                </span>
                              )}
                            </td>
                            <td className="px-6 py-3.5 text-right">
                              {item.stockStatus !== 'IN_STOCK' && canAccessPurchases ? (
                                <Button
                                  variant="secondary"
                                  size="xs"
                                  onClick={() => navigate('/purchases')}
                                  className="text-xs text-blue-600"
                                >
                                  Restock PO
                                </Button>
                              ) : (
                                <Button
                                  variant="ghost"
                                  size="xs"
                                  onClick={() => navigate('/products')}
                                  className="text-xs text-gray-500"
                                >
                                  View
                                </Button>
                              )}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination Controls */}
                {renderPagination(reportData.items)}
              </div>
            </>
          )}

          {/* ========================================================= */}
          {/* TAB 2: SALES REPORT */}
          {/* ========================================================= */}
          {activeTab === 'sales' && (
            <>
              {/* Summary Cards */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Completed Orders</span>
                    <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-gray-900">
                    {reportData.summary?.completedOrdersCount || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Excludes drafts & cancellations</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Units Sold</span>
                    <Package className="w-4 h-4 text-blue-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-gray-900">
                    {reportData.summary?.totalUnitsSold || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Total units fulfilled</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Net Sales Revenue</span>
                    <DollarSign className="w-4 h-4 text-emerald-600" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-emerald-600">
                    {formatCurrency(reportData.summary?.totalRevenue)}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Realized revenue in period</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Avg Order Value</span>
                    <TrendingUp className="w-4 h-4 text-indigo-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-indigo-600">
                    {formatCurrency(reportData.summary?.averageOrderValue)}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Revenue / Completed orders</div>
                </div>
              </div>

              {/* Top Selling Products Section */}
              {reportData.topSellingProducts?.length > 0 && (
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="font-semibold text-gray-900 text-sm flex items-center gap-2">
                      <Sparkles className="w-4 h-4 text-amber-500" /> Top-Selling Products in Selected Window
                    </h3>
                    <span className="text-xs text-gray-500">Ranked by volume sold</span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {reportData.topSellingProducts.slice(0, 6).map((top, idx) => (
                      <div
                        key={top.productId}
                        className="p-3.5 rounded-lg border border-gray-100 bg-gray-50 flex items-center justify-between"
                      >
                        <div className="space-y-0.5">
                          <div className="flex items-center gap-2">
                            <span className="w-5 h-5 rounded-full bg-blue-100 text-blue-700 text-xs font-bold flex items-center justify-center">
                              {idx + 1}
                            </span>
                            <span className="font-medium text-gray-900 text-sm truncate max-w-[160px]">
                              {top.productName}
                            </span>
                          </div>
                          <div className="text-xs font-mono text-gray-500 pl-7">{top.sku}</div>
                        </div>
                        <div className="text-right">
                          <div className="text-sm font-bold text-gray-900">{top.unitsSold} units</div>
                          <div className="text-xs text-emerald-600 font-medium">
                            {formatCurrency(top.totalRevenue)}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Completed Sales Orders Table */}
              <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                  <h3 className="font-semibold text-gray-900 text-sm">Completed Sales Orders</h3>
                  <span className="text-xs text-gray-500">
                    Showing {reportData.items?.content?.length || 0} of {reportData.items?.totalElements || 0} orders
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-gray-50 text-gray-600 text-xs font-semibold uppercase border-b border-gray-200">
                      <tr>
                        <th className="px-6 py-3">Order Number</th>
                        <th className="px-6 py-3">Completed Date</th>
                        <th className="px-6 py-3">Customer</th>
                        <th className="px-6 py-3 text-right">Units</th>
                        <th className="px-6 py-3 text-right">Order Revenue</th>
                        <th className="px-6 py-3 text-center">Status</th>
                        <th className="px-6 py-3 text-right">Link</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {reportData.items?.content?.length === 0 ? (
                        <tr>
                          <td colSpan="7" className="px-6 py-12 text-center text-gray-500">
                            No completed sales orders found in this period.
                          </td>
                        </tr>
                      ) : (
                        reportData.items?.content?.map((ord) => (
                          <tr key={ord.orderId} className="hover:bg-gray-50 transition-colors">
                            <td className="px-6 py-3.5 font-mono font-medium text-blue-600">
                              {ord.orderNumber}
                            </td>
                            <td className="px-6 py-3.5 text-gray-600 text-xs">
                              {formatDateTime(ord.completedAt)}
                            </td>
                            <td className="px-6 py-3.5">
                              <div className="font-medium text-gray-900">{ord.customerName || 'Walk-in'}</div>
                              {ord.customerContact && (
                                <div className="text-xs text-gray-400">{ord.customerContact}</div>
                              )}
                            </td>
                            <td className="px-6 py-3.5 text-right font-medium text-gray-900">
                              {ord.totalUnits}
                            </td>
                            <td className="px-6 py-3.5 text-right font-bold text-gray-900">
                              {formatCurrency(ord.totalAmount)}
                            </td>
                            <td className="px-6 py-3.5 text-center">
                              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                                COMPLETED
                              </span>
                            </td>
                            <td className="px-6 py-3.5 text-right">
                              <Button
                                variant="ghost"
                                size="xs"
                                onClick={() => navigate('/sales')}
                                className="text-xs text-gray-500"
                              >
                                View Order
                              </Button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {renderPagination(reportData.items)}
              </div>
            </>
          )}

          {/* ========================================================= */}
          {/* TAB 3: PURCHASE REPORT */}
          {/* ========================================================= */}
          {activeTab === 'purchases' && (
            <>
              {/* Summary Cards */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Total Orders</span>
                    <ShoppingCart className="w-4 h-4 text-blue-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-gray-900">
                    {reportData.summary?.totalOrdersCount || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">
                    Drafts: {reportData.summary?.draftOrdersCount || 0} | Cancelled: {reportData.summary?.cancelledOrdersCount || 0}
                  </div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Received Orders</span>
                    <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-emerald-600">
                    {reportData.summary?.receivedOrdersCount || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">
                    In transit / Pending: {reportData.summary?.orderedPendingCount || 0}
                  </div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Genuinely Received Units</span>
                    <Package className="w-4 h-4 text-indigo-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-indigo-600">
                    {reportData.summary?.receivedUnits || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Stock confirmed in ledger</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Actual Received Spend</span>
                    <DollarSign className="w-4 h-4 text-blue-600" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-blue-600">
                    {formatCurrency(reportData.summary?.receivedTotalCost)}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Received cost liability</div>
                </div>
              </div>

              {/* Supplier Summaries Table */}
              {reportData.supplierSummaries?.length > 0 && (
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                  <h3 className="font-semibold text-gray-900 text-sm mb-4 flex items-center gap-2">
                    <Building className="w-4 h-4 text-blue-600" /> Supplier-Wise Purchase Breakdown
                  </h3>
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                      <thead className="bg-gray-50 text-gray-600 text-xs font-semibold uppercase border-b border-gray-200">
                        <tr>
                          <th className="px-4 py-2.5">Supplier Name</th>
                          <th className="px-4 py-2.5 text-center">Total POs</th>
                          <th className="px-4 py-2.5 text-center">Received POs</th>
                          <th className="px-4 py-2.5 text-right">Total Spend on Received Stock</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-100">
                        {reportData.supplierSummaries.map((sup) => (
                          <tr key={sup.supplierId} className="hover:bg-gray-50">
                            <td className="px-4 py-2.5 font-medium text-gray-900">{sup.supplierName}</td>
                            <td className="px-4 py-2.5 text-center text-gray-600">{sup.totalOrders}</td>
                            <td className="px-4 py-2.5 text-center text-emerald-600 font-semibold">{sup.receivedOrders}</td>
                            <td className="px-4 py-2.5 text-right font-bold text-gray-900">
                              {formatCurrency(sup.totalSpent)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Purchase Orders Table */}
              <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                  <h3 className="font-semibold text-gray-900 text-sm">Purchase Orders Log</h3>
                  <span className="text-xs text-gray-500">
                    Showing {reportData.items?.content?.length || 0} of {reportData.items?.totalElements || 0} orders
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-gray-50 text-gray-600 text-xs font-semibold uppercase border-b border-gray-200">
                      <tr>
                        <th className="px-6 py-3">Order Number</th>
                        <th className="px-6 py-3">Supplier</th>
                        <th className="px-6 py-3">Status</th>
                        <th className="px-6 py-3">Ordered Date</th>
                        <th className="px-6 py-3">Received Date</th>
                        <th className="px-6 py-3 text-right">Units</th>
                        <th className="px-6 py-3 text-right">Order Cost</th>
                        <th className="px-6 py-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {reportData.items?.content?.length === 0 ? (
                        <tr>
                          <td colSpan="8" className="px-6 py-12 text-center text-gray-500">
                            No purchase orders found matching current criteria.
                          </td>
                        </tr>
                      ) : (
                        reportData.items?.content?.map((po) => (
                          <tr key={po.orderId} className="hover:bg-gray-50 transition-colors">
                            <td className="px-6 py-3.5 font-mono font-medium text-blue-600">
                              {po.orderNumber}
                            </td>
                            <td className="px-6 py-3.5 text-gray-900 font-medium">
                              {po.supplierName}
                            </td>
                            <td className="px-6 py-3.5">
                              <span
                                className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold ${
                                  po.status === 'RECEIVED'
                                    ? 'bg-emerald-100 text-emerald-800'
                                    : po.status === 'ORDERED'
                                    ? 'bg-blue-100 text-blue-800'
                                    : po.status === 'CANCELLED'
                                    ? 'bg-gray-100 text-gray-600'
                                    : 'bg-amber-100 text-amber-800'
                                }`}
                              >
                                {po.status}
                              </span>
                            </td>
                            <td className="px-6 py-3.5 text-xs text-gray-600">
                              {formatDateTime(po.orderedAt)}
                            </td>
                            <td className="px-6 py-3.5 text-xs text-gray-600">
                              {po.receivedAt ? formatDateTime(po.receivedAt) : '—'}
                            </td>
                            <td className="px-6 py-3.5 text-right font-medium text-gray-900">
                              {po.totalUnits}
                            </td>
                            <td className="px-6 py-3.5 text-right font-bold text-gray-900">
                              {formatCurrency(po.totalAmount)}
                            </td>
                            <td className="px-6 py-3.5 text-right">
                              <Button
                                variant="ghost"
                                size="xs"
                                onClick={() => navigate('/purchases')}
                                className="text-xs text-gray-500"
                              >
                                View PO
                              </Button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {renderPagination(reportData.items)}
              </div>
            </>
          )}

          {/* ========================================================= */}
          {/* TAB 4: INVENTORY MOVEMENTS REPORT */}
          {/* ========================================================= */}
          {activeTab === 'movements' && (
            <>
              {/* Summary Cards */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Total Movements</span>
                    <ArrowRightLeft className="w-4 h-4 text-blue-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-gray-900">
                    {reportData.summary?.totalMovements || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Total ledger events</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Inbound Units</span>
                    <ArrowDownLeft className="w-4 h-4 text-emerald-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-emerald-600">
                    +{reportData.summary?.totalInboundUnits || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">PO receipts & influx</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Outbound Units</span>
                    <ArrowUpRight className="w-4 h-4 text-red-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-red-600">
                    -{reportData.summary?.totalOutboundUnits || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Sales fulfillments</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Net Delta</span>
                    <Layers className="w-4 h-4 text-indigo-500" />
                  </div>
                  <div
                    className={`mt-2 text-2xl font-bold ${
                      (reportData.summary?.netQuantityChange || 0) >= 0
                        ? 'text-emerald-600'
                        : 'text-red-600'
                    }`}
                  >
                    {(reportData.summary?.netQuantityChange || 0) >= 0 ? '+' : ''}
                    {reportData.summary?.netQuantityChange || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Net inventory flow</div>
                </div>
              </div>

              {/* Transactions Ledger Table */}
              <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                  <h3 className="font-semibold text-gray-900 text-sm">Inventory Transaction Ledger</h3>
                  <span className="text-xs text-gray-500">
                    Showing {reportData.items?.content?.length || 0} of {reportData.items?.totalElements || 0} transactions
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-gray-50 text-gray-600 text-xs font-semibold uppercase border-b border-gray-200">
                      <tr>
                        <th className="px-6 py-3">Timestamp</th>
                        <th className="px-6 py-3">Transaction Type</th>
                        <th className="px-6 py-3">Product</th>
                        <th className="px-6 py-3 text-right">Delta</th>
                        <th className="px-6 py-3 text-center">Stock Change</th>
                        <th className="px-6 py-3">Reference</th>
                        <th className="px-6 py-3">Performed By</th>
                        <th className="px-6 py-3">Notes</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {reportData.items?.content?.length === 0 ? (
                        <tr>
                          <td colSpan="8" className="px-6 py-12 text-center text-gray-500">
                            No ledger movements found for the selected criteria.
                          </td>
                        </tr>
                      ) : (
                        reportData.items?.content?.map((tx) => (
                          <tr key={tx.id} className="hover:bg-gray-50 transition-colors">
                            <td className="px-6 py-3.5 text-xs text-gray-600 whitespace-nowrap">
                              {formatDateTime(tx.createdAt)}
                            </td>
                            <td className="px-6 py-3.5 whitespace-nowrap">
                              <span
                                className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-mono font-semibold ${
                                  tx.transactionType === 'PURCHASE_RECEIPT'
                                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                    : tx.transactionType === 'SALE_FULFILLMENT'
                                    ? 'bg-blue-50 text-blue-700 border border-blue-200'
                                    : 'bg-purple-50 text-purple-700 border border-purple-200'
                                }`}
                              >
                                {tx.transactionType}
                              </span>
                            </td>
                            <td className="px-6 py-3.5">
                              <div className="font-medium text-gray-900">{tx.productName}</div>
                              <div className="text-xs font-mono text-gray-500">{tx.sku}</div>
                            </td>
                            <td className="px-6 py-3.5 text-right font-bold">
                              <span
                                className={
                                  tx.quantityDelta > 0
                                    ? 'text-emerald-600'
                                    : tx.quantityDelta < 0
                                    ? 'text-red-600'
                                    : 'text-gray-500'
                                }
                              >
                                {tx.quantityDelta > 0 ? `+${tx.quantityDelta}` : tx.quantityDelta}
                              </span>
                            </td>
                            <td className="px-6 py-3.5 text-center text-xs text-gray-600 font-mono">
                              {tx.quantityBefore} →{' '}
                              <span className="font-bold text-gray-900">{tx.quantityAfter}</span>
                            </td>
                            <td className="px-6 py-3.5 text-xs text-gray-600 font-mono whitespace-nowrap">
                              {tx.referenceType} #{tx.referenceId}
                            </td>
                            <td className="px-6 py-3.5 text-xs text-gray-700 whitespace-nowrap">
                              <div className="flex items-center gap-1.5">
                                <User className="w-3.5 h-3.5 text-gray-400" />
                                {tx.performedBy || 'System'}
                              </div>
                            </td>
                            <td className="px-6 py-3.5 text-xs text-gray-500 max-w-xs truncate">
                              {tx.notes || '—'}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {renderPagination(reportData.items)}
              </div>
            </>
          )}

          {/* ========================================================= */}
          {/* TAB 5: STOCK VALUATION REPORT */}
          {/* ========================================================= */}
          {activeTab === 'valuation' && (
            <>
              {/* Methodological Explanation Notice */}
              <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-amber-900 text-sm flex items-start gap-3">
                <Info className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-semibold text-amber-800">
                    Purchase-Cost Valuation Unavailable
                  </h4>
                  <p className="mt-1 text-xs text-amber-700 leading-relaxed">
                    {reportData.explanation} In accordance with accounting standards, selling prices are not substituted for purchase costs. Instead, potential retail inventory value is displayed alongside quantities below.
                  </p>
                </div>
              </div>

              {/* Summary Cards */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Active Products</span>
                    <Package className="w-4 h-4 text-blue-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-gray-900">
                    {reportData.summary?.totalActiveProducts || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Catalog active items</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Total Units in Stock</span>
                    <Layers className="w-4 h-4 text-indigo-500" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-gray-900">
                    {reportData.summary?.totalStockUnits || 0}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Total physical inventory</div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase">
                    <span>Potential Retail Value</span>
                    <DollarSign className="w-4 h-4 text-emerald-600" />
                  </div>
                  <div className="mt-2 text-2xl font-bold text-emerald-600">
                    {formatCurrency(reportData.summary?.totalPotentialRetailValue)}
                  </div>
                  <div className="mt-1 text-xs text-gray-400">Sum of (Stock × Selling Price)</div>
                </div>
              </div>

              {/* Valuation Table */}
              <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                  <h3 className="font-semibold text-gray-900 text-sm">Product Valuation Ledger</h3>
                  <span className="text-xs text-gray-500">
                    Showing {reportData.items?.content?.length || 0} of {reportData.items?.totalElements || 0} items
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-gray-50 text-gray-600 text-xs font-semibold uppercase border-b border-gray-200">
                      <tr>
                        <th className="px-6 py-3">Product & SKU</th>
                        <th className="px-6 py-3">Category</th>
                        <th className="px-6 py-3 text-right">Units on Hand</th>
                        <th className="px-6 py-3 text-right">Selling Price</th>
                        <th className="px-6 py-3 text-right">Potential Retail Value</th>
                        <th className="px-6 py-3 text-center">Unit Cost</th>
                        <th className="px-6 py-3 text-center">Cost Valuation</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {reportData.items?.content?.length === 0 ? (
                        <tr>
                          <td colSpan="7" className="px-6 py-12 text-center text-gray-500">
                            No products found matching criteria.
                          </td>
                        </tr>
                      ) : (
                        reportData.items?.content?.map((item) => (
                          <tr key={item.productId} className="hover:bg-gray-50 transition-colors">
                            <td className="px-6 py-3.5">
                              <div className="font-medium text-gray-900">{item.productName}</div>
                              <div className="text-xs font-mono text-gray-500">{item.sku}</div>
                            </td>
                            <td className="px-6 py-3.5 text-gray-600">
                              {item.categoryName || 'Uncategorized'}
                            </td>
                            <td className="px-6 py-3.5 text-right font-medium text-gray-900">
                              {item.currentStock}
                            </td>
                            <td className="px-6 py-3.5 text-right font-medium text-gray-700">
                              {formatCurrency(item.retailPrice)}
                            </td>
                            <td className="px-6 py-3.5 text-right font-bold text-emerald-700">
                              {formatCurrency(item.potentialRetailValue)}
                            </td>
                            <td className="px-6 py-3.5 text-center text-xs text-gray-400 italic">
                              Unavailable
                            </td>
                            <td className="px-6 py-3.5 text-center text-xs text-gray-400 italic">
                              Unavailable
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {renderPagination(reportData.items)}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );

  // Helper Pagination component
  function renderPagination(pageObj) {
    if (!pageObj || pageObj.totalPages <= 1) return null;

    return (
      <div className="px-6 py-3 border-t border-gray-200 flex items-center justify-between bg-gray-50 text-xs text-gray-600">
        <div>
          Page <span className="font-semibold text-gray-900">{pageObj.pageNumber + 1}</span> of{' '}
          <span className="font-semibold text-gray-900">{pageObj.totalPages}</span> ({pageObj.totalElements} items)
        </div>
        <div className="flex items-center gap-1.5">
          <Button
            variant="outline"
            size="xs"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={pageObj.isFirst}
            className="flex items-center gap-1"
          >
            <ChevronLeft className="w-3.5 h-3.5" /> Previous
          </Button>
          <Button
            variant="outline"
            size="xs"
            onClick={() => setPage((p) => p + 1)}
            disabled={pageObj.isLast}
            className="flex items-center gap-1"
          >
            Next <ChevronRight className="w-3.5 h-3.5" />
          </Button>
        </div>
      </div>
    );
  }
}
