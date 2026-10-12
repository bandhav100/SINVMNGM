import React, { useState, useEffect, useCallback } from 'react';
import { userApi } from '../../api/userApi';
import { useAuth } from '../../context/AuthContext';
import UserFormModal from './UserFormModal';
import UserDetailsModal from './UserDetailsModal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  Users,
  UserPlus,
  Search,
  Filter,
  Eye,
  Edit2,
  UserCheck,
  UserX,
  Shield,
  RefreshCw
} from 'lucide-react';

const roleBadgeVariant = {
  ADMIN: 'primary',
  INVENTORY_MANAGER: 'info',
  PURCHASE_MANAGER: 'warning',
  SALES_STAFF: 'success'
};

const roleDisplayName = {
  ADMIN: 'Administrator',
  INVENTORY_MANAGER: 'Inventory Manager',
  PURCHASE_MANAGER: 'Purchase Manager',
  SALES_STAFF: 'Sales Staff'
};

export default function UsersPage() {
  const { user: currentUser } = useAuth();

  const [users, setUsers] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);

  const [search, setSearch] = useState('');
  const [selectedRole, setSelectedRole] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');

  const [loading, setLoading] = useState(true);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // Modal states
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [viewingUser, setViewingUser] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [backendErrors, setBackendErrors] = useState(null);

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      setActionError(null);

      const params = {
        page: currentPage,
        size: pageSize
      };
      if (search.trim()) params.search = search.trim();
      if (selectedRole) params.role = selectedRole;
      if (selectedStatus !== '') params.active = selectedStatus === 'true';

      const data = await userApi.getUsers(params);
      setUsers(data.content || []);
      setTotalElements(data.totalElements || 0);
      setPages(data.totalPages || 1);
    } catch (err) {
      setActionError(err.message || 'Failed to load users list.');
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, search, selectedRole, selectedStatus]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setCurrentPage(0);
    fetchUsers();
  };

  const handleOpenCreate = () => {
    setEditingUser(null);
    setBackendErrors(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = (user) => {
    setEditingUser(user);
    setBackendErrors(null);
    setIsFormOpen(true);
  };

  const handleFormSubmit = async (formData) => {
    try {
      setIsSubmitting(true);
      setBackendErrors(null);

      if (editingUser) {
        await userApi.updateUser(editingUser.id, formData);
        setActionSuccess(`User '${editingUser.username}' updated successfully.`);
      } else {
        await userApi.createUser(formData);
        setActionSuccess(`User '${formData.username}' created successfully.`);
      }

      setIsFormOpen(false);
      fetchUsers();
    } catch (err) {
      if (err.validationErrors) {
        setBackendErrors(err.validationErrors);
      } else {
        setBackendErrors(err.message || 'Operation failed.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggleStatus = async (user) => {
    if (user.username === currentUser?.username) {
      setActionError('Security Policy: You cannot deactivate your own administrator account.');
      return;
    }

    const nextStatus = !user.active;
    const actionLabel = nextStatus ? 'activate' : 'deactivate';

    if (!window.confirm(`Are you sure you want to ${actionLabel} account '${user.username}'?`)) {
      return;
    }

    try {
      setActionError(null);
      await userApi.toggleUserStatus(user.id, nextStatus);
      setActionSuccess(`Account '${user.username}' was successfully ${nextStatus ? 'activated' : 'deactivated'}.`);
      fetchUsers();
    } catch (err) {
      setActionError(err.message || `Failed to ${actionLabel} user.`);
    }
  };

  return (
    <div className="page-container">
      {/* Page Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '1.5rem',
        flexWrap: 'wrap',
        gap: '1rem'
      }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--text-primary)', margin: 0 }}>
            Users & Roles Management
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
            Configure team accounts, assigned roles, and access credentials ({totalElements} total users)
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Button
            variant="outline"
            onClick={fetchUsers}
            disabled={loading}
            title="Refresh list"
          >
            <RefreshCw size={16} />
          </Button>

          <Button
            variant="primary"
            onClick={handleOpenCreate}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <UserPlus size={18} />
            Add Staff User
          </Button>
        </div>
      </div>

      {actionError && (
        <div style={{ marginBottom: '1.25rem' }}>
          <AlertBanner type="error" message={actionError} onClose={() => setActionError(null)} />
        </div>
      )}

      {actionSuccess && (
        <div style={{ marginBottom: '1.25rem' }}>
          <AlertBanner type="success" message={actionSuccess} onClose={() => setActionSuccess(null)} />
        </div>
      )}

      {/* Filters Toolbar */}
      <div style={{
        display: 'flex',
        gap: '1rem',
        marginBottom: '1.5rem',
        backgroundColor: 'var(--card-bg, #ffffff)',
        padding: '1rem',
        borderRadius: '8px',
        border: '1px solid var(--border-color, #e2e8f0)',
        flexWrap: 'wrap',
        alignItems: 'center'
      }}>
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '0.5rem', flex: '1 1 280px' }}>
          <Input
            id="user-search"
            placeholder="Search by username, name, or email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ marginBottom: 0 }}
          />
          <Button type="submit" variant="secondary">
            <Search size={16} />
          </Button>
        </form>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <select
            value={selectedRole}
            onChange={(e) => {
              setSelectedRole(e.target.value);
              setCurrentPage(0);
            }}
            style={{
              padding: '0.5rem 0.75rem',
              borderRadius: '6px',
              border: '1px solid var(--border-color, #cbd5e1)',
              backgroundColor: '#ffffff',
              fontSize: '0.875rem'
            }}
          >
            <option value="">All Roles</option>
            <option value="ADMIN">Administrator</option>
            <option value="INVENTORY_MANAGER">Inventory Manager</option>
            <option value="PURCHASE_MANAGER">Purchase Manager</option>
            <option value="SALES_STAFF">Sales Staff</option>
          </select>

          <select
            value={selectedStatus}
            onChange={(e) => {
              setSelectedStatus(e.target.value);
              setCurrentPage(0);
            }}
            style={{
              padding: '0.5rem 0.75rem',
              borderRadius: '6px',
              border: '1px solid var(--border-color, #cbd5e1)',
              backgroundColor: '#ffffff',
              fontSize: '0.875rem'
            }}
          >
            <option value="">All Statuses</option>
            <option value="true">Active Only</option>
            <option value="false">Deactivated Only</option>
          </select>
        </div>
      </div>

      {/* Users Table */}
      <div style={{
        backgroundColor: 'var(--card-bg, #ffffff)',
        borderRadius: '8px',
        border: '1px solid var(--border-color, #e2e8f0)',
        overflow: 'hidden'
      }}>
        {loading ? (
          <div style={{ padding: '3rem', textAlign: 'center' }}>
            <LoadingSpinner text="Loading users directory..." />
          </div>
        ) : users.length === 0 ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
            <Users size={40} style={{ opacity: 0.4, marginBottom: '0.5rem' }} />
            <p style={{ margin: 0, fontWeight: '500' }}>No users found matching your criteria</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ backgroundColor: 'var(--bg-secondary, #f8fafc)', borderBottom: '1px solid var(--border-color, #e2e8f0)' }}>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>User</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Email</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Role</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: '600' }}>Created Date</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: '600', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => {
                  const isCurrent = u.username.toLowerCase() === currentUser?.username?.toLowerCase();
                  return (
                    <tr
                      key={u.id}
                      style={{
                        borderBottom: '1px solid var(--border-color, #f1f5f9)',
                        backgroundColor: isCurrent ? 'var(--color-primary-light, #f0fdf4)' : 'transparent'
                      }}
                    >
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <div style={{ fontWeight: '600', color: 'var(--text-primary)' }}>
                          {u.fullName}
                          {isCurrent && (
                            <span style={{
                              marginLeft: '0.5rem',
                              fontSize: '0.7rem',
                              backgroundColor: '#2563eb',
                              color: '#ffffff',
                              padding: '2px 6px',
                              borderRadius: '4px',
                              fontWeight: 'normal'
                            }}>
                              You
                            </span>
                          )}
                        </div>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>@{u.username}</div>
                      </td>

                      <td style={{ padding: '0.75rem 1rem', color: 'var(--text-primary)' }}>
                        {u.email}
                      </td>

                      <td style={{ padding: '0.75rem 1rem' }}>
                        <Badge variant={roleBadgeVariant[u.role] || 'info'}>
                          {roleDisplayName[u.role] || u.role}
                        </Badge>
                      </td>

                      <td style={{ padding: '0.75rem 1rem' }}>
                        {u.active ? (
                          <Badge variant="success">Active</Badge>
                        ) : (
                          <Badge variant="danger">Deactivated</Badge>
                        )}
                      </td>

                      <td style={{ padding: '0.75rem 1rem', color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}
                      </td>

                      <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '0.35rem' }}>
                          <button
                            onClick={() => setViewingUser(u)}
                            title="View Details"
                            style={{
                              padding: '0.35rem 0.5rem',
                              borderRadius: '4px',
                              border: '1px solid var(--border-color, #e2e8f0)',
                              backgroundColor: '#ffffff',
                              cursor: 'pointer'
                            }}
                          >
                            <Eye size={14} style={{ color: 'var(--text-secondary)' }} />
                          </button>

                          <button
                            onClick={() => handleOpenEdit(u)}
                            title="Edit User"
                            style={{
                              padding: '0.35rem 0.5rem',
                              borderRadius: '4px',
                              border: '1px solid var(--border-color, #e2e8f0)',
                              backgroundColor: '#ffffff',
                              cursor: 'pointer'
                            }}
                          >
                            <Edit2 size={14} style={{ color: '#2563eb' }} />
                          </button>

                          <button
                            onClick={() => handleToggleStatus(u)}
                            disabled={isCurrent}
                            title={
                              isCurrent
                                ? 'You cannot deactivate your own administrator account'
                                : u.active
                                ? 'Deactivate Account'
                                : 'Activate Account'
                            }
                            style={{
                              padding: '0.35rem 0.5rem',
                              borderRadius: '4px',
                              border: '1px solid var(--border-color, #e2e8f0)',
                              backgroundColor: isCurrent ? '#f1f5f9' : '#ffffff',
                              cursor: isCurrent ? 'not-allowed' : 'pointer',
                              opacity: isCurrent ? 0.5 : 1
                            }}
                          >
                            {u.active ? (
                              <UserX size={14} style={{ color: '#ef4444' }} />
                            ) : (
                              <UserCheck size={14} style={{ color: '#10b981' }} />
                            )}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '0.75rem 1rem',
            borderTop: '1px solid var(--border-color, #e2e8f0)',
            backgroundColor: 'var(--bg-secondary, #f8fafc)'
          }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
              Page {currentPage + 1} of {totalPages}
            </span>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((p) => Math.max(p - 1, 0))}
                disabled={currentPage === 0 || loading}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages - 1))}
                disabled={currentPage >= totalPages - 1 || loading}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      {isFormOpen && (
        <UserFormModal
          isOpen={isFormOpen}
          onClose={() => setIsFormOpen(false)}
          onSubmit={handleFormSubmit}
          user={editingUser}
          currentUser={currentUser}
          isSubmitting={isSubmitting}
          backendErrors={backendErrors}
        />
      )}

      {viewingUser && (
        <UserDetailsModal
          isOpen={Boolean(viewingUser)}
          onClose={() => setViewingUser(null)}
          user={viewingUser}
          onEdit={(u) => {
            setViewingUser(null);
            handleOpenEdit(u);
          }}
        />
      )}
    </div>
  );
}
