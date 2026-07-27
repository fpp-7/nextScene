import { apiClient } from './api';
import { User, UserStats, GenrePreference } from '../types';

export const userService = {
  async getProfile(): Promise<User> {
    const res = await apiClient.get<User>('/users/me');
    return res.data;
  },

  async updateProfile(data: Partial<User>): Promise<User> {
    const res = await apiClient.put<User>('/users/me', data);
    return res.data;
  },

  async getStats(): Promise<UserStats> {
    const res = await apiClient.get<UserStats>('/users/me/stats');
    return res.data;
  },

  async updateGenres(prefs: GenrePreference): Promise<void> {
    await apiClient.put('/users/me/genres', prefs);
  },
};
