import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  Boxes,
  LayoutDashboard,
  Package,
  Layers,
  Truck,
  ShoppingCart,
  TrendingUp,
  Brain,
  Bell,
  FileBarChart,
  Users,
  Settings,
  X,
  Server
} from 'lucide-react';

const allNavItems = [
  {
    path: '/products',
    label: 'Products',
    icon: Package,
    phase: 'Live',
    roles: ['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']
  },
  {
    path: '/suppliers',
    label: 'Suppliers',
    icon: Truck,
    phase: 'Live',
    roles: ['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER']
  },
  {
    path: '/dashboard',
    label: 'Dashboard',
    icon: LayoutDashboard,
    phase: 'Live',
    roles: ['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']
  },
  {
    path: '/inventory',
    label: 'Inventory',
    icon: Layers,
    phase: 'Live',
    roles: ['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']
  },
  {
    path: '/purchases',
    label: 'Purchases',
    icon: ShoppingCart,
    phase: 'Live',
    roles: ['ADMIN', 'PURCHASE_MANAGER', 'INVENTORY_MANAGER']
  },
  {
    path: '/sales',
    label: 'Sales',
    icon: TrendingUp,
    phase: 'Live',
    roles: ['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']
  },
  {
    path: '/insights',
    label: 'Smart Insights',
    icon: Brain,
    phase: 'Live',
    roles: ['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']
  },
  {
    path: '/alerts',
    label: 'Alerts',
    icon: Bell,
    phase: 'Live',
    roles: ['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']
  },
  {
    path: '/reports',
    label: 'Reports',
    icon: FileBarChart,
    phase: 'Live',
    roles: ['ADMIN', 'INVENTORY_MANAGER', 'PURCHASE_MANAGER', 'SALES_STAFF']
  },
  {
    path: '/users',
    label: 'Users & Roles',
    icon: Users,
    phase: 'Live',
    roles: ['ADMIN']
  },
  {
    path: '/settings',
    label: 'Settings',
    icon: Settings,
    phase: 'Live',
    roles: ['ADMIN']
  }
];

export default function Sidebar({ isOpen, onClose }) {
  const { user } = useAuth();
  const userRole = user?.role;

  // Filter items based on user's authorized role
  const isItemVisible = (item) => {
    if (!userRole) return false;
    if (userRole === 'ADMIN') return true;
    return item.roles.includes(userRole);
  };

  const coreOperations = allNavItems.slice(0, 6).filter(isItemVisible);
  const adminAndInsights = allNavItems.slice(6).filter(isItemVisible);

  return (
    <aside className={`sidebar ${isOpen ? 'open' : ''}`}>
      <div className="sidebar-header">
        <div className="brand-wrapper">
          <div className="brand-logo">
            <Boxes size={20} />
          </div>
          <div className="brand-meta">
            <h1 className="brand-text">SIMS</h1>
            <span className="brand-badge-pill">Enterprise</span>
          </div>
        </div>

        <button
          className="sidebar-close-btn"
          onClick={onClose}
          aria-label="Close navigation sidebar"
        >
          <X size={18} />
        </button>
      </div>

      <nav className="sidebar-nav">
        {coreOperations.length > 0 && (
          <div className="nav-group">
            <span className="nav-section-title">Core Operations</span>
            {coreOperations.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                >
                  <Icon size={18} className="nav-icon" />
                  <span className="nav-label">{item.label}</span>
                  {item.phase !== 'Live' && (
                    <span className="nav-badge">{item.phase}</span>
                  )}
                </NavLink>
              );
            })}
          </div>
        )}

        {adminAndInsights.length > 0 && (
          <div className="nav-group" style={{ marginTop: '0.75rem' }}>
            <span className="nav-section-title">Intelligence & Admin</span>
            {adminAndInsights.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                >
                  <Icon size={18} className="nav-icon" />
                  <span className="nav-label">{item.label}</span>
                  {item.phase !== 'Live' && (
                    <span className="nav-badge">{item.phase}</span>
                  )}
                </NavLink>
              );
            })}
          </div>
        )}
      </nav>

      <div className="sidebar-footer">
        <div className="system-status-indicator">
          <Server size={14} className="status-icon" />
          <span className="status-label">Spring Boot 4.1.1</span>
        </div>
        <span className="version-tag">v1.0.0</span>
      </div>
    </aside>
  );
}
