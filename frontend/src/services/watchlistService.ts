import { apiClient } from './api';
import { Movie, WatchlistItem } from '../types';
import { MOVIES } from '../data/mock-data';

const USE_MOCK = false;

let mockWatchlist: number[] = [1, 3, 5, 8]; // IDs of movies in watchlist

export const watchlistService = {
  async getWatchlist(): Promise<Movie[]> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 400));
      return MOVIES.filter((m) => mockWatchlist.includes(m.id));
    }
    const res = await apiClient.get<WatchlistItem[]>('/watchlist');
    return res.data.map((item) => item.movie);
  },

  async addToWatchlist(movieId: number): Promise<void> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 300));
      if (!mockWatchlist.includes(movieId)) mockWatchlist.push(movieId);
      return;
    }
    await apiClient.post(`/watchlist/${movieId}`);
  },

  async removeFromWatchlist(movieId: number): Promise<void> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 300));
      mockWatchlist = mockWatchlist.filter((id) => id !== movieId);
      return;
    }
    await apiClient.delete(`/watchlist/${movieId}`);
  },

};
