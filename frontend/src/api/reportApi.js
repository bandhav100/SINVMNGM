import client from './client';

export const reportApi = {
  getInventoryReport: async (params = {}) => {
    const response = await client.get('/reports/inventory', { params });
    return response.data;
  },

  getSalesReport: async (params = {}) => {
    const response = await client.get('/reports/sales', { params });
    return response.data;
  },

  getPurchaseReport: async (params = {}) => {
    const response = await client.get('/reports/purchases', { params });
    return response.data;
  },

  getInventoryMovementReport: async (params = {}) => {
    const response = await client.get('/reports/inventory-movements', { params });
    return response.data;
  },

  getStockValuationReport: async (params = {}) => {
    const response = await client.get('/reports/stock-valuation', { params });
    return response.data;
  }
};
