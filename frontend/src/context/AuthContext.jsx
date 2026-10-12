import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [setupRequired, setSetupRequired] = useState(null);

  const checkSetupStatus = useCallback(async () => {
    try {
      const res = await authApi.getSetupStatus();
      setSetupRequired(res.setupRequired);
      return res.setupRequired;
    } catch {
      return false;
    }
  }, []);

  const refreshUser = useCallback(async () => {
    try {
      const currentUser = await authApi.getCurrentUser();
      setUser(currentUser);
      return currentUser;
    } catch {
      setUser(null);
      return null;
    }
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function initAuth() {
      try {
        await authApi.initCsrf();
        const required = await checkSetupStatus();
        if (!required) {
          await refreshUser();
        }
      } catch {
        // Initialization failure handled gracefully
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    initAuth();

    const handleUnauthorized = () => {
      setUser(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      isMounted = false;
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, [checkSetupStatus, refreshUser]);

  const login = async (credentials) => {
    const res = await authApi.login(credentials);
    const loggedInUser = res.user || (await authApi.getCurrentUser());
    setUser(loggedInUser);
    setSetupRequired(false);
    return loggedInUser;
  };

  const setupAdmin = async (adminData) => {
    const res = await authApi.setupAdmin(adminData);
    const createdAdmin = res.user || (await authApi.getCurrentUser());
    setUser(createdAdmin);
    setSetupRequired(false);
    return createdAdmin;
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      // Clear local user even if network request fails
    } finally {
      setUser(null);
    }
  };

  const hasRole = (allowedRoles) => {
    if (!user || !user.role) return false;
    if (user.role === 'ADMIN') return true;
    if (Array.isArray(allowedRoles)) {
      return allowedRoles.includes(user.role);
    }
    return user.role === allowedRoles;
  };

  const value = {
    user,
    loading,
    setupRequired,
    checkSetupStatus,
    refreshUser,
    login,
    setupAdmin,
    logout,
    hasRole,
    isAuthenticated: !!user
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
