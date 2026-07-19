import axios from 'axios';

const BASE_URL = 'https://slick-parrots-rule.loca.lt/api';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: { 
    'Content-Type': 'application/json',
    'Bypass-Tunnel-Reminder': 'true'
  },
});

let authToken: string | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
}

export function getAuthToken(): string | null {
  return authToken;
}

apiClient.interceptors.request.use((config) => {
  if (authToken) {
    config.headers.Authorization = `Bearer ${authToken}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid - will be handled by AuthContext
    }
    return Promise.reject(error);
  }
);
