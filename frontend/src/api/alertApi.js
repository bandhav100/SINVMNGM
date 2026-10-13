import client from './client';

export const alertApi = {
  getAlerts: async (params = {}) => {
    const response = await client.get('/alerts', { params });
    return response.data;
  },

  getSummary: async () => {
    const response = await client.get('/alerts/summary');
    return response.data;
  },

  getLowStockAlerts: async () => {
    const response = await client.get('/alerts/low-stock');
    return response.data;
  }
};
