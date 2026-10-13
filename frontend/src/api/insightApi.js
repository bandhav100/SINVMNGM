import client from './client';

export const insightApi = {
  getOverview: async () => {
    const response = await client.get('/insights/overview');
    return response.data;
  },

  getFastMovingProducts: async (params = {}) => {
    const response = await client.get('/insights/fast-moving-products', { params });
    return response.data;
  },

  getSlowMovingProducts: async (params = {}) => {
    const response = await client.get('/insights/slow-moving-products', { params });
    return response.data;
  },

  getStockValuation: async (params = {}) => {
    const response = await client.get('/insights/stock-valuation', { params });
    return response.data;
  },

  getPurchaseVsSales: async (params = {}) => {
    const response = await client.get('/insights/purchase-vs-sales', { params });
    return response.data;
  },

  getReorderRecommendations: async () => {
    const response = await client.get('/insights/reorder-recommendations');
    return response.data;
  }
};
