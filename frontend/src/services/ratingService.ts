import { apiClient } from './api';
import { RatingEntry, ColdStartRating } from '../types';

const USE_MOCK = true;

export const ratingService = {
  async rateMovie(movieId: number, type: 'like' | 'dislike' | 'seen'): Promise<RatingEntry> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 300));
      return { id: Math.floor(Math.random() * 1000), movieId, type };
    }
    const res = await apiClient.post<RatingEntry>('/ratings', { movieId, type });
    return res.data;
  },

  async getMyRatings(): Promise<RatingEntry[]> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 300));
      return [];
    }
    const res = await apiClient.get<RatingEntry[]>('/ratings/me');
    return res.data;
  },

  async submitColdStart(ratings: ColdStartRating[]): Promise<void> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 800));
      return;
    }
    await apiClient.post('/recommendations/cold-start', { ratings });
  },
};
