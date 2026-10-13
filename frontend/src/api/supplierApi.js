import apiClient from './client';

export const supplierApi = {
  getSuppliers: async ({ search = '', active = null, page = 0, size = 10, sort = 'id,asc' } = {}) => {
    const params = { page, size, sort };
    if (search && search.trim()) {
      params.search = search.trim();
    }
    if (active !== null && active !== undefined && active !== '') {
      params.active = active;
    }
    const response = await apiClient.get('/suppliers', { params });
    return response.data;
  },

  getActiveSuppliers: async () => {
    const response = await apiClient.get('/suppliers/active');
    return response.data;
  },

  getSupplierById: async (id) => {
    const response = await apiClient.get(`/suppliers/${id}`);
    return response.data;
  },

  createSupplier: async (supplierData) => {
    const response = await apiClient.post('/suppliers', supplierData);
    return response.data;
  },

  updateSupplier: async (id, supplierData) => {
    const response = await apiClient.put(`/suppliers/${id}`, supplierData);
    return response.data;
  },

  toggleSupplierStatus: async (id, active) => {
    const response = await apiClient.patch(`/suppliers/${id}/status`, { active });
    return response.data;
  },

  deleteSupplier: async (id) => {
    const response = await apiClient.delete(`/suppliers/${id}`);
    return response.data;
  }
};
