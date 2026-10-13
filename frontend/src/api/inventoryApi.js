import apiClient from './client';

export const inventoryApi = {
  getStock: async (params = {}) => {
    const response = await apiClient.get('/inventory/stock', { params });
    return response.data;
  },

  getSummary: async () => {
    const response = await apiClient.get('/inventory/summary');
    return response.data;
  },

  getTransactions: async (params = {}) => {
    const response = await apiClient.get('/inventory/transactions', { params });
    return response.data;
  },

  adjustStock: async (data) => {
    const response = await apiClient.post('/inventory/adjustments', data);
    return response.data;
  },

  getAdjustments: async (params = {}) => {
    const response = await apiClient.get('/inventory/adjustments', { params });
    return response.data;
  }
};

export default inventoryApi;
