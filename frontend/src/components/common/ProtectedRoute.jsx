import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from './LoadingSpinner';
import AlertBanner from './AlertBanner';
import { ShieldAlert } from 'lucide-react';

export default function ProtectedRoute({ children, allowedRoles }) {
  const { user, loading, setupRequired, hasRole, isAuthenticated } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', width: '100vw' }}>
        <LoadingSpinner text="Authenticating session..." />
      </div>
    );
  }

  if (setupRequired) {
    return <Navigate to="/setup-admin" replace />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !hasRole(allowedRoles)) {
    return (
      <div style={{ padding: '2rem', maxWidth: '800px', margin: '2rem auto' }}>
        <AlertBanner
          type="error"
          message={`Access Denied: Your role (${user?.role}) does not have permission to access this page.`}
        />
        <div style={{ marginTop: '1rem', textAlign: 'center' }}>
          <p style={{ color: 'var(--text-secondary)' }}>
            Please navigate to an authorized section from the sidebar.
          </p>
        </div>
      </div>
    );
  }

  return children;
}
