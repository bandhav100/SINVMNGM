import apiClient from './client';

export const productApi = {
  getProducts: async ({ search = '', page = 0, size = 10, sort = 'id,asc' } = {}) => {
    const params = { page, size, sort };
    if (search && search.trim()) {
      params.search = search.trim();
    }
    const response = await apiClient.get('/products', { params });
    return response.data;
  },

  getProductById: async (id) => {
    const response = await apiClient.get(`/products/${id}`);
    return response.data;
  },

  createProduct: async (productData) => {
    const response = await apiClient.post('/products', productData);
    return response.data;
  },

  updateProduct: async (id, productData) => {
    const response = await apiClient.put(`/products/${id}`, productData);
    return response.data;
  },

  deleteProduct: async (id) => {
    const response = await apiClient.delete(`/products/${id}`);
    return response.data;
  }
};
