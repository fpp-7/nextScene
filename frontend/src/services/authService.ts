import { apiClient } from './api';
import { AuthResponse, LoginRequest, RegisterRequest } from '../types';

const USE_MOCK = true;

const mockUser = {
  id: 1,
  name: 'Usuario NextScene',
  email: 'usuario@email.com',
  genresPreference: [],
  interactionCount: 0,
};

export const authService = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 800));
      return { token: 'mock-jwt-token-123', user: { ...mockUser, email: data.email } };
    }
    const res = await apiClient.post<AuthResponse>('/auth/login', data);
    return res.data;
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 800));
      return { token: 'mock-jwt-token-456', user: { ...mockUser, name: data.name, email: data.email } };
    }
    const res = await apiClient.post<AuthResponse>('/auth/register', data);
    return res.data;
  },

  async logout(): Promise<void> {
    if (USE_MOCK) return;
    await apiClient.post('/auth/logout');
  },
};
