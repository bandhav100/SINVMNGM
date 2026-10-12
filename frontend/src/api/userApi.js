import apiClient from './client';

export const userApi = {
  getUsers: async (params = {}) => {
    const res = await apiClient.get('/users', { params });
    return res.data;
  },

  getUserById: async (id) => {
    const res = await apiClient.get(`/users/${id}`);
    return res.data;
  },

  createUser: async (data) => {
    const res = await apiClient.post('/users', data);
    return res.data;
  },

  updateUser: async (id, data) => {
    const res = await apiClient.put(`/users/${id}`, data);
    return res.data;
  },

  toggleUserStatus: async (id, active) => {
    const res = await apiClient.patch(`/users/${id}/status`, { active });
    return res.data;
  }
};
