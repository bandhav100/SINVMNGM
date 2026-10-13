import React, { useState, useEffect, useCallback } from 'react';
import { purchaseApi } from '../../api/purchaseApi';
import { supplierApi } from '../../api/supplierApi';
import { useAuth } from '../../context/AuthContext';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import PurchaseOrderModal from './PurchaseOrderModal';
import PurchaseDetailsModal from './PurchaseDetailsModal';
import {
  ShoppingCart,
  Plus,
  Search,
  Eye,
  Edit,
  Trash2,
  RefreshCw,
  PackageCheck,
  Clock,
  CheckCircle2,
  XCircle,
  FileText,
  IndianRupee,
  Truck,
  Calendar,
  X,
  PieChart as PieChartIcon,
  BarChart3,
  Building
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

export default function PurchasesPage() {
  const { user } = useAuth();
  const userRole = user?.role;

  const canManagePurchases = userRole === 'ADMIN' || userRole === 'PURCHASE_MANAGER';
  const canReceiveGoods = userRole === 'ADMIN' || userRole === 'PURCHASE_MANAGER' || userRole === 'INVENTORY_MANAGER';

  const [purchases, setPurchases] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);

  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [supplierFilter, setSupplierFilter] = useState('');
  const [suppliersList, setSuppliersList] = useState([]);

  const [isLoading, setIsLoading] = useState(true);
  const [errorBanner, setErrorBanner] = useState(null);
  const [successBanner, setSuccessBanner] = useState(null);

  // Form Modal state
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [editingOrder, setEditingOrder] = useState(null);
  const [isSubmittingForm, setIsSubmittingForm] = useState(false);
  const [backendFormErrors, setBackendFormErrors] = useState(null);

  // Details Modal state
  const [selectedOrderId, setSelectedOrderId] = useState(null);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);

  // Delete Modal state
  const [orderToDelete, setOrderToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Quick Receive Modal state
  const [orderToReceive, setOrderToReceive] = useState(null);
  const [isReceiving, setIsReceiving] = useState(false);

  // Load suppliers for filter dropdown
  useEffect(() => {
    supplierApi.getSuppliers({ size: 100 })
      .then((res) => setSuppliersList(res.content || []))
      .catch((err) => console.error('Failed to load suppliers for filter:', err));
  }, []);

  const fetchPurchases = useCallback(async (
    targetPage = page,
    searchTerm = appliedSearch,
    status = statusFilter,
    supplierId = supplierFilter
  ) => {
    setIsLoading(true);
    setErrorBanner(null);
    try {
      const response = await purchaseApi.getPurchases({
        page: targetPage,
        size: pageSize,
        search: searchTerm || null,
        status: status || null,
        supplierId: supplierId || null,
        sort: 'id,desc'
      });
      setPurchases(response.content || []);
      setTotalElements(response.totalElements || 0);
      setTotalPages(response.totalPages || 0);
      setPage(response.pageNumber || 0);
    } catch (err) {
      setErrorBanner(err.response?.data?.message || 'Failed to fetch purchase orders');
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, appliedSearch, statusFilter, supplierFilter]);

  useEffect(() => {
    fetchPurchases(page, appliedSearch, statusFilter, supplierFilter);
  }, [page, appliedSearch, statusFilter, supplierFilter, fetchPurchases]);

  const [showCharts, setShowCharts] = useState(true);

  // Chart dataset calculations
  const receivedPurchasesCount = purchases.filter((p) => p.status === 'RECEIVED').length;
  const orderedPurchasesCount = purchases.filter((p) => p.status === 'ORDERED').length;
  const cancelledPurchasesCount = purchases.filter((p) => p.status === 'CANCELLED').length;
  const draftPurchasesCount = purchases.filter((p) => p.status === 'DRAFT').length;

  const purchaseStatusDistribution = [
    { name: 'Received', value: receivedPurchasesCount, color: '#10b981' },
    { name: 'Ordered / Pending', value: orderedPurchasesCount, color: '#f59e0b' },
    { name: 'Draft', value: draftPurchasesCount, color: '#3b82f6' },
    { name: 'Cancelled', value: cancelledPurchasesCount, color: '#ef4444' }
  ].filter((d) => d.value > 0);

  const supplierSpendMap = {};
  purchases.forEach((p) => {
    const sName = p.supplierName || 'Other Vendor';
    const amount = parseFloat(p.totalAmount) || 0;
    supplierSpendMap[sName] = (supplierSpendMap[sName] || 0) + amount;
  });
  const supplierSpendData = Object.entries(supplierSpendMap)
    .map(([name, amount]) => ({
      name: name.length > 14 ? `${name.substring(0, 12)}...` : name,
      fullName: name,
      amount
    }))
    .sort((a, b) => b.amount - a.amount)
    .slice(0, 6);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setAppliedSearch(search);
    setPage(0);
  };

  const handleClearFilters = () => {
    setSearch('');
    setAppliedSearch('');
    setStatusFilter('');
    setSupplierFilter('');
    setPage(0);
  };

  const handleCreateOpen = () => {
    setEditingOrder(null);
    setBackendFormErrors(null);
    setIsFormModalOpen(true);
  };

  const handleEditOpen = async (orderSummary) => {
    try {
      const fullOrder = await purchaseApi.getPurchaseById(orderSummary.id);
      setEditingOrder(fullOrder);
      setBackendFormErrors(null);
      setIsFormModalOpen(true);
    } catch (err) {
      setErrorBanner('Failed to load purchase order for editing');
    }
  };

  const handleFormSubmit = async (formData) => {
    setIsSubmittingForm(true);
    setBackendFormErrors(null);
    try {
      if (editingOrder) {
        await purchaseApi.updatePurchase(editingOrder.id, formData);
        setSuccessBanner(`Purchase order ${editingOrder.orderNumber} updated successfully.`);
      } else {
        const created = await purchaseApi.createPurchase(formData);
        setSuccessBanner(`Purchase order ${created.orderNumber} created successfully.`);
      }
      setIsFormModalOpen(false);
      setEditingOrder(null);
      fetchPurchases(page, appliedSearch, statusFilter, supplierFilter);
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.response?.data?.validationErrors || 'Operation failed';
      setBackendFormErrors(errorMsg);
    } finally {
      setIsSubmittingForm(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!orderToDelete) return;
    setIsDeleting(true);
    try {
      await purchaseApi.deletePurchase(orderToDelete.id);
      setSuccessBanner(`Draft order ${orderToDelete.orderNumber} deleted.`);
      setOrderToDelete(null);
      fetchPurchases(page, appliedSearch, statusFilter, supplierFilter);
    } catch (err) {
      setErrorBanner(err.response?.data?.message || 'Failed to delete draft order');
      setOrderToDelete(null);
    } finally {
      setIsDeleting(false);
    }
  };

  const handleConfirmReceive = async () => {
    if (!orderToReceive) return;
    setIsReceiving(true);
    try {
      await purchaseApi.receivePurchase(orderToReceive.id);
      setSuccessBanner(`Shipment for ${orderToReceive.orderNumber} received. Stock has been incremented.`);
      setOrderToReceive(null);
      fetchPurchases(page, appliedSearch, statusFilter, supplierFilter);
    } catch (err) {
      setErrorBanner(err.response?.data?.message || 'Failed to receive goods');
      setOrderToReceive(null);
    } finally {
      setIsReceiving(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'RECEIVED':
        return <Badge variant="success">RECEIVED</Badge>;
      case 'ORDERED':
        return <Badge variant="warning">ORDERED</Badge>;
      case 'DRAFT':
        return <Badge variant="neutral">DRAFT</Badge>;
      case 'CANCELLED':
        return <Badge variant="danger">CANCELLED</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  return (
    <div className="page-container">
      {/* Page Header */}
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
        <div>
          <h1 className="page-title" style={{ fontSize: '1.75rem', fontWeight: 700, margin: 0, display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <ShoppingCart size={28} style={{ color: 'var(--color-primary)' }} />
            Purchase Management
          </h1>
          <p className="page-subtitle" style={{ color: 'var(--color-text-muted)', fontSize: '0.875rem', margin: '0.25rem 0 0 0' }}>
            Procurement workflow, vendor orders, and automated inventory receiving
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Button
            variant="outline"
            icon={PieChartIcon}
            onClick={() => setShowCharts((prev) => !prev)}
          >
            {showCharts ? 'Hide Charts' : 'Show Charts'}
          </Button>
          <Button
            variant="secondary"
            icon={RefreshCw}
            onClick={() => fetchPurchases(page, appliedSearch, statusFilter, supplierFilter)}
            disabled={isLoading}
          >
            Refresh
          </Button>
          {canManagePurchases && (
            <Button variant="primary" icon={Plus} onClick={handleCreateOpen}>
              Create Purchase Order
            </Button>
          )}
        </div>
      </div>

      {/* Global Alerts */}
      {errorBanner && (
        <div style={{ marginBottom: '1rem' }}>
          <AlertBanner type="error" message={errorBanner} onClose={() => setErrorBanner(null)} />
        </div>
      )}
      {successBanner && (
        <div style={{ marginBottom: '1rem' }}>
          <AlertBanner type="success" message={successBanner} onClose={() => setSuccessBanner(null)} />
        </div>
      )}

      {/* Metrics Row */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1rem',
          marginBottom: '1.5rem'
        }}
      >
        <div className="metric-card" style={{ padding: '1.25rem', backgroundColor: 'var(--color-card-bg)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', backgroundColor: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6' }}>
            <FileText size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontWeight: 500, display: 'block' }}>Total Orders</span>
            <span style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--color-text-main)' }}>{totalElements}</span>
          </div>
        </div>

        <div className="metric-card" style={{ padding: '1.25rem', backgroundColor: 'var(--color-card-bg)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', backgroundColor: 'rgba(245, 158, 11, 0.1)', color: '#f59e0b' }}>
            <Clock size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontWeight: 500, display: 'block' }}>Pending Orders</span>
            <span style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--color-text-main)' }}>
              {purchases.filter((p) => p.status === 'ORDERED').length}
            </span>
          </div>
        </div>

        <div className="metric-card" style={{ padding: '1.25rem', backgroundColor: 'var(--color-card-bg)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', backgroundColor: 'rgba(16, 185, 129, 0.1)', color: '#10b981' }}>
            <PackageCheck size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontWeight: 500, display: 'block' }}>Received Orders</span>
            <span style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--color-text-main)' }}>
              {purchases.filter((p) => p.status === 'RECEIVED').length}
            </span>
          </div>
        </div>

        <div className="metric-card" style={{ padding: '1.25rem', backgroundColor: 'var(--color-card-bg)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', backgroundColor: 'rgba(99, 102, 241, 0.1)', color: '#6366f1' }}>
            <IndianRupee size={22} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontWeight: 500, display: 'block' }}>Total Page Value</span>
            <span style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--color-text-main)' }}>
              ₹{purchases.reduce((acc, p) => acc + (parseFloat(p.totalAmount) || 0), 0).toFixed(2)}
            </span>
          </div>
        </div>
      </div>

      {/* Visual Analytics Charts Section */}
      {showCharts && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))',
            gap: '1.25rem',
            marginBottom: '1.5rem'
          }}
        >
          <ChartCard
            title="PO Status Distribution"
            subtitle={`${purchases.length} orders shown on current page`}
            icon={PieChartIcon}
            iconColor="#10b981"
          >
            {purchaseStatusDistribution.length === 0 ? (
              <div style={{ display: 'flex', height: '100%', alignItems: 'center', justifyContent: 'center', color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>
                No purchase orders recorded yet
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={230}>
                <PieChart>
                  <Tooltip content={<CustomTooltip valueSuffix=" orders" />} />
                  <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{ fontSize: '12px' }} />
                  <Pie
                    data={purchaseStatusDistribution}
                    cx="50%"
                    cy="45%"
                    innerRadius={50}
                    outerRadius={75}
                    paddingAngle={4}
                    dataKey="value"
                  >
                    {purchaseStatusDistribution.map((entry, index) => (
                      <Cell key={`cell-po-status-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            )}
          </ChartCard>

          <ChartCard
            title="Procurement Spend by Supplier"
            subtitle="Top vendors by total purchase value"
            icon={Building}
            iconColor="#6366f1"
          >
            {supplierSpendData.length === 0 ? (
              <div style={{ display: 'flex', height: '100%', alignItems: 'center', justifyContent: 'center', color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>
                No supplier spend data available
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={230}>
                <BarChart data={supplierSpendData} margin={{ top: 10, right: 10, left: 10, bottom: 25 }}>
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} interval={0} />
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
                  <Bar dataKey="amount" fill="#6366f1" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </ChartCard>
        </div>
      )}

      {/* Filter Toolbar */}
      <div
        className="toolbar"
        style={{
          padding: '1rem',
          backgroundColor: 'var(--color-card-bg)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--color-border)',
          marginBottom: '1rem',
          display: 'flex',
          flexWrap: 'wrap',
          gap: '0.75rem',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}
      >
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '0.5rem', flex: '1 1 280px', maxWidth: '400px' }}>
          <div style={{ position: 'relative', width: '100%' }}>
            <input
              type="text"
              placeholder="Search by PO # or supplier..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="form-input"
              style={{ width: '100%', paddingLeft: '2.25rem' }}
            />
            <Search size={16} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-muted)' }} />
          </div>
          <Button type="submit" variant="secondary">
            Search
          </Button>
        </form>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
          {/* Status Filter */}
          <select
            className="form-input"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            style={{ padding: '0.5rem 0.75rem', minWidth: '140px' }}
          >
            <option value="">All Statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="ORDERED">Ordered</option>
            <option value="RECEIVED">Received</option>
            <option value="CANCELLED">Cancelled</option>
          </select>

          {/* Supplier Filter */}
          <select
            className="form-input"
            value={supplierFilter}
            onChange={(e) => {
              setSupplierFilter(e.target.value);
              setPage(0);
            }}
            style={{ padding: '0.5rem 0.75rem', minWidth: '170px' }}
          >
            <option value="">All Suppliers</option>
            {suppliersList.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>

          {(appliedSearch || statusFilter || supplierFilter) && (
            <Button variant="outline" size="sm" icon={X} onClick={handleClearFilters}>
              Reset
            </Button>
          )}
        </div>
      </div>

      {/* Main Table */}
      <div
        className="table-container"
        style={{
          backgroundColor: 'var(--color-card-bg)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--color-border)',
          overflow: 'hidden'
        }}
      >
        {isLoading ? (
          <div style={{ padding: '4rem 0', display: 'flex', justifyContent: 'center' }}>
            <LoadingSpinner text="Loading purchase orders..." />
          </div>
        ) : purchases.length === 0 ? (
          <div style={{ padding: '4rem 1rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
            <ShoppingCart size={48} style={{ opacity: 0.3, marginBottom: '0.75rem' }} />
            <h3 style={{ margin: '0 0 0.5rem 0', color: 'var(--color-text-main)' }}>No Purchase Orders Found</h3>
            <p style={{ margin: 0, fontSize: '0.875rem' }}>
              {appliedSearch || statusFilter || supplierFilter
                ? 'Try adjusting your search criteria or resetting filters.'
                : 'Get started by creating your first purchase order to replenish stock.'}
            </p>
            {canManagePurchases && !appliedSearch && !statusFilter && !supplierFilter && (
              <div style={{ marginTop: '1.25rem' }}>
                <Button variant="primary" icon={Plus} onClick={handleCreateOpen}>
                  Create Purchase Order
                </Button>
              </div>
            )}
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
            <thead style={{ backgroundColor: 'var(--color-bg-secondary)', borderBottom: '1px solid var(--color-border)' }}>
              <tr>
                <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontWeight: 600 }}>PO Number</th>
                <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontWeight: 600 }}>Supplier</th>
                <th style={{ padding: '0.75rem 1rem', textAlign: 'center', fontWeight: 600, width: '120px' }}>Status</th>
                <th style={{ padding: '0.75rem 1rem', textAlign: 'right', fontWeight: 600, width: '90px' }}>Items</th>
                <th style={{ padding: '0.75rem 1rem', textAlign: 'right', fontWeight: 600, width: '120px' }}>Total Cost</th>
                <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontWeight: 600, width: '130px' }}>Delivery Date</th>
                <th style={{ padding: '0.75rem 1rem', textAlign: 'center', fontWeight: 600, width: '140px' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {purchases.map((po, index) => (
                <tr
                  key={po.id}
                  style={{
                    borderBottom: index < purchases.length - 1 ? '1px solid var(--color-border)' : 'none',
                    transition: 'background-color 0.15s ease'
                  }}
                  className="table-row-hover"
                >
                  <td style={{ padding: '0.875rem 1rem', fontWeight: 600, color: 'var(--color-text-main)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <ShoppingCart size={15} style={{ color: 'var(--color-text-muted)' }} />
                      <span>{po.orderNumber}</span>
                    </div>
                  </td>
                  <td style={{ padding: '0.875rem 1rem', color: 'var(--color-text-main)' }}>
                    {po.supplierName}
                  </td>
                  <td style={{ padding: '0.875rem 1rem', textAlign: 'center' }}>
                    {getStatusBadge(po.status)}
                  </td>
                  <td style={{ padding: '0.875rem 1rem', textAlign: 'right', fontWeight: 500 }}>
                    {po.itemsCount}
                  </td>
                  <td style={{ padding: '0.875rem 1rem', textAlign: 'right', fontWeight: 600, color: 'var(--color-primary)' }}>
                    ₹{Number(po.totalAmount).toFixed(2)}
                  </td>
                  <td style={{ padding: '0.875rem 1rem', color: 'var(--color-text-muted)' }}>
                    {po.expectedDeliveryDate || '—'}
                  </td>
                  <td style={{ padding: '0.875rem 1rem', textAlign: 'center' }}>
                    <div style={{ display: 'flex', justifyContent: 'center', gap: '0.375rem' }}>
                      {/* View Details */}
                      <button
                        className="btn-icon"
                        title="View Details"
                        onClick={() => {
                          setSelectedOrderId(po.id);
                          setIsDetailsModalOpen(true);
                        }}
                      >
                        <Eye size={16} />
                      </button>

                      {/* Edit (Draft only) */}
                      {po.status === 'DRAFT' && canManagePurchases && (
                        <button
                          className="btn-icon"
                          title="Edit Draft Order"
                          onClick={() => handleEditOpen(po)}
                        >
                          <Edit size={16} />
                        </button>
                      )}

                      {/* Quick Receive (Ordered only) */}
                      {po.status === 'ORDERED' && canReceiveGoods && (
                        <button
                          className="btn-icon"
                          title="Receive Shipment"
                          style={{ color: 'var(--color-emerald-600, #059669)' }}
                          onClick={() => setOrderToReceive(po)}
                        >
                          <PackageCheck size={16} />
                        </button>
                      )}

                      {/* Delete (Draft only) */}
                      {po.status === 'DRAFT' && canManagePurchases && (
                        <button
                          className="btn-icon text-danger"
                          title="Delete Draft"
                          onClick={() => setOrderToDelete(po)}
                        >
                          <Trash2 size={16} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div
            style={{
              padding: '0.875rem 1rem',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              borderTop: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-bg-secondary)'
            }}
          >
            <span style={{ fontSize: '0.8125rem', color: 'var(--color-text-muted)' }}>
              Showing {page * pageSize + 1} to {Math.min((page + 1) * pageSize, totalElements)} of {totalElements} orders
            </span>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Button
                variant="secondary"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                Previous
              </Button>
              <Button
                variant="secondary"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Form Modal (Create / Edit) */}
      <PurchaseOrderModal
        isOpen={isFormModalOpen}
        onClose={() => {
          setIsFormModalOpen(false);
          setEditingOrder(null);
        }}
        onSubmit={handleFormSubmit}
        initialData={editingOrder}
        isSubmitting={isSubmittingForm}
        backendErrors={backendFormErrors}
      />

      {/* Details Modal */}
      <PurchaseDetailsModal
        isOpen={isDetailsModalOpen}
        onClose={() => {
          setIsDetailsModalOpen(false);
          setSelectedOrderId(null);
        }}
        purchaseOrderId={selectedOrderId}
        onStatusChanged={() => fetchPurchases(page, appliedSearch, statusFilter, supplierFilter)}
        onEditRequested={(po) => handleEditOpen(po)}
        canManagePurchases={canManagePurchases}
        canReceiveGoods={canReceiveGoods}
      />

      {/* Delete Draft Confirmation Modal */}
      <Modal
        isOpen={Boolean(orderToDelete)}
        onClose={() => setOrderToDelete(null)}
        title="Delete Draft Order"
        maxWidth="450px"
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--color-text-main)' }}>
            Are you sure you want to permanently delete draft purchase order <strong>{orderToDelete?.orderNumber}</strong>? This action cannot be undone.
          </p>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button variant="secondary" onClick={() => setOrderToDelete(null)} disabled={isDeleting}>
              Cancel
            </Button>
            <Button variant="danger" loading={isDeleting} onClick={handleConfirmDelete}>
              Delete Order
            </Button>
          </div>
        </div>
      </Modal>

      {/* Quick Receive Confirmation Modal */}
      <Modal
        isOpen={Boolean(orderToReceive)}
        onClose={() => setOrderToReceive(null)}
        title="Receive Purchase Shipment"
        maxWidth="480px"
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div
            style={{
              padding: '0.875rem',
              backgroundColor: 'rgba(16, 185, 129, 0.08)',
              border: '1px solid var(--color-emerald-500, #10b981)',
              borderRadius: 'var(--radius-md)'
            }}
          >
            <h5 style={{ margin: '0 0 0.375rem 0', color: 'var(--color-emerald-700, #047857)', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
              <PackageCheck size={18} /> Confirm Stock Arrival
            </h5>
            <p style={{ margin: 0, fontSize: '0.8125rem', color: 'var(--color-text-main)' }}>
              Receiving order <strong>{orderToReceive?.orderNumber}</strong> will automatically increment inventory levels for all {orderToReceive?.itemsCount} line items and write audit entries to the stock ledger.
            </p>
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button variant="secondary" onClick={() => setOrderToReceive(null)} disabled={isReceiving}>
              Cancel
            </Button>
            <Button variant="primary" loading={isReceiving} onClick={handleConfirmReceive}>
              Yes, Receive Shipment
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
