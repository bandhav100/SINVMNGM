import apiClient from './client';

export const categoryApi = {
  getCategories: async ({ search = '', page = 0, size = 10, sort = 'id,asc' } = {}) => {
    const params = { page, size, sort };
    if (search && search.trim()) {
      params.search = search.trim();
    }
    const response = await apiClient.get('/categories', { params });
    return response.data;
  },

  getAllCategories: async () => {
    const response = await apiClient.get('/categories/all');
    return response.data;
  },

  getCategoryById: async (id) => {
    const response = await apiClient.get(`/categories/${id}`);
    return response.data;
  },

  getProductCount: async (id) => {
    const response = await apiClient.get(`/categories/${id}/products/count`);
    return response.data;
  },

  createCategory: async (categoryData) => {
    const response = await apiClient.post('/categories', categoryData);
    return response.data;
  },

  updateCategory: async (id, categoryData) => {
    const response = await apiClient.put(`/categories/${id}`, categoryData);
    return response.data;
  },

  deleteCategory: async (id) => {
    const response = await apiClient.delete(`/categories/${id}`);
    return response.data;
  }
};
