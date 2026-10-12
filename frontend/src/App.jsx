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
import PurchasesPage from './pages/purchases/PurchasesPage';
import SalesPage from './pages/sales/SalesPage';
import SmartInsightsPage from './pages/insights/SmartInsightsPage';
import AlertsPage from './pages/alerts/AlertsPage';
import ReportsPage from './pages/reports/ReportsPage';
import SettingsPage from './pages/settings/SettingsPage';
import DashboardPage from './pages/dashboard/DashboardPage';
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
            <Route index element={<Navigate to="/dashboard" replace />} />
            
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

            {/* Dashboard (All 4 roles) */}
            <Route
              path="dashboard"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <DashboardPage />
                </ProtectedRoute>
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
                <ProtectedRoute allowedRoles={['ADMIN', 'PURCHASE_MANAGER', 'INVENTORY_MANAGER']}>
                  <PurchasesPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="sales"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <SalesPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="insights"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <SmartInsightsPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="alerts"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <AlertsPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="reports"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <ReportsPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="settings"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <SettingsPage />
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
