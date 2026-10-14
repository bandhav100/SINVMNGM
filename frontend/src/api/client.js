import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

const apiClient = axios.create({
  baseURL,
  timeout: 10000,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'X-Client-Type': 'api'
  }
});

// Request interceptor: attach Authorization Bearer token from localStorage and set X-Client-Type
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('sims_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    config.headers['X-Client-Type'] = 'api';
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for capturing token, consistent error parsing, and 401 handling
apiClient.interceptors.response.use(
  (response) => {
    // If response includes an Authorization header, capture token
    const authHeader = response.headers?.['authorization'] || response.headers?.['Authorization'];
    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7);
      if (token && token !== 'null') {
        localStorage.setItem('sims_token', token);
      }
    }
    // If response body has token
    if (response.data?.token) {
      localStorage.setItem('sims_token', response.data.token);
    }
    return response;
  },
  (error) => {
    let message = 'An unexpected error occurred';
    let validationErrors = null;

    if (error.response && error.response.data) {
      const data = error.response.data;
      if (data.message) {
        message = data.message;
      }
      if (data.validationErrors) {
        validationErrors = data.validationErrors;
      }
    } else if (error.message) {
      message = error.message;
    }

    const enhancedError = new Error(message);
    enhancedError.status = error.response ? error.response.status : null;
    enhancedError.validationErrors = validationErrors;
    enhancedError.originalError = error;

    if (error.response && error.response.status === 401) {
      const isLoginRequest = error.config?.url?.includes('/auth/login');
      if (!isLoginRequest) {
        localStorage.removeItem('sims_token');
        localStorage.removeItem('sims_user');
        window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      }
    }

    return Promise.reject(enhancedError);
  }
);

export default apiClient;

