import { apiClient } from './api';
import { Movie, WatchlistItem } from '../types';

export const watchlistService = {
  async getWatchlist(): Promise<Movie[]> {
    const res = await apiClient.get<WatchlistItem[]>('/watchlist');
    return res.data.map((item) => item.movie);
  },

  async addToWatchlist(movieId: number): Promise<void> {
    await apiClient.post(`/watchlist/${movieId}`);
  },

  async removeFromWatchlist(movieId: number): Promise<void> {
    await apiClient.delete(`/watchlist/${movieId}`);
  },
};
