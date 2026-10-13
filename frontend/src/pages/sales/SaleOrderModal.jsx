import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import AlertBanner from '../../components/common/AlertBanner';
import { productApi } from '../../api/productApi';
import { Plus, Trash2, ShoppingBag, Calculator, AlertCircle, AlertTriangle } from 'lucide-react';

export default function SaleOrderModal({
  isOpen,
  onClose,
  onSubmit,
  initialData = null,
  isSubmitting = false,
  backendErrors = null
}) {
  const isEditing = Boolean(initialData);

  const [customerName, setCustomerName] = useState('');
  const [customerContact, setCustomerContact] = useState('');
  const [notes, setNotes] = useState('');
  const [items, setItems] = useState([
    { productId: '', quantity: 1, unitPrice: '' }
  ]);

  const [products, setProducts] = useState([]);
  const [isLoadingProducts, setIsLoadingProducts] = useState(false);
  const [clientErrors, setClientErrors] = useState({});

  useEffect(() => {
    if (!isOpen) return;

    const loadProducts = async () => {
      setIsLoadingProducts(true);
      try {
        const productsRes = await productApi.getProducts({ size: 200 });
        setProducts(productsRes.content || []);
      } catch (err) {
        console.error('Failed to load products for sale order:', err);
      } finally {
        setIsLoadingProducts(false);
      }
    };

    loadProducts();

    if (initialData) {
      setCustomerName(initialData.customerName || '');
      setCustomerContact(initialData.customerContact || '');
      setNotes(initialData.notes || '');
      if (initialData.items && initialData.items.length > 0) {
        setItems(
          initialData.items.map((i) => ({
            productId: String(i.productId),
            quantity: i.quantity,
            unitPrice: i.unitPrice != null ? String(i.unitPrice) : ''
          }))
        );
      } else {
        setItems([{ productId: '', quantity: 1, unitPrice: '' }]);
      }
    } else {
      setCustomerName('');
      setCustomerContact('');
      setNotes('');
      setItems([{ productId: '', quantity: 1, unitPrice: '' }]);
    }
    setClientErrors({});
  }, [isOpen, initialData]);

  const handleProductChange = (index, prodId) => {
    const selected = products.find((p) => String(p.id) === String(prodId));
    const newItems = [...items];
    newItems[index].productId = prodId;
    if (selected && (!newItems[index].unitPrice || newItems[index].unitPrice === '')) {
      newItems[index].unitPrice = String(selected.price);
    }
    setItems(newItems);
  };

  const handleQuantityChange = (index, val) => {
    const newItems = [...items];
    const qty = parseInt(val, 10);
    newItems[index].quantity = isNaN(qty) ? '' : qty;
    setItems(newItems);
  };

  const handleUnitPriceChange = (index, val) => {
    const newItems = [...items];
    newItems[index].unitPrice = val;
    setItems(newItems);
  };

  const handleAddItem = () => {
    setItems([...items, { productId: '', quantity: 1, unitPrice: '' }]);
  };

  const handleRemoveItem = (index) => {
    if (items.length <= 1) return;
    setItems(items.filter((_, idx) => idx !== index));
  };

  const calculateLineTotal = (item) => {
    const qty = Number(item.quantity) || 0;
    const price = Number(item.unitPrice) || 0;
    return (qty * price).toFixed(2);
  };

  const calculateTotalAmount = () => {
    return items
      .reduce((sum, item) => sum + (Number(item.quantity) || 0) * (Number(item.unitPrice) || 0), 0)
      .toFixed(2);
  };

  const validate = () => {
    const errors = {};
    if (!items || items.length === 0) {
      errors.items = 'Sales order must contain at least one product.';
    }

    const seenProducts = new Set();
    items.forEach((item, idx) => {
      if (!item.productId) {
        errors[`item_${idx}_product`] = 'Product selection is required';
      } else if (seenProducts.has(item.productId)) {
        errors[`item_${idx}_product`] = 'Duplicate product selected';
      } else {
        seenProducts.add(item.productId);
      }

      if (!item.quantity || Number(item.quantity) <= 0) {
        errors[`item_${idx}_quantity`] = 'Quantity must be > 0';
      }

      if (item.unitPrice !== '' && Number(item.unitPrice) < 0) {
        errors[`item_${idx}_price`] = 'Price cannot be negative';
      }
    });

    setClientErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;

    const payload = {
      customerName: customerName.trim() || null,
      customerContact: customerContact.trim() || null,
      notes: notes.trim() || null,
      items: items.map((i) => ({
        productId: Number(i.productId),
        quantity: Number(i.quantity),
        unitPrice: i.unitPrice !== '' ? Number(i.unitPrice) : null
      }))
    };

    onSubmit(payload);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? `Edit Sale Order ${initialData?.orderNumber || ''}` : 'Create New Sale Order'}
      maxWidth="max-w-4xl"
    >
      <form onSubmit={handleSubmit} className="space-y-6">
        {backendErrors && typeof backendErrors === 'string' && (
          <AlertBanner type="error" message={backendErrors} />
        )}

        {/* Customer Information */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-50 dark:bg-slate-800/50 p-4 rounded-xl border border-slate-200 dark:border-slate-700/60">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1.5">
              Customer Name
            </label>
            <Input
              type="text"
              placeholder="e.g. Acme Corp / Walk-in Customer"
              value={customerName}
              onChange={(e) => setCustomerName(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1.5">
              Customer Contact
            </label>
            <Input
              type="text"
              placeholder="e.g. +1 555-0199 or email"
              value={customerContact}
              onChange={(e) => setCustomerContact(e.target.value)}
            />
          </div>
        </div>

        {/* Order Items Table */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <ShoppingBag className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
              Order Items ({items.length})
            </h3>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleAddItem}
              className="flex items-center gap-1.5 text-xs font-semibold border-emerald-300 dark:border-emerald-700 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-950/40"
            >
              <Plus className="w-3.5 h-3.5" />
              Add Product Item
            </Button>
          </div>

          {clientErrors.items && (
            <p className="text-xs text-rose-500 font-medium flex items-center gap-1">
              <AlertCircle className="w-3.5 h-3.5" />
              {clientErrors.items}
            </p>
          )}

          <div className="border border-slate-200 dark:border-slate-700/80 rounded-xl overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-semibold uppercase tracking-wider">
                  <tr>
                    <th className="py-2.5 px-3">Product</th>
                    <th className="py-2.5 px-3 w-28">Available</th>
                    <th className="py-2.5 px-3 w-28">Quantity</th>
                    <th className="py-2.5 px-3 w-32">Unit Price (₹)</th>
                    <th className="py-2.5 px-3 w-32 text-right">Line Total</th>
                    <th className="py-2.5 px-2 w-12 text-center"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-700/60 bg-white dark:bg-slate-900">
                  {items.map((item, idx) => {
                    const selectedProd = products.find((p) => String(p.id) === String(item.productId));
                    const stock = selectedProd ? selectedProd.quantity : null;
                    const isExceedingStock = stock !== null && Number(item.quantity) > stock;

                    return (
                      <tr key={idx} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                        {/* Product Select */}
                        <td className="py-2.5 px-3">
                          <select
                            value={item.productId}
                            onChange={(e) => handleProductChange(idx, e.target.value)}
                            disabled={isLoadingProducts}
                            className={`w-full text-xs rounded-lg px-2.5 py-1.5 border bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                              clientErrors[`item_${idx}_product`]
                                ? 'border-rose-500'
                                : 'border-slate-300 dark:border-slate-600'
                            }`}
                          >
                            <option value="">Select product...</option>
                            {products.map((p) => (
                              <option key={p.id} value={p.id}>
                                {p.name} ({p.sku}) — Stock: {p.quantity}
                              </option>
                            ))}
                          </select>
                          {clientErrors[`item_${idx}_product`] && (
                            <span className="text-[11px] text-rose-500 block mt-0.5">
                              {clientErrors[`item_${idx}_product`]}
                            </span>
                          )}
                        </td>

                        {/* Available Stock Indicator */}
                        <td className="py-2.5 px-3">
                          {selectedProd ? (
                            <span
                              className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                                selectedProd.quantity <= 0
                                  ? 'bg-rose-100 text-rose-700 dark:bg-rose-950/60 dark:text-rose-400'
                                  : selectedProd.quantity < 10
                                  ? 'bg-amber-100 text-amber-700 dark:bg-amber-950/60 dark:text-amber-400'
                                  : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-400'
                              }`}
                            >
                              {selectedProd.quantity} units
                            </span>
                          ) : (
                            <span className="text-slate-400 dark:text-slate-600 text-xs">—</span>
                          )}
                        </td>

                        {/* Quantity Input */}
                        <td className="py-2.5 px-3">
                          <input
                            type="number"
                            min="1"
                            value={item.quantity}
                            onChange={(e) => handleQuantityChange(idx, e.target.value)}
                            className={`w-full text-xs rounded-lg px-2 py-1.5 border bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                              clientErrors[`item_${idx}_quantity`]
                                ? 'border-rose-500'
                                : 'border-slate-300 dark:border-slate-600'
                            }`}
                          />
                          {isExceedingStock && (
                            <span className="text-[10px] text-amber-600 dark:text-amber-400 flex items-center gap-0.5 mt-0.5 font-medium">
                              <AlertTriangle className="w-3 h-3 flex-shrink-0" /> Exceeds stock
                            </span>
                          )}
                          {clientErrors[`item_${idx}_quantity`] && (
                            <span className="text-[11px] text-rose-500 block mt-0.5">
                              {clientErrors[`item_${idx}_quantity`]}
                            </span>
                          )}
                        </td>

                        {/* Unit Selling Price */}
                        <td className="py-2.5 px-3">
                          <input
                            type="number"
                            step="0.01"
                            min="0"
                            placeholder="0.00"
                            value={item.unitPrice}
                            onChange={(e) => handleUnitPriceChange(idx, e.target.value)}
                            className={`w-full text-xs rounded-lg px-2 py-1.5 border bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
                              clientErrors[`item_${idx}_price`]
                                ? 'border-rose-500'
                                : 'border-slate-300 dark:border-slate-600'
                            }`}
                          />
                          {clientErrors[`item_${idx}_price`] && (
                            <span className="text-[11px] text-rose-500 block mt-0.5">
                              {clientErrors[`item_${idx}_price`]}
                            </span>
                          )}
                        </td>

                        {/* Line Total */}
                        <td className="py-2.5 px-3 text-right font-semibold text-slate-900 dark:text-slate-100">
                          ₹{calculateLineTotal(item)}
                        </td>

                        {/* Remove Action */}
                        <td className="py-2.5 px-2 text-center">
                          <button
                            type="button"
                            onClick={() => handleRemoveItem(idx)}
                            disabled={items.length <= 1}
                            className={`p-1 rounded text-slate-400 hover:text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors ${
                              items.length <= 1 ? 'opacity-30 cursor-not-allowed' : ''
                            }`}
                            title="Remove line item"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Total Footer Banner */}
            <div className="bg-slate-50 dark:bg-slate-800/80 px-4 py-3 border-t border-slate-200 dark:border-slate-700 flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
                <Calculator className="w-3.5 h-3.5" /> Calculated Order Total
              </span>
              <div className="text-right">
                <span className="text-lg font-bold text-emerald-600 dark:text-emerald-400 tracking-tight">
                  ₹{calculateTotalAmount()}
                </span>
                <span className="text-[10px] text-slate-400 dark:text-slate-500 block">INR</span>
              </div>
            </div>
          </div>
        </div>

        {/* Notes */}
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1.5">
            Internal Notes & Instructions (Optional)
          </label>
          <textarea
            rows="2"
            placeholder="Special delivery notes, customer PO reference, payment instructions..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="w-full text-xs rounded-xl p-3 border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 resize-none"
          />
        </div>

        {/* Modal Actions */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200 dark:border-slate-800">
          <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={isSubmitting || isLoadingProducts}
            className="bg-emerald-600 hover:bg-emerald-700 text-white font-medium shadow-md shadow-emerald-500/20"
          >
            {isSubmitting ? 'Saving...' : isEditing ? 'Save Changes' : 'Create Draft Sale Order'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
