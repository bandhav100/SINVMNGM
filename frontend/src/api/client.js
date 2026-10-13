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
    'Accept': 'application/json'
  }
});

// Response interceptor for consistent error parsing and 401 handling
apiClient.interceptors.response.use(
  (response) => response,
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
      // Notify authentication listeners of session expiration
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    }

    return Promise.reject(enhancedError);
  }
);

export default apiClient;
