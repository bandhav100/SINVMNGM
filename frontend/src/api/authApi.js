import apiClient from './client';

export const authApi = {
  getSetupStatus: async () => {
    const res = await apiClient.get('/auth/setup-status');
    return res.data;
  },

  initCsrf: async () => {
    try {
      await apiClient.get('/auth/csrf');
    } catch {
      // Best effort CSRF initialization
    }
  },

  setupAdmin: async (data) => {
    const res = await apiClient.post('/auth/setup-admin', data);
    return res.data;
  },

  login: async (credentials) => {
    const res = await apiClient.post('/auth/login', credentials);
    return res.data;
  },

  getCurrentUser: async () => {
    const res = await apiClient.get('/auth/me');
    return res.data;
  },

  logout: async () => {
    const res = await apiClient.post('/auth/logout');
    return res.data;
  }
};
