import React, { useState } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import { useAuth } from '../../context/AuthContext';
import {
  ShoppingBag,
  CheckCircle2,
  XCircle,
  Edit,
  Trash2,
  Calendar,
  User,
  Phone,
  FileText,
  Clock,
  DollarSign,
  AlertTriangle
} from 'lucide-react';

export default function SaleDetailsModal({
  isOpen,
  onClose,
  sale,
  onEdit,
  onComplete,
  onCancel,
  onDelete,
  isProcessing = false
}) {
  const { user } = useAuth();
  const [confirmAction, setConfirmAction] = useState(null); // 'complete' | 'cancel' | 'delete'

  if (!isOpen || !sale) return null;

  const isDraft = sale.status === 'DRAFT';
  const isCompleted = sale.status === 'COMPLETED';
  const isCancelled = sale.status === 'CANCELLED';

  const canManage = user?.role === 'ADMIN' || user?.role === 'SALES_STAFF';

  const getStatusBadge = (status) => {
    switch (status) {
      case 'DRAFT':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-300 dark:border-amber-800">
            <Clock className="w-3.5 h-3.5" /> Draft Order
          </span>
        );
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800">
            <CheckCircle2 className="w-3.5 h-3.5" /> Completed & Stock Deducted
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300 border border-slate-300 dark:border-slate-700">
            <XCircle className="w-3.5 h-3.5" /> Cancelled
          </span>
        );
      default:
        return <span>{status}</span>;
    }
  };

  const handleExecuteConfirmedAction = () => {
    if (confirmAction === 'complete') {
      onComplete(sale.id);
    } else if (confirmAction === 'cancel') {
      onCancel(sale.id);
    } else if (confirmAction === 'delete') {
      onDelete(sale.id);
    }
    setConfirmAction(null);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={
        <div className="flex items-center gap-3">
          <span className="font-mono text-base font-bold text-slate-900 dark:text-slate-100">
            {sale.orderNumber}
          </span>
          {getStatusBadge(sale.status)}
        </div>
      }
      maxWidth="max-w-4xl"
    >
      <div className="space-y-6">
        {/* Confirmation Modal Overlay Banner if action initiated */}
        {confirmAction && (
          <div className="p-4 rounded-xl border border-amber-300 bg-amber-50 dark:bg-amber-950/40 dark:border-amber-800/80 animate-in fade-in duration-200">
            <div className="flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-amber-600 dark:text-amber-400 mt-0.5 flex-shrink-0" />
              <div className="flex-1">
                <h4 className="text-sm font-semibold text-amber-900 dark:text-amber-200">
                  {confirmAction === 'complete' && 'Confirm Sale Completion & Stock Deduction'}
                  {confirmAction === 'cancel' && 'Confirm Order Cancellation'}
                  {confirmAction === 'delete' && 'Confirm Draft Order Deletion'}
                </h4>
                <p className="text-xs text-amber-800/90 dark:text-amber-300/90 mt-1">
                  {confirmAction === 'complete' &&
                    'This action will atomically deduct product inventory, record immutable SALE_DEDUCTION transactions in the ledger, and mark the order as COMPLETED. This operation cannot be reversed.'}
                  {confirmAction === 'cancel' &&
                    'This will mark the sales order as CANCELLED. No inventory will be deducted.'}
                  {confirmAction === 'delete' &&
                    'This draft order and all its line items will be permanently deleted.'}
                </p>
                <div className="flex items-center gap-2 mt-3">
                  <Button
                    size="sm"
                    variant={confirmAction === 'delete' ? 'danger' : 'primary'}
                    onClick={handleExecuteConfirmedAction}
                    disabled={isProcessing}
                    className={
                      confirmAction === 'complete'
                        ? 'bg-emerald-600 hover:bg-emerald-700 text-white font-medium'
                        : ''
                    }
                  >
                    {isProcessing ? 'Processing...' : 'Yes, Proceed'}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setConfirmAction(null)}
                    disabled={isProcessing}
                  >
                    Cancel
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Customer & Order Metadata Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 bg-slate-50 dark:bg-slate-800/40 p-4 rounded-xl border border-slate-200 dark:border-slate-700/60 text-xs">
          <div className="space-y-1">
            <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
              Customer Info
            </span>
            <p className="font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-1.5">
              <User className="w-3.5 h-3.5 text-slate-400" />
              {sale.customerName || 'Walk-in Customer'}
            </p>
            {sale.customerContact && (
              <p className="text-slate-600 dark:text-slate-400 flex items-center gap-1.5">
                <Phone className="w-3.5 h-3.5 text-slate-400" />
                {sale.customerContact}
              </p>
            )}
          </div>

          <div className="space-y-1">
            <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
              Created Timeline
            </span>
            <p className="text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5 text-slate-400" />
              {sale.createdAt ? new Date(sale.createdAt).toLocaleDateString() : '—'}
            </p>
            <p className="text-slate-500 text-[11px]">
              Created by: <span className="font-medium text-slate-700 dark:text-slate-300">{sale.createdBy || 'System'}</span>
            </p>
          </div>

          <div className="space-y-1">
            <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
              Completion Audit
            </span>
            {isCompleted ? (
              <>
                <p className="text-emerald-600 dark:text-emerald-400 font-semibold flex items-center gap-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  {sale.completedAt ? new Date(sale.completedAt).toLocaleString() : 'Completed'}
                </p>
                <p className="text-slate-500 text-[11px]">
                  Completed by: <span className="font-medium text-slate-700 dark:text-slate-300">{sale.completedBy || '—'}</span>
                </p>
              </>
            ) : (
              <p className="text-slate-400 dark:text-slate-600 italic">Pending completion</p>
            )}
          </div>
        </div>

        {/* Notes */}
        {sale.notes && (
          <div className="bg-slate-50 dark:bg-slate-800/30 p-3 rounded-lg border border-slate-200 dark:border-slate-800 text-xs">
            <span className="font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-1.5 mb-1">
              <FileText className="w-3.5 h-3.5 text-slate-400" /> Order Notes
            </span>
            <p className="text-slate-600 dark:text-slate-400 whitespace-pre-wrap">{sale.notes}</p>
          </div>
        )}

        {/* Line Items Table */}
        <div className="space-y-2">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
            <ShoppingBag className="w-3.5 h-3.5 text-slate-400" />
            Purchased Line Items
          </h4>
          <div className="border border-slate-200 dark:border-slate-700/80 rounded-xl overflow-hidden shadow-sm">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-semibold uppercase tracking-wider">
                <tr>
                  <th className="py-2.5 px-3">Product Name</th>
                  <th className="py-2.5 px-3">SKU</th>
                  <th className="py-2.5 px-3 text-right">Quantity</th>
                  <th className="py-2.5 px-3 text-right">Unit Price</th>
                  <th className="py-2.5 px-3 text-right">Line Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-700/60 bg-white dark:bg-slate-900">
                {sale.items?.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                    <td className="py-2.5 px-3 font-medium text-slate-900 dark:text-slate-100">
                      {item.productName}
                    </td>
                    <td className="py-2.5 px-3 font-mono text-[11px] text-slate-500">
                      {item.productSku}
                    </td>
                    <td className="py-2.5 px-3 text-right font-semibold text-slate-700 dark:text-slate-300">
                      {item.quantity}
                    </td>
                    <td className="py-2.5 px-3 text-right text-slate-600 dark:text-slate-400">
                      ${Number(item.unitPrice).toFixed(2)}
                    </td>
                    <td className="py-2.5 px-3 text-right font-bold text-slate-900 dark:text-slate-100">
                      ${Number(item.lineTotal).toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Total Footer */}
            <div className="bg-slate-50 dark:bg-slate-800/80 px-4 py-3 border-t border-slate-200 dark:border-slate-700 flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-600 dark:text-slate-400 flex items-center gap-1">
                <DollarSign className="w-3.5 h-3.5 text-emerald-500" /> Total Sale Amount
              </span>
              <span className="text-lg font-bold text-emerald-600 dark:text-emerald-400">
                ${Number(sale.totalAmount).toFixed(2)}
              </span>
            </div>
          </div>
        </div>

        {/* Modal Action Buttons */}
        <div className="flex items-center justify-between pt-4 border-t border-slate-200 dark:border-slate-800">
          <Button variant="outline" onClick={onClose} disabled={isProcessing}>
            Close
          </Button>

          {isDraft && canManage && (
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setConfirmAction('delete')}
                disabled={isProcessing}
                className="text-rose-600 border-rose-300 hover:bg-rose-50 dark:hover:bg-rose-950/40"
              >
                <Trash2 className="w-3.5 h-3.5 mr-1" /> Delete Draft
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setConfirmAction('cancel')}
                disabled={isProcessing}
              >
                <XCircle className="w-3.5 h-3.5 mr-1" /> Cancel Order
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  onClose();
                  onEdit(sale);
                }}
                disabled={isProcessing}
              >
                <Edit className="w-3.5 h-3.5 mr-1" /> Edit Draft
              </Button>
              <Button
                size="sm"
                onClick={() => setConfirmAction('complete')}
                disabled={isProcessing}
                className="bg-emerald-600 hover:bg-emerald-700 text-white font-medium shadow-md shadow-emerald-500/20"
              >
                <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Complete Sale & Deduct Stock
              </Button>
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
}
