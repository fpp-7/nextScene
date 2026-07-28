import { apiClient } from './api';
import { Movie } from '../types';

export const PAGE_SIZE = 20;

/**
 * Critério de ordenação do catálogo.
 *
 * `popular` usa o número de avaliações no TMDB, não a nota média: nota mede
 * qualidade percebida, e um documentário obscuro com 60 votos pode ter média
 * 9,0 sem que ninguém esteja assistindo.
 */
export type MovieSort = 'popular' | 'recent' | 'rating';

export const movieService = {
  async getMovies(genre?: string, page = 0, sort?: MovieSort): Promise<Movie[]> {
    const res = await apiClient.get<Movie[]>('/movies', {
      params: { ...(genre ? { genre } : {}), ...(sort ? { sort } : {}), page, size: PAGE_SIZE },
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
