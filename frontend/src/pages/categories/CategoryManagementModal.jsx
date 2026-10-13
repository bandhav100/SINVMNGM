import React, { useState, useEffect, useCallback } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { categoryApi } from '../../api/categoryApi';
import { Plus, Edit, Trash2, Tag, AlertTriangle, Check, X } from 'lucide-react';

export default function CategoryManagementModal({ isOpen, onClose, onCategoriesUpdated }) {
  const [categories, setCategories] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorBanner, setErrorBanner] = useState(null);
  const [successBanner, setSuccessBanner] = useState(null);

  // Form state (inline or modal)
  const [editingCategory, setEditingCategory] = useState(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  // Deletion confirmation state
  const [categoryToDelete, setCategoryToDelete] = useState(null);
  const [productCountWarning, setProductCountWarning] = useState(null);
  const [isCheckingDelete, setIsCheckingDelete] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const loadCategories = useCallback(async () => {
    setIsLoading(true);
    setErrorBanner(null);
    try {
      const data = await categoryApi.getAllCategories();
      setCategories(data || []);
    } catch (err) {
      setErrorBanner(err.message || 'Failed to load categories');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isOpen) {
      loadCategories();
      resetForm();
      setCategoryToDelete(null);
      setErrorBanner(null);
      setSuccessBanner(null);
    }
  }, [isOpen, loadCategories]);

  const resetForm = () => {
    setEditingCategory(null);
    setName('');
    setDescription('');
    setFormError(null);
  };

  const handleStartEdit = (cat) => {
    setEditingCategory(cat);
    setName(cat.name);
    setDescription(cat.description || '');
    setFormError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setFormError('Category name is required');
      return;
    }

    setIsSubmitting(true);
    setFormError(null);
    try {
      if (editingCategory) {
        await categoryApi.updateCategory(editingCategory.id, {
          name: name.trim(),
          description: description.trim() || null
        });
        setSuccessBanner(`Category "${name.trim()}" updated successfully`);
      } else {
        await categoryApi.createCategory({
          name: name.trim(),
          description: description.trim() || null
        });
        setSuccessBanner(`Category "${name.trim()}" created successfully`);
      }
      resetForm();
      await loadCategories();
      if (onCategoriesUpdated) onCategoriesUpdated();
    } catch (err) {
      if (err.validationErrors && err.validationErrors.name) {
        setFormError(err.validationErrors.name);
      } else {
        setFormError(err.message || 'Failed to save category');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const initiateDeleteCheck = async (cat) => {
    setCategoryToDelete(cat);
    setIsCheckingDelete(true);
    setProductCountWarning(null);
    try {
      const countRes = await categoryApi.getProductCount(cat.id);
      setProductCountWarning(countRes.productCount || 0);
    } catch (err) {
      setProductCountWarning(0);
    } finally {
      setIsCheckingDelete(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!categoryToDelete) return;
    setIsDeleting(true);
    try {
      await categoryApi.deleteCategory(categoryToDelete.id);
      setSuccessBanner(
        `Category "${categoryToDelete.name}" deleted successfully. Any linked products now have category set to NULL.`
      );
      setCategoryToDelete(null);
      await loadCategories();
      if (onCategoriesUpdated) onCategoriesUpdated();
    } catch (err) {
      setErrorBanner(err.message || 'Failed to delete category');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Category Management"
      maxWidth="680px"
      footer={
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
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

        {/* Create / Edit Form */}
        <form
          onSubmit={handleSubmit}
          style={{
            padding: '1rem',
            backgroundColor: 'var(--color-surface-subtle)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--color-border)',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.75rem'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontWeight: '600', fontSize: '0.9375rem', color: 'var(--color-text-primary)' }}>
              {editingCategory ? `Edit Category: ${editingCategory.name}` : 'Add New Category'}
            </span>
            {editingCategory && (
              <Button size="sm" variant="secondary" onClick={resetForm} type="button">
                Cancel Edit
              </Button>
            )}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <Input
              label="Category Name"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                setFormError(null);
              }}
              required
              placeholder="e.g. Peripherals"
              error={formError}
            />
            <Input
              label="Description (Optional)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Keyboards, mice, webcams"
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.25rem' }}>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              icon={editingCategory ? Check : Plus}
              loading={isSubmitting}
            >
              {editingCategory ? 'Update Category' : 'Add Category'}
            </Button>
          </div>
        </form>

        {/* Category List */}
        <div>
          <span style={{ fontWeight: '600', fontSize: '0.875rem', color: 'var(--color-text-secondary)', display: 'block', marginBottom: '0.5rem' }}>
            Configured Categories ({categories.length})
          </span>

          {isLoading ? (
            <LoadingSpinner message="Loading categories..." />
          ) : categories.length === 0 ? (
            <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
              No categories configured yet. Add your first category above.
            </div>
          ) : (
            <div style={{ maxHeight: '300px', overflowY: 'auto', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)' }}>
              <table className="data-table" style={{ margin: 0 }}>
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Description</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {categories.map((cat) => (
                    <tr key={cat.id}>
                      <td>
                        <span style={{ fontWeight: '600', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                          <Tag size={13} style={{ color: 'var(--color-primary)' }} />
                          {cat.name}
                        </span>
                      </td>
                      <td style={{ color: 'var(--color-text-muted)', fontSize: '0.8125rem' }}>
                        {cat.description || '—'}
                      </td>
                      <td>
                        <div className="actions-cell">
                          <Button
                            variant="secondary"
                            size="sm"
                            icon={Edit}
                            onClick={() => handleStartEdit(cat)}
                            title="Edit Category"
                          />
                          <Button
                            variant="outline-danger"
                            size="sm"
                            icon={Trash2}
                            onClick={() => initiateDeleteCheck(cat)}
                            title="Delete Category"
                          />
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Delete Confirmation Modal / Alert */}
        {categoryToDelete && (
          <Modal
            isOpen={true}
            onClose={() => setCategoryToDelete(null)}
            title="Confirm Category Deletion"
            maxWidth="500px"
            footer={
              <>
                <Button variant="secondary" onClick={() => setCategoryToDelete(null)} disabled={isDeleting}>
                  Cancel
                </Button>
                <Button
                  variant="danger"
                  onClick={handleConfirmDelete}
                  loading={isDeleting}
                  disabled={isCheckingDelete}
                >
                  Confirm Delete
                </Button>
              </>
            }
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <p>
                Are you sure you want to delete category <strong>&quot;{categoryToDelete.name}&quot;</strong>?
              </p>

              {isCheckingDelete ? (
                <LoadingSpinner message="Checking referenced products..." />
              ) : productCountWarning > 0 ? (
                <div
                  style={{
                    backgroundColor: '#fffbeb',
                    border: '1px solid #fde68a',
                    borderRadius: 'var(--radius-md)',
                    padding: '0.875rem',
                    display: 'flex',
                    gap: '0.75rem',
                    alignItems: 'flex-start'
                  }}
                >
                  <AlertTriangle size={20} color="#d97706" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div style={{ fontSize: '0.875rem', color: '#92400e' }}>
                    <strong>Warning: {productCountWarning} product(s)</strong> currently reference this category.
                    <p style={{ marginTop: '0.375rem', lineHeight: '1.4' }}>
                      Under the database schema rules, products will <em>not</em> be deleted. Their category reference will be safely set to <strong>NULL</strong> (Uncategorized).
                    </p>
                  </div>
                </div>
              ) : (
                <p style={{ fontSize: '0.875rem', color: 'var(--color-text-muted)' }}>
                  No products currently reference this category. It can be safely removed.
                </p>
              )}
            </div>
          </Modal>
        )}
      </div>
    </Modal>
  );
}
