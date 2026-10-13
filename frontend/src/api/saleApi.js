import client from './client';

export const saleApi = {
  getSales: async (params = {}) => {
    const response = await client.get('/sales', { params });
    return response.data;
  },

  getSaleById: async (id) => {
    const response = await client.get(`/sales/${id}`);
    return response.data;
  },

  createSale: async (data) => {
    const response = await client.post('/sales', data);
    return response.data;
  },

  updateSale: async (id, data) => {
    const response = await client.put(`/sales/${id}`, data);
    return response.data;
  },

  deleteSale: async (id) => {
    const response = await client.delete(`/sales/${id}`);
    return response.data;
  },

  completeSale: async (id) => {
    const response = await client.post(`/sales/${id}/complete`);
    return response.data;
  },

  cancelSale: async (id) => {
    const response = await client.post(`/sales/${id}/cancel`);
    return response.data;
  }
};
