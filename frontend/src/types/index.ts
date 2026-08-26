// User
export interface User {
  /** UUID em texto. Não é numérico — ver nota em UserResponse.java no backend. */
  id: string;
  name: string;
  email: string;
  genresPreference: string[];
  /** Gêneros vetados pelo usuário; o backend não recomenda nada com eles. */
  genresExcluded: string[];
}

// Auth
export interface AuthResponse {
  /** Access token JWT, curto. */
  token: string;
  /** Token opaco de longa duração, usado para renovar sem novo login. */
  refreshToken: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

// Movie (matching existing mock-data structure)
export interface Movie {
  id: number;
  title: string;
  year: number;
  genre: string;
  rating: number;
  imdb: number;
  poster: string;
  synopsis: string;
  cast: string[];
  /** Chave do vídeo no YouTube. Ausente quando o filme não tem trailer conhecido. */
  trailerKey?: string | null;
}

// Rating
export interface RatingEntry {
  /** UUID em texto. */
  id?: string;
  movieId: number;
  score?: number;
  type: 'like' | 'dislike' | 'seen';
}

// Watchlist
export interface WatchlistItem {
  /** UUID em texto. */
  id: string;
  movieId: number;
  movie: Movie;
  addedAt: string;
}

// User Stats
export interface UserStats {
  rated: number;
  watched: number;
  favorites: number;
}

// API Response wrapper
export interface ApiResponse<T> {
  data: T;
  message?: string;
}

// Genre preference for onboarding
export interface GenrePreference {
  liked: string[];
  disliked: string[];
}

// Cold start rating
export interface ColdStartRating {
  movieId: number;
  type: 'like' | 'dislike' | 'seen';
}
