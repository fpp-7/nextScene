import axios from 'axios';

/**
 * URL da API. Configure via `EXPO_PUBLIC_API_URL` no arquivo `.env` do frontend
 * (veja `.env.example`). Em dev com dispositivo físico, use o IP da sua máquina
 * na LAN — `localhost` aponta para o próprio aparelho.
 */
const BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api';

if (__DEV__) {
  console.log(`[api] baseURL = ${BASE_URL}`);
}

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Bypass-Tunnel-Reminder': 'true',
  },
});

let authToken: string | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
}

export function getAuthToken(): string | null {
  return authToken;
}

/**
 * Handler chamado quando a API responde 401 (token expirado ou inválido).
 * O AuthContext registra o logout aqui — evita import circular entre os módulos.
 */
let onUnauthorized: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
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
    // Só desloga se havia uma sessão ativa — um 401 no próprio login é erro de
    // credencial e deve seguir para a tela tratar.
    if (error.response?.status === 401 && authToken) {
      authToken = null;
      onUnauthorized?.();
    }
    return Promise.reject(error);
  }
);
