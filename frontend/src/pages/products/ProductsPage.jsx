import React, { useState, useEffect, useCallback } from 'react';
import { productApi } from '../../api/productApi';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ProductFormModal from './ProductFormModal';
import ProductDetailsModal from './ProductDetailsModal';
import CategoryManagementModal from '../categories/CategoryManagementModal';
import {
  Package,
  Plus,
  Search,
  Eye,
  Edit,
  Trash2,
  RefreshCw,
  AlertTriangle,
  Boxes,
  TrendingDown,
  X,
  Tag
} from 'lucide-react';

export default function ProductsPage() {
  const [products, setProducts] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');

  const [isLoading, setIsLoading] = useState(true);
  const [errorBanner, setErrorBanner] = useState(null);
  const [successBanner, setSuccessBanner] = useState(null);

  // Modals state
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [isSubmittingForm, setIsSubmittingForm] = useState(false);
  const [backendFormErrors, setBackendFormErrors] = useState(null);

  const [selectedProduct, setSelectedProduct] = useState(null);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);

  const [productToDelete, setProductToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);

  // Fetch products
  const fetchProducts = useCallback(async (targetPage = page, searchTerm = appliedSearch) => {
    setIsLoading(true);
    setErrorBanner(null);
    try {
      const response = await productApi.getProducts({
        page: targetPage,
        size: pageSize,
        search: searchTerm,
        sort: 'id,asc'
      });
      setProducts(response.content || []);
      setTotalElements(response.totalElements || 0);
      setTotalPages(response.totalPages || 0);
      setPage(response.pageNumber || 0);
    } catch (err) {
      setErrorBanner(err.message || 'Failed to load products from server');
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, appliedSearch]);

  useEffect(() => {
    fetchProducts(page, appliedSearch);
  }, [page, appliedSearch, fetchProducts]);

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

  // Create or Update
  const handleOpenCreateModal = () => {
    setEditingProduct(null);
    setBackendFormErrors(null);
    setIsFormModalOpen(true);
  };

  const handleOpenEditModal = (product) => {
    setEditingProduct(product);
    setBackendFormErrors(null);
    setIsFormModalOpen(true);
  };

  const handleFormSubmit = async (formData) => {
    setIsSubmittingForm(true);
    setBackendFormErrors(null);
    try {
      if (editingProduct) {
        await productApi.updateProduct(editingProduct.id, formData);
        setSuccessBanner(`Product "${formData.name}" updated successfully`);
      } else {
        await productApi.createProduct(formData);
        setSuccessBanner(`Product "${formData.name}" created successfully`);
      }
      setIsFormModalOpen(false);
      fetchProducts(page, appliedSearch);
    } catch (err) {
      setBackendFormErrors(err);
    } finally {
      setIsSubmittingForm(false);
    }
  };

  // Delete
  const handleOpenDeleteModal = (product) => {
    setProductToDelete(product);
  };

  const handleConfirmDelete = async () => {
    if (!productToDelete) return;
    setIsDeleting(true);
    try {
      await productApi.deleteProduct(productToDelete.id);
      setSuccessBanner(`Product "${productToDelete.name}" (${productToDelete.sku}) deleted successfully`);
      setProductToDelete(null);
      // If we deleted the last item on a page > 0, decrement page
      if (products.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        fetchProducts(page, appliedSearch);
      }
    } catch (err) {
      setErrorBanner(err.message || 'Failed to delete product');
    } finally {
      setIsDeleting(false);
    }
  };

  // Stats calculation
  const totalStockUnits = products.reduce((acc, p) => acc + (p.quantity || 0), 0);
  const lowStockCount = products.filter((p) => p.quantity > 0 && p.quantity <= p.reorderLevel).length;
  const outOfStockCount = products.filter((p) => p.quantity === 0).length;

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

      {/* Summary KPI Strip */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-content">
            <span className="stat-label">Total Registered</span>
            <span className="stat-value">{totalElements}</span>
          </div>
          <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--color-primary-light)', color: 'var(--color-primary)' }}>
            <Package size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-content">
            <span className="stat-label">Visible Stock Units</span>
            <span className="stat-value">{totalStockUnits}</span>
          </div>
          <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--color-success-light)', color: 'var(--color-success)' }}>
            <Boxes size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-content">
            <span className="stat-label">Low Stock (Page)</span>
            <span className="stat-value">{lowStockCount}</span>
          </div>
          <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--color-warning-light)', color: 'var(--color-warning)' }}>
            <TrendingDown size={22} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-content">
            <span className="stat-label">Out of Stock (Page)</span>
            <span className="stat-value">{outOfStockCount}</span>
          </div>
          <div className="stat-icon-wrapper" style={{ backgroundColor: 'var(--color-danger-light)', color: 'var(--color-danger)' }}>
            <AlertTriangle size={22} />
          </div>
        </div>
      </div>

      {/* Main Card */}
      <div className="card">
        <div className="card-header">
          <form onSubmit={handleSearchSubmit} className="search-input-wrapper">
            <Search size={16} className="search-input-icon" />
            <input
              type="text"
              placeholder="Search by name or SKU..."
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

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Button
              variant="secondary"
              icon={Tag}
              onClick={() => setIsCategoryModalOpen(true)}
              title="Manage product categories"
            >
              Categories
            </Button>
            <Button
              variant="secondary"
              icon={RefreshCw}
              onClick={() => fetchProducts(page, appliedSearch)}
              disabled={isLoading}
              title="Refresh product list"
            >
              Refresh
            </Button>
            <Button
              variant="primary"
              icon={Plus}
              onClick={handleOpenCreateModal}
            >
              Add Product
            </Button>
          </div>
        </div>

        {/* Content area */}
        {isLoading ? (
          <LoadingSpinner message="Loading products from inventory database..." />
        ) : products.length === 0 ? (
          <div className="empty-state">
            <Package size={44} style={{ opacity: 0.4 }} />
            <h3 style={{ fontSize: '1.125rem', fontWeight: '600', color: 'var(--color-text-primary)' }}>
              {appliedSearch ? 'No products match your search' : 'No products found'}
            </h3>
            <p style={{ maxWidth: '400px' }}>
              {appliedSearch
                ? `No products found matching "${appliedSearch}". Try adjusting your keywords or clearing the filter.`
                : 'Your inventory catalogue is currently empty. Get started by adding your first product.'}
            </p>
            {appliedSearch ? (
              <Button variant="secondary" onClick={handleClearSearch}>
                Clear Search
              </Button>
            ) : (
              <Button variant="primary" icon={Plus} onClick={handleOpenCreateModal}>
                Add Your First Product
              </Button>
            )}
          </div>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th>Category</th>
                  <th>Price</th>
                  <th>Stock Qty</th>
                  <th>Reorder Level</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => {
                  const isOutOfStock = p.quantity === 0;
                  const isLowStock = !isOutOfStock && p.quantity <= p.reorderLevel;

                  return (
                    <tr key={p.id}>
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column' }}>
                          <span style={{ fontWeight: '600', color: 'var(--color-text-primary)' }}>
                            {p.name}
                          </span>
                          {p.description && (
                            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '300px' }}>
                              {p.description}
                            </span>
                          )}
                        </div>
                      </td>
                      <td>
                        <code style={{ fontSize: '0.8125rem', padding: '0.125rem 0.375rem', backgroundColor: 'var(--color-surface-subtle)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border)' }}>
                          {p.sku}
                        </code>
                      </td>
                      <td>
                        {p.categoryName ? (
                          <Badge variant="primary">{p.categoryName}</Badge>
                        ) : (
                          <span style={{ color: 'var(--color-text-muted)', fontSize: '0.8125rem' }}>—</span>
                        )}
                      </td>
                      <td style={{ fontWeight: '600' }}>
                        ₹{Number(p.price).toFixed(2)}
                      </td>
                      <td style={{ fontWeight: '600' }}>
                        {p.quantity}
                      </td>
                      <td style={{ color: 'var(--color-text-secondary)' }}>
                        {p.reorderLevel}
                      </td>
                      <td>
                        {isOutOfStock ? (
                          <Badge variant="danger">Out of Stock</Badge>
                        ) : isLowStock ? (
                          <Badge variant="warning">Low Stock</Badge>
                        ) : (
                          <Badge variant="success">In Stock</Badge>
                        )}
                      </td>
                      <td>
                        <div className="actions-cell">
                          <Button
                            variant="secondary"
                            size="sm"
                            icon={Eye}
                            onClick={() => {
                              setSelectedProduct(p);
                              setIsDetailsModalOpen(true);
                            }}
                            title="View product details"
                          >
                            View
                          </Button>
                          <Button
                            variant="secondary"
                            size="sm"
                            icon={Edit}
                            onClick={() => handleOpenEditModal(p)}
                            title="Edit product"
                          >
                            Edit
                          </Button>
                          <Button
                            variant="outline-danger"
                            size="sm"
                            icon={Trash2}
                            onClick={() => handleOpenDeleteModal(p)}
                            title="Delete product"
                          >
                            Delete
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination bar */}
        {!isLoading && totalElements > 0 && (
          <div className="pagination-container">
            <span>
              Showing <strong>{products.length}</strong> of <strong>{totalElements}</strong> products
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

      {/* Product Create / Edit Modal */}
      <ProductFormModal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        onSubmit={handleFormSubmit}
        product={editingProduct}
        isSubmitting={isSubmittingForm}
        backendErrors={backendFormErrors}
      />

      {/* Product Details Modal */}
      <ProductDetailsModal
        isOpen={isDetailsModalOpen}
        onClose={() => setIsDetailsModalOpen(false)}
        product={selectedProduct}
        onEdit={(prod) => handleOpenEditModal(prod)}
      />

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={Boolean(productToDelete)}
        onClose={() => setProductToDelete(null)}
        title="Confirm Deletion"
        footer={
          <>
            <Button
              variant="secondary"
              onClick={() => setProductToDelete(null)}
              disabled={isDeleting}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              icon={Trash2}
              onClick={handleConfirmDelete}
              loading={isDeleting}
            >
              Delete Product
            </Button>
          </>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <p style={{ color: 'var(--color-text-primary)' }}>
            Are you sure you want to permanently delete this product?
          </p>
          {productToDelete && (
            <div style={{ padding: '0.75rem 1rem', backgroundColor: 'var(--color-danger-light)', border: '1px solid var(--color-danger-border)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontWeight: '600', color: 'var(--color-danger)' }}>
                {productToDelete.name}
              </div>
              <div style={{ fontSize: '0.8125rem', color: 'var(--color-text-secondary)', marginTop: '0.25rem' }}>
                SKU: <code>{productToDelete.sku}</code> | Current Stock: {productToDelete.quantity}
              </div>
            </div>
          )}
          <p style={{ fontSize: '0.8125rem', color: 'var(--color-text-muted)' }}>
            This action cannot be undone. Any historical records linked directly will be affected.
          </p>
        </div>
      </Modal>

      {/* Category Management Modal */}
      <CategoryManagementModal
        isOpen={isCategoryModalOpen}
        onClose={() => setIsCategoryModalOpen(false)}
        onCategoriesUpdated={() => fetchProducts(page, appliedSearch)}
      />
    </div>
  );
}
