import client from './client';

export const settingsApi = {
  getSettings: async () => {
    const response = await client.get('/settings');
    return response.data;
  },

  updateSettings: async (settingsData) => {
    const response = await client.put('/settings', settingsData);
    return response.data;
  }
};
