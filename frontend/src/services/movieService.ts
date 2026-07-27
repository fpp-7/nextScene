import { apiClient } from './api';
import { Movie } from '../types';

export const PAGE_SIZE = 20;

export const movieService = {
  async getMovies(genre?: string, page = 0): Promise<Movie[]> {
    const res = await apiClient.get<Movie[]>('/movies', {
      params: { ...(genre ? { genre } : {}), page, size: PAGE_SIZE },
    });
    return res.data;
  },

  async getMovieById(id: number): Promise<Movie | null> {
    const res = await apiClient.get<Movie>(`/movies/${id}`);
    return res.data;
  },

  async getFeaturedMovie(): Promise<Movie> {
    const res = await apiClient.get<Movie>('/movies/featured');
    return res.data;
  },

  async searchMovies(query: string, page = 0, signal?: AbortSignal): Promise<Movie[]> {
    const res = await apiClient.get<Movie[]>('/movies/search', {
      params: { q: query, page, size: PAGE_SIZE },
      signal,
    });
    return res.data;
  },

  async getRecommendations(): Promise<{ aiPicks: Movie[]; similarUsers: Movie[] }> {
    const res = await apiClient.get('/recommendations');
    return res.data;
  },
};
