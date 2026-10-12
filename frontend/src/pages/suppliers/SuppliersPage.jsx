import React, { useState, useEffect, useCallback } from 'react';
import { supplierApi } from '../../api/supplierApi';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import SupplierFormModal from './SupplierFormModal';
import SupplierDetailsModal from './SupplierDetailsModal';
import {
  Truck,
  Plus,
  Search,
  Eye,
  Edit,
  Trash2,
  RefreshCw,
  CheckCircle2,
  XCircle,
  X,
  Phone,
  Mail,
  UserCheck
} from 'lucide-react';

export default function SuppliersPage() {
  const [suppliers, setSuppliers] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState(''); // '' | 'true' | 'false'

  const [isLoading, setIsLoading] = useState(true);
  const [errorBanner, setErrorBanner] = useState(null);
  const [successBanner, setSuccessBanner] = useState(null);

  // Form Modal state
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState(null);
  const [isSubmittingForm, setIsSubmittingForm] = useState(false);
  const [backendFormErrors, setBackendFormErrors] = useState(null);

  // Details Modal state
  const [selectedSupplier, setSelectedSupplier] = useState(null);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);

  // Delete Modal state
  const [supplierToDelete, setSupplierToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Status toggle in-progress state
  const [togglingId, setTogglingId] = useState(null);

  const fetchSuppliers = useCallback(async (targetPage = page, searchTerm = appliedSearch, filter = statusFilter) => {
    setIsLoading(true);
    setErrorBanner(null);
    try {
      const activeParam = filter === 'true' ? true : filter === 'false' ? false : null;
      const response = await supplierApi.getSuppliers({
        page: targetPage,
        size: pageSize,
        search: searchTerm,
        active: activeParam,
        sort: 'id,asc'
      });
      setSuppliers(response.content || []);
      setTotalElements(response.totalElements || 0);
      setTotalPages(response.totalPages || 0);
      setPage(response.pageNumber || 0);
    } catch (err) {
      setErrorBanner(err.message || 'Failed to load suppliers from server');
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, appliedSearch, statusFilter]);

  useEffect(() => {
    fetchSuppliers(page, appliedSearch, statusFilter);
  }, [page, appliedSearch, statusFilter, fetchSuppliers]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setAppliedSearch(search);
  };

  const handleClearSearch = () => {
    setSearch('');
    setAppliedSearch('');
    setPage(0);
  };

  const handleStatusFilterChange = (e) => {
    setStatusFilter(e.target.value);
    setPage(0);
  };

  // Create / Update
  const handleOpenCreateModal = () => {
    setEditingSupplier(null);
    setBackendFormErrors(null);
    setIsFormModalOpen(true);
  };

  const handleOpenEditModal = (sup) => {
    setEditingSupplier(sup);
    setBackendFormErrors(null);
    setIsFormModalOpen(true);
  };

  const handleFormSubmit = async (formData) => {
    setIsSubmittingForm(true);
    setBackendFormErrors(null);
    try {
      if (editingSupplier) {
        await supplierApi.updateSupplier(editingSupplier.id, formData);
        setSuccessBanner(`Supplier "${formData.name}" updated successfully`);
      } else {
        await supplierApi.createSupplier(formData);
        setSuccessBanner(`Supplier "${formData.name}" registered successfully`);
      }
      setIsFormModalOpen(false);
      fetchSuppliers(page, appliedSearch, statusFilter);
    } catch (err) {
      setBackendFormErrors(err);
    } finally {
      setIsSubmittingForm(false);
    }
  };

  // Toggle active status
  const handleToggleStatus = async (sup) => {
    setTogglingId(sup.id);
    try {
      const newStatus = !sup.active;
      await supplierApi.toggleSupplierStatus(sup.id, newStatus);
      setSuccessBanner(`Supplier "${sup.name}" status updated to ${newStatus ? 'Active' : 'Inactive'}`);
      fetchSuppliers(page, appliedSearch, statusFilter);
    } catch (err) {
      setErrorBanner(err.message || 'Failed to update supplier status');
    } finally {
      setTogglingId(null);
    }
  };

  // Delete
  const handleOpenDeleteModal = (sup) => {
    setSupplierToDelete(sup);
  };

  const handleConfirmDelete = async () => {
    if (!supplierToDelete) return;
    setIsDeleting(true);
    try {
      await supplierApi.deleteSupplier(supplierToDelete.id);
      setSuccessBanner(`Supplier "${supplierToDelete.name}" deleted successfully`);
      setSupplierToDelete(null);
      if (suppliers.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        fetchSuppliers(page, appliedSearch, statusFilter);
      }
    } catch (err) {
      setErrorBanner(err.message || 'Failed to delete supplier');
    } finally {
      setIsDeleting(false);
    }
  };

  // Summary counts
  const activeCount = suppliers.filter((s) => s.active).length;
  const inactiveCount = suppliers.filter((s) => !s.active).length;

  return (
    <div>
      {/* Top Banner Notifications */}
      {successBanner && (
        <AlertBanner
          type="success"
          message={successBanner}
          onDismiss={() => setSuccessBanner(null)}
        />
      )}
      {errorBanner && (
        <AlertBanner
          type="error"
          message={errorBanner}
          onDismiss={() => setErrorBanner(null)}
        />
      )}

      {/* KPI Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-content">
            <span className="stat-label">Total Suppliers</span>
            <span className="stat-value">{totalElements}</span>
          </div>
          <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--color-primary-light)', color: 'var(--color-primary)' }}>
            <Truck size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-content">
            <span className="stat-label">Active Vendors (Page)</span>
            <span className="stat-value">{activeCount}</span>
          </div>
          <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--color-success-light)', color: 'var(--color-success)' }}>
            <CheckCircle2 size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-content">
            <span className="stat-label">Inactive Vendors (Page)</span>
            <span className="stat-value">{inactiveCount}</span>
          </div>
          <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--color-danger-light)', color: 'var(--color-danger)' }}>
            <XCircle size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-content">
            <span className="stat-label">PO Ready Status</span>
            <span className="stat-value">100% Verified</span>
          </div>
          <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--color-primary-light)', color: 'var(--color-primary)' }}>
            <UserCheck size={22} />
          </div>
        </div>
      </div>

      {/* Main Card */}
      <div className="card">
        <div className="card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', flex: 1 }}>
            <form onSubmit={handleSearchSubmit} className="search-input-wrapper" style={{ minWidth: '240px' }}>
              <Search size={16} className="search-input-icon" />
              <input
                type="text"
                placeholder="Search vendor name or contact..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="search-input"
              />
              {search && (
                <button
                  type="button"
                  onClick={handleClearSearch}
                  style={{ position: 'absolute', right: '8px', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--color-text-muted)' }}
                >
                  <X size={14} />
                </button>
              )}
            </form>

            <select
              value={statusFilter}
              onChange={handleStatusFilterChange}
              className="form-input"
              style={{ width: 'auto', padding: '0.5rem 0.75rem', cursor: 'pointer' }}
            >
              <option value="">All Statuses</option>
              <option value="true">Active Only</option>
              <option value="false">Inactive Only</option>
            </select>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Button
              variant="secondary"
              icon={RefreshCw}
              onClick={() => fetchSuppliers(page, appliedSearch, statusFilter)}
              disabled={isLoading}
              title="Refresh suppliers list"
            >
              Refresh
            </Button>
            <Button
              variant="primary"
              icon={Plus}
              onClick={handleOpenCreateModal}
            >
              Add Supplier
            </Button>
          </div>
        </div>

        {/* Content Area */}
        {isLoading ? (
          <LoadingSpinner message="Loading suppliers directory..." />
        ) : suppliers.length === 0 ? (
          <div className="empty-state">
            <Truck size={44} style={{ opacity: 0.4 }} />
            <h3 style={{ fontSize: '1.125rem', fontWeight: '600', color: 'var(--color-text-primary)' }}>
              {appliedSearch || statusFilter ? 'No suppliers match your filters' : 'No suppliers registered'}
            </h3>
            <p style={{ maxWidth: '420px' }}>
              {appliedSearch || statusFilter
                ? 'Try broadening your search keywords or resetting the status filter.'
                : 'Build your procurement network by adding vendors and supplier partners to SIMS.'}
            </p>
            {appliedSearch || statusFilter ? (
              <Button
                variant="secondary"
                onClick={() => {
                  handleClearSearch();
                  setStatusFilter('');
                }}
              >
                Reset Filters
              </Button>
            ) : (
              <Button variant="primary" icon={Plus} onClick={handleOpenCreateModal}>
                Register First Supplier
              </Button>
            )}
          </div>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Vendor / Company</th>
                  <th>Contact Person</th>
                  <th>Contact Details</th>
                  <th>Address</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {suppliers.map((s) => (
                  <tr key={s.id}>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <span style={{ fontWeight: '600', color: 'var(--color-text-primary)' }}>
                          {s.name}
                        </span>
                        <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                          ID: #{s.id}
                        </span>
                      </div>
                    </td>
                    <td>
                      <span style={{ color: s.contactPerson ? 'var(--color-text-primary)' : 'var(--color-text-muted)' }}>
                        {s.contactPerson || '—'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem', fontSize: '0.8125rem' }}>
                        {s.email && (
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', color: 'var(--color-text-secondary)' }}>
                            <Mail size={12} />
                            {s.email}
                          </span>
                        )}
                        {s.phone && (
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', color: 'var(--color-text-secondary)' }}>
                            <Phone size={12} />
                            {s.phone}
                          </span>
                        )}
                        {!s.email && !s.phone && (
                          <span style={{ color: 'var(--color-text-muted)' }}>—</span>
                        )}
                      </div>
                    </td>
                    <td>
                      <span
                        style={{
                          fontSize: '0.8125rem',
                          color: 'var(--color-text-secondary)',
                          display: 'inline-block',
                          maxWidth: '220px',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap'
                        }}
                        title={s.address || ''}
                      >
                        {s.address || '—'}
                      </span>
                    </td>
                    <td>
                      <button
                        type="button"
                        onClick={() => handleToggleStatus(s)}
                        disabled={togglingId === s.id}
                        style={{
                          background: 'none',
                          border: 'none',
                          padding: 0,
                          cursor: 'pointer',
                          display: 'inline-flex'
                        }}
                        title="Click to toggle active status"
                      >
                        {s.active ? (
                          <Badge variant="success">Active</Badge>
                        ) : (
                          <Badge variant="neutral">Inactive</Badge>
                        )}
                      </button>
                    </td>
                    <td>
                      <div className="actions-cell">
                        <Button
                          variant="secondary"
                          size="sm"
                          icon={Eye}
                          onClick={() => {
                            setSelectedSupplier(s);
                            setIsDetailsModalOpen(true);
                          }}
                          title="View supplier profile"
                        >
                          View
                        </Button>
                        <Button
                          variant="secondary"
                          size="sm"
                          icon={Edit}
                          onClick={() => handleOpenEditModal(s)}
                          title="Edit supplier"
                        >
                          Edit
                        </Button>
                        <Button
                          variant="outline-danger"
                          size="sm"
                          icon={Trash2}
                          onClick={() => handleOpenDeleteModal(s)}
                          title="Delete supplier"
                        >
                          Delete
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {!isLoading && totalElements > 0 && (
          <div className="pagination-container">
            <span>
              Showing <strong>{suppliers.length}</strong> of <strong>{totalElements}</strong> suppliers
            </span>
            <div className="pagination-controls">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </Button>
              <span style={{ fontSize: '0.8125rem', margin: '0 0.5rem' }}>
                Page {page + 1} of {Math.max(1, totalPages)}
              </span>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Form Modal (Create / Edit) */}
      <SupplierFormModal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        onSubmit={handleFormSubmit}
        supplier={editingSupplier}
        isSubmitting={isSubmittingForm}
        backendErrors={backendFormErrors}
      />

      {/* Details Modal */}
      <SupplierDetailsModal
        isOpen={isDetailsModalOpen}
        onClose={() => setIsDetailsModalOpen(false)}
        supplier={selectedSupplier}
        onEdit={(sup) => {
          setIsDetailsModalOpen(false);
          handleOpenEditModal(sup);
        }}
      />

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={Boolean(supplierToDelete)}
        onClose={() => setSupplierToDelete(null)}
        title="Confirm Supplier Deletion"
        footer={
          <>
            <Button variant="secondary" onClick={() => setSupplierToDelete(null)} disabled={isDeleting}>
              Cancel
            </Button>
            <Button
              variant="danger"
              icon={Trash2}
              onClick={handleConfirmDelete}
              loading={isDeleting}
            >
              Delete Supplier
            </Button>
          </>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <p style={{ color: 'var(--color-text-primary)' }}>
            Are you sure you want to permanently delete this supplier?
          </p>
          {supplierToDelete && (
            <div style={{ padding: '0.75rem 1rem', backgroundColor: 'var(--color-danger-light)', border: '1px solid var(--color-danger-border)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontWeight: '600', color: 'var(--color-danger)' }}>
                {supplierToDelete.name}
              </div>
              <div style={{ fontSize: '0.8125rem', color: 'var(--color-text-secondary)', marginTop: '0.25rem' }}>
                Contact: {supplierToDelete.contactPerson || 'N/A'} | Status: {supplierToDelete.active ? 'Active' : 'Inactive'}
              </div>
            </div>
          )}
          <p style={{ fontSize: '0.8125rem', color: 'var(--color-text-muted)' }}>
            This supplier record will be permanently removed from your catalog.
          </p>
        </div>
      </Modal>
    </div>
  );
}
