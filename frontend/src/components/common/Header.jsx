import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { settingsApi } from '../../api/settingsApi';
import { Database, LogOut, User as UserIcon, Menu } from 'lucide-react';
import Badge from './Badge';

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

export default function Header({ onToggleSidebar }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [companyName, setCompanyName] = useState('Smart Inventory Management System');

  useEffect(() => {
    let isMounted = true;
    if (user?.role === 'ADMIN') {
      settingsApi.getSettings()
        .then((data) => {
          if (isMounted && data?.companyName) {
            setCompanyName(data.companyName);
          }
        })
        .catch(() => {
          // Gracefully fallback to default title
        });
    }
    return () => {
      isMounted = false;
    };
  }, [user]);

  const handleLogout = async () => {
    await logout();
    navigate('/', { replace: true });
  };

  return (
    <header className="header">
      <div className="header-left">
        <button
          className="sidebar-toggle-btn"
          onClick={onToggleSidebar}
          aria-label="Toggle navigation menu"
          title="Toggle Navigation"
        >
          <Menu size={20} />
        </button>

        <div className="header-title-container">
          <h2 className="page-title">{companyName}</h2>
          <span className="page-subtitle">Real-Time Enterprise Operations</span>
        </div>
      </div>

      <div className="header-actions">
        <div className="db-status-pill" title="Connected to PostgreSQL Database">
          <span className="db-pulse-dot" />
          <Database size={13} className="db-icon" />
          <span className="db-status-text">PostgreSQL Active</span>
        </div>

        {user && (
          <div className="user-profile-section">
            <div className="user-info-pill">
              <div className="user-avatar">
                <UserIcon size={15} />
              </div>
              <div className="user-meta">
                <span className="user-name">
                  {user.fullName || user.username}
                </span>
                <span className="user-role-badge">
                  <Badge variant={roleBadgeVariant[user.role] || 'info'}>
                    {roleDisplayName[user.role] || user.role}
                  </Badge>
                </span>
              </div>
            </div>

            <button
              onClick={handleLogout}
              className="sign-out-btn"
              title="Sign Out"
              aria-label="Sign Out"
            >
              <LogOut size={14} />
              <span className="sign-out-text">Sign Out</span>
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
