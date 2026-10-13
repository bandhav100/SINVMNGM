import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import Layout from './components/common/Layout';
import LoadingSpinner from './components/common/LoadingSpinner';
import LandingPage from './pages/landing/LandingPage';
import LoginPage from './pages/auth/LoginPage';
import SetupAdminPage from './pages/auth/SetupAdminPage';
import CreateInventoryPage from './pages/auth/CreateInventoryPage';
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
import InventoryPage from './pages/inventory/InventoryPage';

function PublicLandingOrRedirect() {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        width: '100vw',
        backgroundColor: '#0b0f19',
        color: '#ffffff'
      }}>
        <LoadingSpinner text="Authenticating session..." />
      </div>
    );
  }

  // Authenticated users opening '/' are automatically redirected to the dashboard
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  // Unauthenticated visitors see the public landing page
  return <LandingPage />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public Landing & Authentication Routes */}
          <Route path="/" element={<PublicLandingOrRedirect />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/setup-admin" element={<SetupAdminPage />} />
          <Route path="/create-inventory" element={<CreateInventoryPage />} />

          {/* Protected Application Layout (For Authenticated Users) */}
          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            {/* Dashboard (All 4 roles) */}
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />

            {/* Products (All 4 roles) */}
            <Route
              path="/products"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <ProductsPage />
                </ProtectedRoute>
              }
            />

            {/* Suppliers (Admin, Inventory Manager, Purchase Manager) */}
            <Route
              path="/suppliers"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER']}>
                  <SuppliersPage />
                </ProtectedRoute>
              }
            />

            {/* Users & Roles (Admin Only) */}
            <Route
              path="/users"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <UsersPage />
                </ProtectedRoute>
              }
            />

            {/* Inventory (All 4 roles) */}
            <Route
              path="/inventory"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <InventoryPage />
                </ProtectedRoute>
              }
            />

            {/* Purchases (Admin, Purchase Manager, Inventory Manager) */}
            <Route
              path="/purchases"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'PURCHASE_MANAGER', 'INVENTORY_MANAGER']}>
                  <PurchasesPage />
                </ProtectedRoute>
              }
            />

            {/* Sales (All 4 roles) */}
            <Route
              path="/sales"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <SalesPage />
                </ProtectedRoute>
              }
            />

            {/* Smart Insights (All 4 roles) */}
            <Route
              path="/insights"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <SmartInsightsPage />
                </ProtectedRoute>
              }
            />

            {/* Alerts (All 4 roles) */}
            <Route
              path="/alerts"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <AlertsPage />
                </ProtectedRoute>
              }
            />

            {/* Reports (All 4 roles) */}
            <Route
              path="/reports"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']}>
                  <ReportsPage />
                </ProtectedRoute>
              }
            />

            {/* System Settings (Admin Only) */}
            <Route
              path="/settings"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <SettingsPage />
                </ProtectedRoute>
              }
            />

            {/* Fallback Catch-All */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
