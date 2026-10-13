import client from './client';

export const purchaseApi = {
  getPurchases: async (params = {}) => {
    const response = await client.get('/purchases', { params });
    return response.data;
  },

  getPurchaseById: async (id) => {
    const response = await client.get(`/purchases/${id}`);
    return response.data;
  },

  createPurchase: async (data) => {
    const response = await client.post('/purchases', data);
    return response.data;
  },

  updatePurchase: async (id, data) => {
    const response = await client.put(`/purchases/${id}`, data);
    return response.data;
  },

  deletePurchase: async (id) => {
    const response = await client.delete(`/purchases/${id}`);
    return response.data;
  },

  submitPurchase: async (id) => {
    const response = await client.post(`/purchases/${id}/submit`);
    return response.data;
  },

  receivePurchase: async (id) => {
    const response = await client.post(`/purchases/${id}/receive`);
    return response.data;
  },

  cancelPurchase: async (id) => {
    const response = await client.post(`/purchases/${id}/cancel`);
    return response.data;
  }
};
