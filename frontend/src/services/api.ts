import axios from 'axios';

/**
 * URL da API. Configure via `EXPO_PUBLIC_API_URL` no arquivo `.env` do frontend
 * (veja `.env.example`). Em dev com dispositivo físico, use o IP da sua máquina
 * na LAN — `localhost` aponta para o próprio aparelho.
 */
const BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1';

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
let refreshToken: string | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
}

export function getAuthToken(): string | null {
  return authToken;
}

export function setRefreshToken(token: string | null) {
  refreshToken = token;
}

export function getRefreshToken(): string | null {
  return refreshToken;
}

/**
 * Handler chamado quando não há mais como renovar a sessão.
 * O AuthContext registra o logout aqui — evita import circular entre os módulos.
 */
let onUnauthorized: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
}

/** Chamado a cada renovação bem-sucedida, para persistir os tokens novos. */
let onTokensRenewed: ((token: string, refresh: string) => void) | null = null;

export function setTokensRenewedHandler(
  handler: ((token: string, refresh: string) => void) | null
) {
  onTokensRenewed = handler;
}

apiClient.interceptors.request.use((config) => {
  if (authToken) {
    config.headers.Authorization = `Bearer ${authToken}`;
  }
  return config;
});

/**
 * Renovação em andamento, compartilhada por todas as requisições.
 *
 * Sem isso, uma tela que dispara três chamadas em paralelo faria três
 * renovações simultâneas. Como o refresh token é rotacionado e vale uma única
 * vez, a segunda seria recusada — e o backend interpretaria a repetição como
 * roubo de token, derrubando a sessão inteira. As demais requisições esperam a
 * primeira renovação terminar.
 */
let refreshInFlight: Promise<string> | null = null;

async function renewSession(): Promise<string> {
  if (!refreshToken) {
    throw new Error('Sem refresh token');
  }

  // Instância limpa: usar o apiClient reentraria no interceptor de resposta.
  const response = await axios.post(
    `${BASE_URL}/auth/refresh`,
    { refreshToken },
    { timeout: 10000, headers: { 'Content-Type': 'application/json' } }
  );

  const { token, refreshToken: renewed } = response.data;
  authToken = token;
  refreshToken = renewed;
  onTokensRenewed?.(token, renewed);
  return token;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const isUnauthorized = error.response?.status === 401;

    // `_retried` evita laço: se a requisição já foi repetida após renovar e
    // ainda voltou 401, insistir não vai adiantar.
    const canRenew =
      isUnauthorized && refreshToken && original && !original._retried &&
      !original.url?.includes('/auth/refresh');

    if (canRenew) {
      original._retried = true;
      try {
        refreshInFlight = refreshInFlight ?? renewSession();
        const token = await refreshInFlight;
        original.headers = { ...original.headers, Authorization: `Bearer ${token}` };
        return apiClient(original);
      } catch {
        // Refresh recusado: a sessão acabou de verdade.
        authToken = null;
        refreshToken = null;
        onUnauthorized?.();
        return Promise.reject(error);
      } finally {
        refreshInFlight = null;
      }
    }

    // Só desloga se havia uma sessão ativa — um 401 no próprio login é erro de
    // credencial e deve seguir para a tela tratar.
    if (isUnauthorized && authToken) {
      authToken = null;
      refreshToken = null;
      onUnauthorized?.();
    }

    return Promise.reject(error);
  }
);
