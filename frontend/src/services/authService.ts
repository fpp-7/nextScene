import { apiClient } from './api';
import { AuthResponse, LoginRequest, RegisterRequest } from '../types';

export const authService = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    const res = await apiClient.post<AuthResponse>('/auth/login', data);
    return res.data;
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const res = await apiClient.post<AuthResponse>('/auth/register', data);
    return res.data;
  },

  /**
   * Troca o refresh token por um novo par.
   *
   * Na prática o interceptor em `api.ts` cuida disso sozinho; este método existe
   * para uso explícito, como revalidar a sessão ao abrir o app.
   */
  async refresh(refreshToken: string): Promise<AuthResponse> {
    const res = await apiClient.post<AuthResponse>('/auth/refresh', { refreshToken });
    return res.data;
  },

  async logout(): Promise<void> {
    await apiClient.post('/auth/logout');
  },
};
