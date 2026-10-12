import React, { useState, useEffect } from 'react';
import { settingsApi } from '../../api/settingsApi';
import { useAuth } from '../../context/AuthContext';
import Button from '../../components/common/Button';
import AlertBanner from '../../components/common/AlertBanner';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  Settings,
  Building,
  DollarSign,
  Globe,
  Bell,
  ShieldCheck,
  Save,
  RotateCcw,
  Clock,
  User,
  Info
} from 'lucide-react';

export default function SettingsPage() {
  const { user } = useAuth();

  const [settings, setSettings] = useState(null);
  const [formData, setFormData] = useState({
    companyName: '',
    companyEmail: '',
    companyPhone: '',
    currency: 'USD',
    timezone: 'UTC',
    defaultReorderThreshold: 10,
    lowStockAlertsEnabled: true,
    outOfStockAlertsEnabled: true,
    allowNegativeStock: false,
    requirePurchaseApproval: false
  });

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [errorBanner, setErrorBanner] = useState(null);
  const [successBanner, setSuccessBanner] = useState(null);

  useEffect(() => {
    fetchSettings();
  }, []);

  async function fetchSettings() {
    setIsLoading(true);
    setErrorBanner(null);
    try {
      const data = await settingsApi.getSettings();
      setSettings(data);
      setFormData({
        companyName: data.companyName || '',
        companyEmail: data.companyEmail || '',
        companyPhone: data.companyPhone || '',
        currency: data.currency || 'USD',
        timezone: data.timezone || 'UTC',
        defaultReorderThreshold: data.defaultReorderThreshold ?? 10,
        lowStockAlertsEnabled: data.lowStockAlertsEnabled ?? true,
        outOfStockAlertsEnabled: data.outOfStockAlertsEnabled ?? true,
        allowNegativeStock: data.allowNegativeStock ?? false,
        requirePurchaseApproval: data.requirePurchaseApproval ?? false
      });
    } catch (err) {
      console.error('Error fetching settings:', err);
      setErrorBanner(err.response?.data?.message || 'Failed to load system settings.');
    } finally {
      setIsLoading(false);
    }
  }

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSaving(true);
    setErrorBanner(null);
    setSuccessBanner(null);

    try {
      const payload = {
        companyName: formData.companyName.trim(),
        companyEmail: formData.companyEmail ? formData.companyEmail.trim() : null,
        companyPhone: formData.companyPhone ? formData.companyPhone.trim() : null,
        currency: formData.currency.trim().toUpperCase(),
        timezone: formData.timezone.trim(),
        defaultReorderThreshold: parseInt(formData.defaultReorderThreshold, 10) || 0,
        lowStockAlertsEnabled: !!formData.lowStockAlertsEnabled,
        outOfStockAlertsEnabled: !!formData.outOfStockAlertsEnabled,
        allowNegativeStock: !!formData.allowNegativeStock,
        requirePurchaseApproval: !!formData.requirePurchaseApproval
      };

      const updated = await settingsApi.updateSettings(payload);
      setSettings(updated);
      setSuccessBanner('System settings successfully saved.');
    } catch (err) {
      console.error('Error saving settings:', err);
      const msg = err.response?.data?.message || 'Failed to save settings. Please verify inputs.';
      setErrorBanner(msg);
    } finally {
      setIsSaving(false);
    }
  };

  const handleReset = () => {
    if (settings) {
      setFormData({
        companyName: settings.companyName || '',
        companyEmail: settings.companyEmail || '',
        companyPhone: settings.companyPhone || '',
        currency: settings.currency || 'USD',
        timezone: settings.timezone || 'UTC',
        defaultReorderThreshold: settings.defaultReorderThreshold ?? 10,
        lowStockAlertsEnabled: settings.lowStockAlertsEnabled ?? true,
        outOfStockAlertsEnabled: settings.outOfStockAlertsEnabled ?? true,
        allowNegativeStock: settings.allowNegativeStock ?? false,
        requirePurchaseApproval: settings.requirePurchaseApproval ?? false
      });
      setErrorBanner(null);
      setSuccessBanner(null);
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

  if (isLoading) {
    return (
      <div className="py-24 flex flex-col items-center justify-center">
        <LoadingSpinner size="lg" />
        <p className="mt-4 text-sm text-gray-500 font-medium">Loading system configurations...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-600 rounded-xl border border-blue-100 shadow-sm">
            <Settings className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 tracking-tight">System Settings</h1>
            <p className="text-sm text-gray-500">
              Manage organization parameters, notification policies, and operational inventory controls.
            </p>
          </div>
        </div>

        {settings && (
          <div className="text-xs text-gray-500 flex flex-col sm:items-end">
            <div className="flex items-center gap-1.5 font-medium">
              <Clock className="w-3.5 h-3.5 text-gray-400" />
              Last updated: {formatDateTime(settings.updatedAt)}
            </div>
            <div className="flex items-center gap-1 text-gray-400">
              <User className="w-3 h-3" />
              By: {settings.updatedBy || 'System'}
            </div>
          </div>
        )}
      </div>

      {errorBanner && (
        <AlertBanner
          type="error"
          message={errorBanner}
          onClose={() => setErrorBanner(null)}
        />
      )}

      {successBanner && (
        <AlertBanner
          type="success"
          message={successBanner}
          onClose={() => setSuccessBanner(null)}
        />
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Section 1: Organization & Profile */}
        <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-gray-100">
            <Building className="w-5 h-5 text-blue-600" />
            <h2 className="text-base font-semibold text-gray-900">Organization Profile</h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-gray-700">Company Name *</label>
              <input
                type="text"
                name="companyName"
                value={formData.companyName}
                onChange={handleChange}
                required
                maxLength={100}
                className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                placeholder="e.g. Acme Corp"
              />
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-gray-700">Official Contact Email</label>
              <input
                type="email"
                name="companyEmail"
                value={formData.companyEmail}
                onChange={handleChange}
                maxLength={100}
                className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                placeholder="ops@company.com"
              />
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-gray-700">Contact Phone Number</label>
              <input
                type="text"
                name="companyPhone"
                value={formData.companyPhone}
                onChange={handleChange}
                maxLength={50}
                className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                placeholder="+1 (555) 019-2834"
              />
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-gray-700">Base Currency *</label>
              <select
                name="currency"
                value={formData.currency}
                onChange={handleChange}
                required
                className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="USD">USD - United States Dollar ($)</option>
                <option value="EUR">EUR - Euro (€)</option>
                <option value="GBP">GBP - British Pound (£)</option>
                <option value="CAD">CAD - Canadian Dollar ($)</option>
                <option value="AUD">AUD - Australian Dollar ($)</option>
                <option value="INR">INR - Indian Rupee (₹)</option>
              </select>
            </div>

            <div className="space-y-1 md:col-span-2">
              <label className="text-xs font-semibold text-gray-700">System Timezone *</label>
              <select
                name="timezone"
                value={formData.timezone}
                onChange={handleChange}
                required
                className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="UTC">UTC (Coordinated Universal Time)</option>
                <option value="America/New_York">America/New_York (Eastern Time)</option>
                <option value="America/Chicago">America/Chicago (Central Time)</option>
                <option value="America/Los_Angeles">America/Los_Angeles (Pacific Time)</option>
                <option value="Europe/London">Europe/London (GMT/BST)</option>
                <option value="Asia/Kolkata">Asia/Kolkata (IST)</option>
                <option value="Asia/Singapore">Asia/Singapore (SGT)</option>
              </select>
            </div>
          </div>
        </div>

        {/* Section 2: Stock & Inventory Policies */}
        <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-gray-100">
            <Bell className="w-5 h-5 text-indigo-600" />
            <h2 className="text-base font-semibold text-gray-900">Inventory Alert & Threshold Rules</h2>
          </div>

          <div className="space-y-4">
            <div className="space-y-1 max-w-sm">
              <label className="text-xs font-semibold text-gray-700">
                Default Safety Stock Reorder Threshold *
              </label>
              <input
                type="number"
                name="defaultReorderThreshold"
                value={formData.defaultReorderThreshold}
                onChange={handleChange}
                min={0}
                required
                className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
              <p className="text-xs text-gray-400">
                Applied to newly cataloged items when individual thresholds are omitted.
              </p>
            </div>

            <div className="space-y-3 pt-2">
              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  name="lowStockAlertsEnabled"
                  checked={formData.lowStockAlertsEnabled}
                  onChange={handleChange}
                  className="mt-1 h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <div>
                  <span className="text-sm font-medium text-gray-900">
                    Enable Low-Stock Trigger Alerts
                  </span>
                  <p className="text-xs text-gray-500">
                    Flags items whose live quantity drops to or below the configured reorder threshold.
                  </p>
                </div>
              </label>

              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  name="outOfStockAlertsEnabled"
                  checked={formData.outOfStockAlertsEnabled}
                  onChange={handleChange}
                  className="mt-1 h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <div>
                  <span className="text-sm font-medium text-gray-900">
                    Enable Critical Out-of-Stock Alerts
                  </span>
                  <p className="text-xs text-gray-500">
                    Prioritizes zero-stock products in the system notification header and alerts dashboard.
                  </p>
                </div>
              </label>
            </div>
          </div>
        </div>

        {/* Section 3: Governance & Operational Controls */}
        <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-gray-100">
            <ShieldCheck className="w-5 h-5 text-emerald-600" />
            <h2 className="text-base font-semibold text-gray-900">Operational Controls & Governance</h2>
          </div>

          <div className="space-y-4">
            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                name="allowNegativeStock"
                checked={formData.allowNegativeStock}
                onChange={handleChange}
                className="mt-1 h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <div>
                <span className="text-sm font-medium text-gray-900">
                  Allow Negative Stock Fulfillment
                </span>
                <p className="text-xs text-gray-500">
                  When disabled (recommended), sales orders are prevented from completing if available stock is insufficient.
                </p>
              </div>
            </label>

            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                name="requirePurchaseApproval"
                checked={formData.requirePurchaseApproval}
                onChange={handleChange}
                className="mt-1 h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <div>
                <span className="text-sm font-medium text-gray-900">
                  Require Purchase Order Admin Approval
                </span>
                <p className="text-xs text-gray-500">
                  Requires administrative sign-off before purchase orders transition into confirmed vendor orders.
                </p>
              </div>
            </label>
          </div>
        </div>

        {/* Actions bar */}
        <div className="flex items-center justify-end gap-3 pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={handleReset}
            disabled={isSaving}
            className="flex items-center gap-2"
          >
            <RotateCcw className="w-4 h-4" />
            Reset
          </Button>

          <Button
            type="submit"
            variant="primary"
            disabled={isSaving}
            className="flex items-center gap-2"
          >
            {isSaving ? (
              <LoadingSpinner size="sm" />
            ) : (
              <Save className="w-4 h-4" />
            )}
            Save Configuration
          </Button>
        </div>
      </form>
    </div>
  );
}
