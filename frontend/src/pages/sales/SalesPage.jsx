import React, { useState, useEffect, useCallback } from 'react';
import { saleApi } from '../../api/saleApi';
import { useAuth } from '../../context/AuthContext';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import SaleOrderModal from './SaleOrderModal';
import SaleDetailsModal from './SaleDetailsModal';
import {
  TrendingUp,
  Plus,
  Search,
  Eye,
  Edit,
  Trash2,
  RefreshCw,
  CheckCircle2,
  XCircle,
  Clock,
  IndianRupee,
  User,
  ShoppingBag,
  Calendar,
  X,
  PieChart as PieChartIcon,
  BarChart3,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend
} from 'recharts';
import ChartCard from '../../components/charts/ChartCard';
import CustomTooltip from '../../components/charts/CustomTooltip';

export default function SalesPage() {
  const { user } = useAuth();
  const userRole = user?.role;

  const canManageSales = userRole === 'ADMIN' || userRole === 'SALES_STAFF';

  const [sales, setSales] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);

  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const [isLoading, setIsLoading] = useState(true);
  const [errorBanner, setErrorBanner] = useState(null);
  const [successBanner, setSuccessBanner] = useState(null);

  // Form Modal state
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [editingSale, setEditingSale] = useState(null);
  const [isSubmittingForm, setIsSubmittingForm] = useState(false);
  const [backendFormErrors, setBackendFormErrors] = useState(null);

  // Details Modal state
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [selectedSaleDetails, setSelectedSaleDetails] = useState(null);
  const [isProcessingAction, setIsProcessingAction] = useState(false);

  // Fetch sales
  const fetchSales = useCallback(async () => {
    setIsLoading(true);
    setErrorBanner(null);
    try {
      const params = {
        page,
        size: pageSize,
        sort: 'createdAt,desc'
      };
      if (appliedSearch) params.search = appliedSearch;
      if (statusFilter) params.status = statusFilter;
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;

      const data = await saleApi.getSales(params);
      setSales(data.content || []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 0);
    } catch (err) {
      console.error('Failed to fetch sales:', err);
      setErrorBanner(err.response?.data?.message || 'Failed to load sales orders.');
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, appliedSearch, statusFilter, startDate, endDate]);

  useEffect(() => {
    fetchSales();
  }, [fetchSales]);

  const [showCharts, setShowCharts] = useState(true);

  // KPI & Analytics Calculations
  const completedSalesCount = sales.filter((s) => s.status === 'COMPLETED').length;
  const draftSalesCount = sales.filter((s) => s.status === 'DRAFT').length;
  const cancelledSalesCount = sales.filter((s) => s.status === 'CANCELLED').length;
  const completedRevenue = sales
    .filter((s) => s.status === 'COMPLETED')
    .reduce((sum, s) => sum + (Number(s.totalAmount) || 0), 0);
  const draftRevenue = sales
    .filter((s) => s.status === 'DRAFT')
    .reduce((sum, s) => sum + (Number(s.totalAmount) || 0), 0);

  const statusDistributionData = [
    { name: 'Completed', value: completedSalesCount, color: '#10b981' },
    { name: 'Draft', value: draftSalesCount, color: '#f59e0b' },
    { name: 'Cancelled', value: cancelledSalesCount, color: '#ef4444' }
  ].filter(d => d.value > 0);

  const salesVolumeData = [
    { name: 'Completed', amount: completedRevenue, count: completedSalesCount, fill: '#10b981' },
    { name: 'Draft', amount: draftRevenue, count: draftSalesCount, fill: '#f59e0b' }
  ];

  // Search handler
  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setAppliedSearch(search.trim());
  };

  const handleClearFilters = () => {
    setSearch('');
    setAppliedSearch('');
    setStatusFilter('');
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  // Open Create Modal
  const handleOpenCreateModal = () => {
    setEditingSale(null);
    setBackendFormErrors(null);
    setIsFormModalOpen(true);
  };

  // Open Edit Modal
  const handleOpenEditModal = async (saleSummary) => {
    try {
      const fullSale = await saleApi.getSaleById(saleSummary.id);
      setEditingSale(fullSale);
      setBackendFormErrors(null);
      setIsFormModalOpen(true);
    } catch (err) {
      setErrorBanner(err.response?.data?.message || 'Failed to load sale details for editing.');
    }
  };

  // Open Details Modal
  const handleOpenDetailsModal = async (saleSummary) => {
    try {
      const fullSale = await saleApi.getSaleById(saleSummary.id);
      setSelectedSaleDetails(fullSale);
      setIsDetailsModalOpen(true);
    } catch (err) {
      setErrorBanner(err.response?.data?.message || 'Failed to load sale details.');
    }
  };

  // Create or Update Submit
  const handleFormSubmit = async (payload) => {
    setIsSubmittingForm(true);
    setBackendFormErrors(null);
    try {
      if (editingSale) {
        await saleApi.updateSale(editingSale.id, payload);
        setSuccessBanner(`Sale order ${editingSale.orderNumber} updated successfully.`);
      } else {
        const created = await saleApi.createSale(payload);
        setSuccessBanner(`Draft sale order ${created.orderNumber} created successfully.`);
      }
      setIsFormModalOpen(false);
      setEditingSale(null);
      fetchSales();
    } catch (err) {
      console.error('Save sale failed:', err);
      const res = err.response?.data;
      if (res?.validationErrors) {
        const firstErr = Object.values(res.validationErrors)[0];
        setBackendFormErrors(firstErr || res.message);
      } else {
        setBackendFormErrors(res?.message || 'Failed to save sales order.');
      }
    } finally {
      setIsSubmittingForm(false);
    }
  };

  // Complete Sale
  const handleCompleteSale = async (id) => {
    setIsProcessingAction(true);
    try {
      const completed = await saleApi.completeSale(id);
      setSuccessBanner(`Sale ${completed.orderNumber} completed and stock deducted successfully!`);
      setIsDetailsModalOpen(false);
      fetchSales();
    } catch (err) {
      setErrorBanner(err.response?.data?.message || 'Failed to complete sales order.');
    } finally {
      setIsProcessingAction(false);
    }
  };

  // Cancel Sale
  const handleCancelSale = async (id) => {
    setIsProcessingAction(true);
    try {
      const cancelled = await saleApi.cancelSale(id);
      setSuccessBanner(`Sale ${cancelled.orderNumber} has been cancelled.`);
      setIsDetailsModalOpen(false);
      fetchSales();
    } catch (err) {
      setErrorBanner(err.response?.data?.message || 'Failed to cancel sales order.');
    } finally {
      setIsProcessingAction(false);
    }
  };

  // Delete Draft
  const handleDeleteSale = async (id) => {
    setIsProcessingAction(true);
    try {
      await saleApi.deleteSale(id);
      setSuccessBanner('Draft sales order deleted successfully.');
      setIsDetailsModalOpen(false);
      fetchSales();
    } catch (err) {
      setErrorBanner(err.response?.data?.message || 'Failed to delete draft sales order.');
    } finally {
      setIsProcessingAction(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'DRAFT':
        return <Badge variant="warning">Draft</Badge>;
      case 'COMPLETED':
        return <Badge variant="success">Completed</Badge>;
      case 'CANCELLED':
        return <Badge variant="neutral">Cancelled</Badge>;
      default:
        return <Badge variant="neutral">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100 flex items-center gap-2.5">
            <TrendingUp className="w-6 h-6 text-emerald-600 dark:text-emerald-400" />
            Sales Orders
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Manage customer sales, draft estimates, and atomic inventory stock deduction.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowCharts((prev) => !prev)}
            className="flex items-center gap-1.5 text-xs text-slate-600 dark:text-slate-300"
            title="Toggle Visual Charts"
          >
            <PieChartIcon className="w-3.5 h-3.5 text-emerald-600" />
            {showCharts ? 'Hide Charts' : 'Show Charts'}
          </Button>

          <Button
            variant="outline"
            size="sm"
            onClick={fetchSales}
            disabled={isLoading}
            className="flex items-center gap-1.5 text-xs text-slate-600 dark:text-slate-300"
            title="Refresh sales list"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
          </Button>

          {canManageSales && (
            <Button
              size="sm"
              onClick={handleOpenCreateModal}
              className="bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-1.5 shadow-md shadow-emerald-500/20 text-xs font-semibold"
            >
              <Plus className="w-4 h-4" />
              New Sale Order
            </Button>
          )}
        </div>
      </div>

      {/* Alert Banners */}
      {successBanner && (
        <AlertBanner
          type="success"
          message={successBanner}
          onClose={() => setSuccessBanner(null)}
        />
      )}
      {errorBanner && (
        <AlertBanner
          type="error"
          message={errorBanner}
          onClose={() => setErrorBanner(null)}
        />
      )}

      {/* KPI Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-xl shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500">Total Orders</span>
            <div className="p-2 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-lg">
              <ShoppingBag className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-2">
            {totalElements}
          </p>
          <span className="text-[11px] text-slate-400">All recorded orders</span>
        </div>

        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-xl shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500">Completed Sales</span>
            <div className="p-2 bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 rounded-lg">
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-2">
            {completedSalesCount}
          </p>
          <span className="text-[11px] text-slate-400">Inventory deducted</span>
        </div>

        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-xl shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500">Draft Orders</span>
            <div className="p-2 bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400 rounded-lg">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-2">
            {draftSalesCount}
          </p>
          <span className="text-[11px] text-slate-400">Pending completion</span>
        </div>

        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-xl shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500">Page Revenue</span>
            <div className="p-2 bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400 rounded-lg">
              <IndianRupee className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-2">
            ₹{completedRevenue.toFixed(2)}
          </p>
          <span className="text-[11px] text-slate-400">Completed on current page</span>
        </div>
      </div>

      {/* Visual Analytics Charts Section */}
      {showCharts && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <ChartCard
            title="Sales Status Breakdown"
            subtitle={`${sales.length} orders shown on current page`}
            icon={PieChartIcon}
            iconColor="#10b981"
          >
            {statusDistributionData.length === 0 ? (
              <div className="flex h-full items-center justify-center text-xs text-slate-400">
                No orders available for chart
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={230}>
                <PieChart>
                  <Tooltip content={<CustomTooltip valueSuffix=" orders" />} />
                  <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{ fontSize: '12px' }} />
                  <Pie
                    data={statusDistributionData}
                    cx="50%"
                    cy="45%"
                    innerRadius={50}
                    outerRadius={75}
                    paddingAngle={4}
                    dataKey="value"
                  >
                    {statusDistributionData.map((entry, index) => (
                      <Cell key={`cell-status-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            )}
          </ChartCard>

          <ChartCard
            title="Order Revenue by Status"
            subtitle="Comparing completed revenue vs pending draft value"
            icon={BarChart3}
            iconColor="#3b82f6"
          >
            <ResponsiveContainer width="100%" height={230}>
              <BarChart data={salesVolumeData} margin={{ top: 10, right: 10, left: 10, bottom: 20 }}>
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis
                  tick={{ fontSize: 10 }}
                  tickFormatter={(val) => `₹${val >= 1000 ? `${(val / 1000).toFixed(0)}k` : val}`}
                />
                <Tooltip
                  content={
                    <CustomTooltip
                      formatter={(val) => `₹${Number(val).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`}
                    />
                  }
                />
                <Bar dataKey="amount" radius={[6, 6, 0, 0]}>
                  {salesVolumeData.map((entry, index) => (
                    <Cell key={`cell-vol-${index}`} fill={entry.fill} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>
      )}

      {/* Filter and Search Controls */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow-xs space-y-3">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 md:grid-cols-12 gap-3">
          {/* Search text */}
          <div className="md:col-span-5 relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by order #, customer name, contact..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full text-xs rounded-lg pl-9 pr-3 py-2 border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          {/* Status Filter */}
          <div className="md:col-span-3">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="w-full text-xs rounded-lg px-3 py-2 border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              <option value="">All Statuses</option>
              <option value="DRAFT">Draft</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>

          {/* Date range */}
          <div className="md:col-span-2">
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setPage(0);
              }}
              className="w-full text-xs rounded-lg px-2.5 py-2 border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              title="Start Date"
            />
          </div>

          <div className="md:col-span-2 flex items-center gap-2">
            <input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setPage(0);
              }}
              className="w-full text-xs rounded-lg px-2.5 py-2 border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              title="End Date"
            />
            {(appliedSearch || statusFilter || startDate || endDate) && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleClearFilters}
                className="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200"
                title="Clear all filters"
              >
                <X className="w-3.5 h-3.5" />
              </Button>
            )}
          </div>
        </form>
      </div>

      {/* Main Table */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-xs">
        {isLoading ? (
          <div className="p-12 flex justify-center">
            <LoadingSpinner message="Loading sales orders..." />
          </div>
        ) : sales.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-400 flex items-center justify-center mx-auto">
              <TrendingUp className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
              No sales orders found
            </h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              {appliedSearch || statusFilter || startDate || endDate
                ? 'No sales matched your filters. Try clearing filters or search terms.'
                : 'Get started by creating your first sales order to deduct stock.'}
            </p>
            {canManageSales && (
              <Button
                size="sm"
                onClick={handleOpenCreateModal}
                className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold"
              >
                <Plus className="w-3.5 h-3.5 mr-1" /> Create Sale Order
              </Button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 font-semibold uppercase tracking-wider border-b border-slate-200 dark:border-slate-800">
                <tr>
                  <th className="py-3 px-4">Order Number</th>
                  <th className="py-3 px-4">Customer</th>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4 text-center">Items</th>
                  <th className="py-3 px-4 text-right">Total Amount</th>
                  <th className="py-3 px-4 text-center">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60 text-slate-700 dark:text-slate-300">
                {sales.map((order) => {
                  const isDraft = order.status === 'DRAFT';

                  return (
                    <tr
                      key={order.id}
                      className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors"
                    >
                      {/* Order Number */}
                      <td className="py-3 px-4 font-mono font-semibold text-slate-900 dark:text-slate-100">
                        {order.orderNumber}
                      </td>

                      {/* Customer */}
                      <td className="py-3 px-4">
                        <div className="font-medium text-slate-900 dark:text-slate-100 flex items-center gap-1.5">
                          <User className="w-3.5 h-3.5 text-slate-400" />
                          {order.customerName || 'Walk-in Customer'}
                        </div>
                        {order.customerContact && (
                          <div className="text-[11px] text-slate-500">
                            {order.customerContact}
                          </div>
                        )}
                      </td>

                      {/* Date */}
                      <td className="py-3 px-4 text-slate-600 dark:text-slate-400">
                        {order.createdAt ? new Date(order.createdAt).toLocaleDateString() : '—'}
                      </td>

                      {/* Total Items */}
                      <td className="py-3 px-4 text-center font-medium">
                        {order.totalItems || 0}
                      </td>

                      {/* Total Amount */}
                      <td className="py-3 px-4 text-right font-bold text-slate-900 dark:text-slate-100">
                        ₹{Number(order.totalAmount).toFixed(2)}
                      </td>

                      {/* Status */}
                      <td className="py-3 px-4 text-center">
                        {getStatusBadge(order.status)}
                      </td>

                      {/* Actions */}
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* View details */}
                          <button
                            type="button"
                            onClick={() => handleOpenDetailsModal(order)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                            title="View sale details"
                          >
                            <Eye className="w-4 h-4" />
                          </button>

                          {/* Edit (Draft & canManage) */}
                          {isDraft && canManageSales && (
                            <button
                              type="button"
                              onClick={() => handleOpenEditModal(order)}
                              className="p-1.5 rounded-lg text-amber-600 hover:bg-amber-50 dark:hover:bg-amber-950/40 transition-colors"
                              title="Edit draft order"
                            >
                              <Edit className="w-4 h-4" />
                            </button>
                          )}

                          {/* Complete (Draft & canManage) */}
                          {isDraft && canManageSales && (
                            <button
                              type="button"
                              onClick={() => handleCompleteSale(order.id)}
                              className="p-1.5 rounded-lg text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/40 transition-colors"
                              title="Complete sale and deduct stock"
                            >
                              <CheckCircle2 className="w-4 h-4" />
                            </button>
                          )}

                          {/* Cancel (Draft & canManage) */}
                          {isDraft && canManageSales && (
                            <button
                              type="button"
                              onClick={() => handleCancelSale(order.id)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                              title="Cancel order"
                            >
                              <XCircle className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="px-4 py-3 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between text-xs text-slate-600 dark:text-slate-400">
            <span>
              Showing {page * pageSize + 1}–{Math.min((page + 1) * pageSize, totalElements)} of {totalElements}
            </span>
            <div className="flex items-center gap-1.5">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </Button>
              <span className="px-2">
                Page {page + 1} of {totalPages}
              </span>
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

      {/* Sale Order Form Modal (Create / Edit) */}
      <SaleOrderModal
        isOpen={isFormModalOpen}
        onClose={() => {
          setIsFormModalOpen(false);
          setEditingSale(null);
        }}
        onSubmit={handleFormSubmit}
        initialData={editingSale}
        isSubmitting={isSubmittingForm}
        backendErrors={backendFormErrors}
      />

      {/* Sale Details Modal */}
      <SaleDetailsModal
        isOpen={isDetailsModalOpen}
        onClose={() => {
          setIsDetailsModalOpen(false);
          setSelectedSaleDetails(null);
        }}
        sale={selectedSaleDetails}
        onEdit={(sale) => {
          setEditingSale(sale);
          setIsFormModalOpen(true);
        }}
        onComplete={handleCompleteSale}
        onCancel={handleCancelSale}
        onDelete={handleDeleteSale}
        isProcessing={isProcessingAction}
      />
    </div>
  );
}
