import { apiClient } from './api';
import { RatingEntry, ColdStartRating, Movie } from '../types';

export const ratingService = {
  async rateMovie(movieId: number, type: 'like' | 'dislike' | 'seen'): Promise<RatingEntry> {
    const res = await apiClient.post<RatingEntry>('/ratings', { movieId, type });
    return res.data;
  },

  async getMyRatings(): Promise<RatingEntry[]> {
    const res = await apiClient.get<RatingEntry[]>('/ratings/me');
    return res.data;
  },

  /**
   * Conclui o onboarding.
   *
   * As avaliações são **gravadas** antes de pedir as recomendações. Antes elas
   * eram enviadas apenas para /recommendations/cold-start, que só devolve
   * sugestões e não persiste nada — o histórico do usuário nascia vazio e todas
   * as recomendações seguintes saíam genéricas.
   */
  async submitColdStart(
    ratings: ColdStartRating[]
  ): Promise<{ aiPicks: Movie[]; similarUsers: Movie[] } | null> {
    if (ratings.length === 0) return null;

    await apiClient.post('/ratings/batch', ratings);

    const res = await apiClient.post('/recommendations/cold-start', { ratings });
    return res.data;
  },

  async deleteRating(movieId: number): Promise<void> {
    await apiClient.delete(`/ratings/${movieId}`);
  },
};
