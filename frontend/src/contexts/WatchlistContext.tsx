import React, { createContext, useContext, useReducer, useEffect, useCallback, ReactNode } from 'react';
import { Movie } from '../types';
import { watchlistService } from '../services/watchlistService';
import { useAuth } from './AuthContext';

interface WatchlistState {
  items: Movie[];
  movieIds: Set<number>;
  isLoading: boolean;
  error: string | null;
}

type WatchlistAction =
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ITEMS'; payload: Movie[] }
  | { type: 'ADD_ITEM'; payload: Movie }
  | { type: 'REMOVE_ITEM'; payload: number }
  | { type: 'SET_ERROR'; payload: string | null };

interface WatchlistContextType extends WatchlistState {
  addToWatchlist: (movie: Movie) => Promise<void>;
  removeFromWatchlist: (movieId: number) => Promise<void>;
  isInWatchlist: (movieId: number) => boolean;
  refreshWatchlist: () => Promise<void>;
}

const WatchlistContext = createContext<WatchlistContextType | undefined>(undefined);

const initialState: WatchlistState = {
  items: [],
  movieIds: new Set(),
  isLoading: true,
  error: null,
};

function watchlistReducer(state: WatchlistState, action: WatchlistAction): WatchlistState {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, isLoading: action.payload };
    case 'SET_ITEMS':
      return { ...state, items: action.payload, movieIds: new Set(action.payload.map(m => m.id)), isLoading: false, error: null };
    case 'ADD_ITEM':
      if (state.movieIds.has(action.payload.id)) return state;
      const newIds = new Set(state.movieIds);
      newIds.add(action.payload.id);
      return { ...state, items: [...state.items, action.payload], movieIds: newIds };
    case 'REMOVE_ITEM':
      const filteredIds = new Set(state.movieIds);
      filteredIds.delete(action.payload);
      return { ...state, items: state.items.filter(m => m.id !== action.payload), movieIds: filteredIds };
    case 'SET_ERROR':
      return { ...state, error: action.payload, isLoading: false };
    default:
      return state;
  }
}

export function WatchlistProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(watchlistReducer, initialState);
  const { isAuthenticated } = useAuth();

  const refreshWatchlist = useCallback(async () => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true });
      const items = await watchlistService.getWatchlist();
      dispatch({ type: 'SET_ITEMS', payload: items });
    } catch (err: any) {
      dispatch({ type: 'SET_ERROR', payload: err.message || 'Erro ao carregar watchlist' });
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      refreshWatchlist();
    } else {
      dispatch({ type: 'SET_ITEMS', payload: [] });
    }
  }, [isAuthenticated, refreshWatchlist]);

  const addToWatchlist = async (movie: Movie) => {
    dispatch({ type: 'ADD_ITEM', payload: movie });
    try {
      await watchlistService.addToWatchlist(movie.id);
    } catch (err: any) {
      dispatch({ type: 'REMOVE_ITEM', payload: movie.id }); // rollback
    }
  };

  const removeFromWatchlist = async (movieId: number) => {
    const item = state.items.find(m => m.id === movieId);
    dispatch({ type: 'REMOVE_ITEM', payload: movieId });
    try {
      await watchlistService.removeFromWatchlist(movieId);
    } catch (err: any) {
      if (item) dispatch({ type: 'ADD_ITEM', payload: item }); // rollback
    }
  };

  const isInWatchlist = (movieId: number) => state.movieIds.has(movieId);

  return (
    <WatchlistContext.Provider value={{ ...state, addToWatchlist, removeFromWatchlist, isInWatchlist, refreshWatchlist }}>
      {children}
    </WatchlistContext.Provider>
  );
}

export function useWatchlist(): WatchlistContextType {
  const context = useContext(WatchlistContext);
  if (!context) throw new Error('useWatchlist must be used within WatchlistProvider');
  return context;
}
