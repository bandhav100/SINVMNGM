import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import Layout from './components/common/Layout';
import LoginPage from './pages/auth/LoginPage';
import SetupAdminPage from './pages/auth/SetupAdminPage';
import ProductsPage from './pages/products/ProductsPage';
import SuppliersPage from './pages/suppliers/SuppliersPage';
import UsersPage from './pages/users/UsersPage';
import ModulePlaceholderPage from './pages/common/ModulePlaceholderPage';
import {
  LayoutDashboard,
  Layers,
  Truck,
  ShoppingCart,
  TrendingUp,
  Brain,
  Bell,
  FileBarChart,
  Users,
  Settings
} from 'lucide-react';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public Authentication Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/setup-admin" element={<SetupAdminPage />} />

          {/* Protected Application Layout */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/products" replace />} />
            
            {/* Products (All 4 roles) */}
            <Route
              path="products"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <ProductsPage />
                </ProtectedRoute>
              }
            />

            {/* Suppliers (Admin, Inventory Manager, Purchase Manager) */}
            <Route
              path="suppliers"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER']}>
                  <SuppliersPage />
                </ProtectedRoute>
              }
            />

            {/* Users & Roles (Admin Only) */}
            <Route
              path="users"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <UsersPage />
                </ProtectedRoute>
              }
            />

            {/* Module Placeholders */}
            <Route
              path="dashboard"
              element={
                <ModulePlaceholderPage
                  title="Business Metrics Dashboard"
                  icon={LayoutDashboard}
                  phase="Phase 8"
                  description="Real-time KPI overview featuring total products, stock valuations, low/out-of-stock counts, and recent transaction trends."
                  features={[
                    'Live stock count & total valuation metrics',
                    'Recent purchase orders & sales activity feed',
                    'Low-stock warning radar with 1-click replenishment',
                    'Role-tailored metrics & charts'
                  ]}
                />
              }
            />

            <Route
              path="inventory"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER']}>
                  <ModulePlaceholderPage
                    title="Inventory & Stock Ledger"
                    icon={Layers}
                    phase="Phase 5"
                    description="Authoritative real-time stock levels, adjustment workflows with audit reasons, and immutable stock movement tracking."
                    features={[
                      'Manual stock adjustments with required reasons',
                      'Complete stock movement audit log (Receive, Sold, Adjusted)',
                      'Automatic concurrency & negative-stock protection',
                      'Synchronized ledger with purchases and sales'
                    ]}
                  />
                </ProtectedRoute>
              }
            />

            <Route
              path="purchases"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'PURCHASE_MANAGER']}>
                  <ModulePlaceholderPage
                    title="Purchase Orders"
                    icon={ShoppingCart}
                    phase="Phase 6"
                    description="Full purchasing lifecycle: Order creation, supplier selection, approval workflows, and automated stock receipt."
                    features={[
                      'Draft purchase order creation with line items',
                      'Multi-stage approval workflow (Draft → Pending → Approved)',
                      'Receive shipments and automatically increment stock',
                      'Prevent duplicate receipts and track purchase costs'
                    ]}
                  />
                </ProtectedRoute>
              }
            />

            <Route
              path="sales"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'SALES_STAFF']}>
                  <ModulePlaceholderPage
                    title="Sales Transactions"
                    icon={TrendingUp}
                    phase="Phase 7"
                    description="Order processing, stock availability verification, instant checkout, and automatic stock deductions."
                    features={[
                      'Point-of-sale style order creation with live inventory check',
                      'Strict oversell prevention & transactional stock deduction',
                      'Automatic tax, line item, and grand total calculation',
                      'Permitted order cancellation with stock reversal logging'
                    ]}
                  />
                </ProtectedRoute>
              }
            />

            <Route
              path="insights"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER']}>
                  <ModulePlaceholderPage
                    title="Smart Insights & Demand Forecasting"
                    icon={Brain}
                    phase="Phase 9"
                    description="Intelligent inventory analysis powered by dataset history, identifying velocity trends and recommending replenishment."
                    features={[
                      'Fast-moving, slow-moving, and dead-stock automated classification',
                      'Historical demand trend analysis across regions',
                      'Recommended reorder quantities with transparent rules',
                      'Stockout risk prevention alerts before items deplete'
                    ]}
                  />
                </ProtectedRoute>
              }
            />

            <Route
              path="alerts"
              element={
                <ModulePlaceholderPage
                  title="Alerts & Notification Center"
                  icon={Bell}
                  phase="Phase 8"
                  description="Active notifications for low inventory, out-of-stock items, and purchasing warnings."
                  features={[
                    'Low-stock threshold triggers based on reorder levels',
                    'Out-of-stock emergency alerts with vendor links',
                    'Duplicate alert suppression for unresolved conditions',
                    'Acknowledgement & resolution workflow'
                  ]}
                />
              }
            />

            <Route
              path="reports"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER']}>
                  <ModulePlaceholderPage
                    title="Business Reports & Analytics"
                    icon={FileBarChart}
                    phase="Phase 9"
                    description="Backend-generated reports covering stock valuation, movement audits, sales breakdowns, and CSV export."
                    features={[
                      'Inventory valuation summary (Cost vs Selling price)',
                      'Sales & profit margin breakdown by category & region',
                      'Full stock movement ledger report with date range filters',
                      '1-click CSV download for offline accounting'
                    ]}
                  />
                </ProtectedRoute>
              }
            />

            <Route
              path="settings"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <ModulePlaceholderPage
                    title="System Settings"
                    icon={Settings}
                    phase="Phase 10"
                    description="Business profile, default reorder thresholds, inventory alert parameters, and administrative controls."
                    features={[
                      'Company profile & currency settings',
                      'Global default reorder levels & stock warning thresholds',
                      'System health & PostgreSQL database status',
                      'Audit log inspection'
                    ]}
                  />
                </ProtectedRoute>
              }
            />

            <Route path="*" element={<Navigate to="/products" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
