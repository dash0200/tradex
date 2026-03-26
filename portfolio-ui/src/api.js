import axios from 'axios';

// The base portfolio API 
export const portfolioApi = axios.create({
  baseURL: import.meta.env.VITE_PORTFOLIO_API_URL,
});

// The base trade upload API
export const tradeUploadApi = axios.create({
  baseURL: import.meta.env.VITE_TRADE_UPLOAD_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Configure auth header injection for both instances
const authInterceptor = (config) => {
  // Do not send authorization header for login or register endpoints
  if (config.url && config.url.includes('/auth/')) {
    return config;
  }
  
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`; // Adjust based on your auth type
  }
  return config;
};

portfolioApi.interceptors.request.use(authInterceptor);
tradeUploadApi.interceptors.request.use(authInterceptor);
