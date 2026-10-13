import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const getUploadUrl = (path) => {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  const backendUrl = import.meta.env.VITE_BACKEND_URL || '';
  return `${backendUrl}${path.startsWith('/') ? '' : '/'}${path}`;
};

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('ngoconnect_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      // Optional: Handle token expiration
    }
    return Promise.reject(error);
  }
);

export default api;
