import apiClient from './client';

export const dashboardApi = {
  getSummary: async () => {
    const response = await apiClient.get('/dashboard/summary');
    return response.data;
  }
};

export default dashboardApi;
