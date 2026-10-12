import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import AlertBanner from '../../components/common/AlertBanner';
import { purchaseApi } from '../../api/purchaseApi';
import {
  ShoppingCart,
  Truck,
  Calendar,
  User,
  Clock,
  CheckCircle,
  XCircle,
  PackageCheck,
  Send,
  Edit,
  Trash2,
  Ban
} from 'lucide-react';

export default function PurchaseDetailsModal({
  isOpen,
  onClose,
  purchaseOrderId,
  onStatusChanged,
  onEditRequested,
  canManagePurchases = false,
  canReceiveGoods = false
}) {
  const [order, setOrder] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorBanner, setErrorBanner] = useState(null);
  const [actionSuccessBanner, setActionSuccessBanner] = useState(null);
  const [isActionLoading, setIsActionLoading] = useState(false);
  const [showReceiveConfirm, setShowReceiveConfirm] = useState(false);

  useEffect(() => {
    if (!isOpen || !purchaseOrderId) {
      setOrder(null);
      setErrorBanner(null);
      setActionSuccessBanner(null);
      setShowReceiveConfirm(false);
      return;
    }

    const fetchDetails = async () => {
      setIsLoading(true);
      setErrorBanner(null);
      try {
        const data = await purchaseApi.getPurchaseById(purchaseOrderId);
        setOrder(data);
      } catch (err) {
        setErrorBanner(err.response?.data?.message || 'Failed to load purchase order details');
      } finally {
        setIsLoading(false);
      }
    };

    fetchDetails();
  }, [isOpen, purchaseOrderId]);

  const handleAction = async (actionFn, successMsg) => {
    setIsActionLoading(true);
    setErrorBanner(null);
    try {
      const updated = await actionFn(order.id);
      setOrder(updated);
      setActionSuccessBanner(successMsg);
      setShowReceiveConfirm(false);
      if (onStatusChanged) onStatusChanged();
    } catch (err) {
      setErrorBanner(err.response?.data?.message || 'Operation failed');
    } finally {
      setIsActionLoading(false);
    }
  };

  const getStatusBadgeVariant = (status) => {
    switch (status) {
      case 'RECEIVED':
        return 'success';
      case 'ORDERED':
        return 'warning';
      case 'DRAFT':
        return 'neutral';
      case 'CANCELLED':
        return 'danger';
      default:
        return 'neutral';
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={order ? `Purchase Order ${order.orderNumber}` : 'Purchase Order Details'}
      maxWidth="750px"
    >
      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '3rem 0' }}>
          <LoadingSpinner text="Loading purchase order..." />
        </div>
      ) : order ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {errorBanner && <AlertBanner type="error" message={errorBanner} />}
          {actionSuccessBanner && <AlertBanner type="success" message={actionSuccessBanner} />}

          {/* Header Summary Card */}
          <div
            style={{
              padding: '1.25rem',
              backgroundColor: 'var(--color-bg-secondary)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}
          >
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <span style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--color-text-main)' }}>
                  {order.orderNumber}
                </span>
                <Badge variant={getStatusBadgeVariant(order.status)}>{order.status}</Badge>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', marginTop: '0.25rem', color: 'var(--color-text-muted)', fontSize: '0.8125rem' }}>
                <Truck size={14} />
                <span>Supplier: <strong>{order.supplierName}</strong></span>
              </div>
            </div>

            <div style={{ textAlign: 'right' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>Total Amount</span>
              <span style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--color-primary)' }}>
                ${Number(order.totalAmount).toFixed(2)}
              </span>
            </div>
          </div>

          {/* Metadata Grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
              gap: '1rem',
              padding: '0.875rem',
              backgroundColor: 'var(--color-card-bg)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              fontSize: '0.8125rem'
            }}
          >
            <div>
              <span style={{ color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.375rem', marginBottom: '0.25rem' }}>
                <Calendar size={14} /> Created At
              </span>
              <strong>{new Date(order.createdAt).toLocaleDateString()}</strong>
            </div>

            {order.expectedDeliveryDate && (
              <div>
                <span style={{ color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.375rem', marginBottom: '0.25rem' }}>
                  <Calendar size={14} /> Expected Delivery
                </span>
                <strong>{order.expectedDeliveryDate}</strong>
              </div>
            )}

            {order.createdBy && (
              <div>
                <span style={{ color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.375rem', marginBottom: '0.25rem' }}>
                  <User size={14} /> Created By
                </span>
                <strong>{order.createdBy}</strong>
              </div>
            )}

            {order.receivedAt && (
              <div>
                <span style={{ color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.375rem', marginBottom: '0.25rem' }}>
                  <PackageCheck size={14} /> Received At
                </span>
                <strong>{new Date(order.receivedAt).toLocaleDateString()} {order.receivedBy ? `by ${order.receivedBy}` : ''}</strong>
              </div>
            )}
          </div>

          {order.notes && (
            <div style={{ padding: '0.75rem 1rem', backgroundColor: 'var(--color-bg-secondary)', borderRadius: 'var(--radius-sm)', fontSize: '0.8125rem' }}>
              <span style={{ fontWeight: 600, color: 'var(--color-text-muted)', display: 'block', marginBottom: '0.25rem' }}>Notes:</span>
              <p style={{ margin: 0, color: 'var(--color-text-main)' }}>{order.notes}</p>
            </div>
          )}

          {/* Receive Confirmation Prompt */}
          {showReceiveConfirm && (
            <div
              style={{
                padding: '1rem',
                backgroundColor: 'rgba(16, 185, 129, 0.08)',
                border: '1px solid var(--color-emerald-500, #10b981)',
                borderRadius: 'var(--radius-md)'
              }}
            >
              <h5 style={{ margin: '0 0 0.5rem 0', color: 'var(--color-emerald-700, #047857)', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                <PackageCheck size={18} /> Confirm Goods Receiving
              </h5>
              <p style={{ margin: '0 0 0.75rem 0', fontSize: '0.8125rem', color: 'var(--color-text-main)' }}>
                Receiving will permanently update inventory levels by adding each line item quantity to current stock and creating immutable audit entries in the stock ledger. This action cannot be reversed.
              </p>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Button
                  variant="primary"
                  size="sm"
                  loading={isActionLoading}
                  onClick={() => handleAction(purchaseApi.receivePurchase, 'Goods received successfully and stock incremented in inventory ledger.')}
                >
                  Yes, Receive Shipment
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={isActionLoading}
                  onClick={() => setShowReceiveConfirm(false)}
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}

          {/* Line Items Table */}
          <div>
            <h4 style={{ margin: '0 0 0.5rem 0', fontSize: '0.9375rem', fontWeight: 600 }}>
              Order Line Items ({order.items.length})
            </h4>
            <div style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                <thead style={{ backgroundColor: 'var(--color-bg-secondary)', borderBottom: '1px solid var(--color-border)' }}>
                  <tr>
                    <th style={{ padding: '0.625rem 0.875rem', textAlign: 'left', fontWeight: 600 }}>Product</th>
                    <th style={{ padding: '0.625rem 0.875rem', textAlign: 'left', fontWeight: 600 }}>SKU</th>
                    <th style={{ padding: '0.625rem 0.875rem', textAlign: 'right', fontWeight: 600 }}>Qty</th>
                    <th style={{ padding: '0.625rem 0.875rem', textAlign: 'right', fontWeight: 600 }}>Unit Cost</th>
                    <th style={{ padding: '0.625rem 0.875rem', textAlign: 'right', fontWeight: 600 }}>Total</th>
                  </tr>
                </thead>
                <tbody>
                  {order.items.map((item, idx) => (
                    <tr key={item.id || idx} style={{ borderBottom: idx < order.items.length - 1 ? '1px solid var(--color-border)' : 'none' }}>
                      <td style={{ padding: '0.625rem 0.875rem', fontWeight: 500 }}>{item.productName}</td>
                      <td style={{ padding: '0.625rem 0.875rem', color: 'var(--color-text-muted)' }}>{item.productSku}</td>
                      <td style={{ padding: '0.625rem 0.875rem', textAlign: 'right', fontWeight: 600 }}>{item.quantity}</td>
                      <td style={{ padding: '0.625rem 0.875rem', textAlign: 'right' }}>${Number(item.unitCost).toFixed(2)}</td>
                      <td style={{ padding: '0.625rem 0.875rem', textAlign: 'right', fontWeight: 600, color: 'var(--color-primary)' }}>
                        ${Number(item.totalCost).toFixed(2)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Lifecycle Action Toolbar */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginTop: '0.5rem',
              paddingTop: '1rem',
              borderTop: '1px solid var(--color-border)'
            }}
          >
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              {/* DRAFT actions */}
              {order.status === 'DRAFT' && canManagePurchases && (
                <>
                  <Button
                    variant="primary"
                    size="sm"
                    icon={Send}
                    loading={isActionLoading}
                    onClick={() => handleAction(purchaseApi.submitPurchase, 'Purchase order submitted successfully to ORDERED status.')}
                  >
                    Submit Order
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    icon={Edit}
                    onClick={() => {
                      onClose();
                      if (onEditRequested) onEditRequested(order);
                    }}
                  >
                    Edit
                  </Button>
                  <Button
                    variant="danger"
                    size="sm"
                    icon={Ban}
                    loading={isActionLoading}
                    onClick={() => handleAction(purchaseApi.cancelPurchase, 'Purchase order cancelled.')}
                  >
                    Cancel
                  </Button>
                </>
              )}

              {/* ORDERED actions */}
              {order.status === 'ORDERED' && (
                <>
                  {canReceiveGoods && !showReceiveConfirm && (
                    <Button
                      variant="primary"
                      size="sm"
                      icon={PackageCheck}
                      onClick={() => setShowReceiveConfirm(true)}
                    >
                      Receive Shipment
                    </Button>
                  )}
                  {canManagePurchases && (
                    <Button
                      variant="danger"
                      size="sm"
                      icon={Ban}
                      loading={isActionLoading}
                      onClick={() => handleAction(purchaseApi.cancelPurchase, 'Purchase order cancelled.')}
                    >
                      Cancel Order
                    </Button>
                  )}
                </>
              )}

              {/* RECEIVED status info */}
              {order.status === 'RECEIVED' && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', color: 'var(--color-emerald-600, #059669)', fontSize: '0.8125rem', fontWeight: 600 }}>
                  <CheckCircle size={16} />
                  <span>Shipment received & inventory stock updated</span>
                </div>
              )}

              {/* CANCELLED status info */}
              {order.status === 'CANCELLED' && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', color: 'var(--color-danger)', fontSize: '0.8125rem', fontWeight: 600 }}>
                  <XCircle size={16} />
                  <span>Order cancelled</span>
                </div>
              )}
            </div>

            <Button variant="secondary" size="sm" onClick={onClose}>
              Close
            </Button>
          </div>
        </div>
      ) : null}
    </Modal>
  );
}
