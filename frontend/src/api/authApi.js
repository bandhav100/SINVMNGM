import apiClient from './client';

const extractAndStoreToken = (res) => {
  let token = res.data?.token;
  if (!token) {
    const authHeader = res.headers?.['authorization'] || res.headers?.['Authorization'];
    if (authHeader && authHeader.startsWith('Bearer ')) {
      token = authHeader.substring(7);
    }
  }
  if (token && token !== 'null') {
    localStorage.setItem('sims_token', token);
  }
  return token;
};

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
    const token = extractAndStoreToken(res);
    return {
      ...res.data,
      token: token || res.data?.token
    };
  },

  registerCompany: async (data) => {
    const res = await apiClient.post('/auth/register-company', data);
    const token = extractAndStoreToken(res);
    return {
      ...res.data,
      token: token || res.data?.token
    };
  },

  login: async (credentials) => {
    const res = await apiClient.post('/auth/login', credentials);
    const token = extractAndStoreToken(res);
    return {
      ...res.data,
      token: token || res.data?.token
    };
  },

  getCurrentUser: async () => {
    const res = await apiClient.get('/auth/me');
    return res.data;
  },

  logout: async () => {
    try {
      const res = await apiClient.post('/auth/logout');
      return res.data;
    } finally {
      localStorage.removeItem('sims_token');
      localStorage.removeItem('sims_user');
    }
  }
};

