import { apiClient } from './api';
import { Movie } from '../types';
import { MOVIES } from '../data/mock-data';

const USE_MOCK = false;

export const movieService = {
  async getMovies(genre?: string): Promise<Movie[]> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 500));
      if (genre && genre !== 'Todos') return MOVIES.filter((m) => m.genre === genre);
      return MOVIES;
    }
    const res = await apiClient.get<Movie[]>('/movies', { params: genre ? { genre } : {} });
    return res.data;
  },

  async getMovieById(id: number): Promise<Movie | null> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 300));
      return MOVIES.find((m) => m.id === id) || null;
    }
    const res = await apiClient.get<Movie>(`/movies/${id}`);
    return res.data;
  },

  async getFeaturedMovie(): Promise<Movie> {
    if (USE_MOCK) {
      return MOVIES[1]; // Interestelar
    }
    const res = await apiClient.get<Movie>('/movies/featured');
    return res.data;
  },

  async searchMovies(query: string): Promise<Movie[]> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 300));
      const q = query.toLowerCase();
      return MOVIES.filter((m) => m.title.toLowerCase().includes(q));
    }
    const res = await apiClient.get<Movie[]>('/movies/search', { params: { q: query } });
    return res.data;
  },

  async getRecommendations(): Promise<{ aiPicks: Movie[]; similarUsers: Movie[] }> {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 600));
      return {
        aiPicks: [MOVIES[0], MOVIES[3], MOVIES[4], MOVIES[2]],
        similarUsers: [MOVIES[5], MOVIES[7], MOVIES[6], MOVIES[8]],
      };
    }
    const res = await apiClient.get('/recommendations');
    return res.data;
  },
};
