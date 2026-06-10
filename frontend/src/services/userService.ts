import { apiClient } from './api';
import { User, UserStats, GenrePreference } from '../types';

const USE_MOCK = true;

export const userService = {
  async getProfile(): Promise<User> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 300));
      return { id: 1, name: 'Usuario NextScene', email: 'usuario@email.com', genresPreference: [], interactionCount: 47 };
    }
    const res = await apiClient.get<User>('/users/me');
    return res.data;
  },

  async updateProfile(data: Partial<User>): Promise<User> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 500));
      return { id: 1, name: data.name || 'Usuario NextScene', email: data.email || 'usuario@email.com', genresPreference: [], interactionCount: 47 };
    }
    const res = await apiClient.put<User>('/users/me', data);
    return res.data;
  },

  async getStats(): Promise<UserStats> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 300));
      return { rated: 47, watched: 62, favorites: 15 };
    }
    const res = await apiClient.get<UserStats>('/users/me/stats');
    return res.data;
  },

  async updateGenres(prefs: GenrePreference): Promise<void> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 500));
      return;
    }
    await apiClient.put('/users/me/genres', prefs);
  },
};
