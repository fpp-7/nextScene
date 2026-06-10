// User
export interface User {
  id: number;
  name: string;
  email: string;
  genresPreference: string[];
  interactionCount: number;
}

// Auth
export interface AuthResponse {
  token: string;
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
}

// Rating
export interface RatingEntry {
  id?: number;
  movieId: number;
  score?: number;
  type: 'like' | 'dislike' | 'seen';
}

// Watchlist
export interface WatchlistItem {
  id: number;
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
