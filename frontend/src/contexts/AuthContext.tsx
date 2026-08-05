import React, { createContext, useContext, useReducer, useEffect, useCallback, ReactNode } from 'react';
import { storage } from '../services/storage';
import { User } from '../types';
import { authService } from '../services/authService';
import {
  setAuthToken, setRefreshToken, setUnauthorizedHandler, setTokensRenewedHandler,
} from '../services/api';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  hasCompletedOnboarding: boolean;
}

type AuthAction =
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'LOGIN_SUCCESS'; payload: { user: User; token: string } }
  | { type: 'LOGOUT' }
  | { type: 'UPDATE_USER'; payload: User }
  | { type: 'COMPLETE_ONBOARDING' }
  | { type: 'RESTORE_TOKEN'; payload: { user: User; token: string; hasCompletedOnboarding: boolean } };

interface AuthContextType extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (user: User) => void;
  completeOnboarding: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const TOKEN_KEY = 'nextscene_auth_token';
const USER_KEY = 'nextscene_user_data';
const REFRESH_KEY = 'nextscene_refresh_token';
const ONBOARDING_KEY = 'nextscene_onboarding_complete';

const initialState: AuthState = {
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: true,
  hasCompletedOnboarding: false,
};

function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, isLoading: action.payload };
    case 'LOGIN_SUCCESS':
      return {
        ...state,
        user: action.payload.user,
        token: action.payload.token,
        isAuthenticated: true,
        isLoading: false,
      };
    case 'LOGOUT':
      return { ...initialState, isLoading: false };
    case 'UPDATE_USER':
      return { ...state, user: action.payload };
    case 'COMPLETE_ONBOARDING':
      return { ...state, hasCompletedOnboarding: true };
    case 'RESTORE_TOKEN':
      return {
        ...state,
        user: action.payload.user,
        token: action.payload.token,
        isAuthenticated: true,
        isLoading: false,
        hasCompletedOnboarding: action.payload.hasCompletedOnboarding,
      };
    default:
      return state;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialState);

  // Restore token on app start
  useEffect(() => {
    const restoreToken = async () => {
      try {
        const token = await storage.getItem(TOKEN_KEY);
        const refresh = await storage.getItem(REFRESH_KEY);
        const userData = await storage.getItem(USER_KEY);
        const onboardingDone = await storage.getItem(ONBOARDING_KEY);

        if (token && userData && !token.startsWith('mock-')) {
          const user = JSON.parse(userData);
          setAuthToken(token);
          // Mesmo com o access token vencido, o refresh permite retomar a
          // sessão de forma transparente na primeira requisição.
          setRefreshToken(refresh);
          dispatch({
            type: 'RESTORE_TOKEN',
            payload: { user, token, hasCompletedOnboarding: onboardingDone === 'true' },
          });
        } else {
          await storage.removeItem(TOKEN_KEY);
          await storage.removeItem(REFRESH_KEY);
          await storage.removeItem(USER_KEY);
          await storage.removeItem(ONBOARDING_KEY);
          dispatch({ type: 'SET_LOADING', payload: false });
        }
      } catch {
        dispatch({ type: 'SET_LOADING', payload: false });
      }
    };
    restoreToken();
  }, []);

  /**
   * Encerra a sessão localmente, sem chamar o servidor. Usado tanto pelo logout
   * explícito quanto pela expiração do token (401).
   *
   * `keepOnboarding` preserva a flag de onboarding concluído: quando a sessão
   * apenas expira, o usuário não deve refazer o onboarding ao entrar de novo.
   */
  const clearSession = useCallback(async (keepOnboarding: boolean) => {
    setAuthToken(null);
    setRefreshToken(null);
    await storage.removeItem(TOKEN_KEY);
    await storage.removeItem(REFRESH_KEY);
    await storage.removeItem(USER_KEY);
    if (!keepOnboarding) {
      await storage.removeItem(ONBOARDING_KEY);
    }
    dispatch({ type: 'LOGOUT' });
  }, []);

  // Sessão irrecuperável → volta para a tela de login em vez de deixar o app
  // "logado" mostrando erro em todas as telas.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearSession(true);
    });
    return () => setUnauthorizedHandler(null);
  }, [clearSession]);

  // O interceptor renova os tokens sozinho; aqui eles são persistidos para que
  // a sessão sobreviva ao fechamento do app.
  useEffect(() => {
    setTokensRenewedHandler((token, refresh) => {
      storage.setItem(TOKEN_KEY, token);
      storage.setItem(REFRESH_KEY, refresh);
    });
    return () => setTokensRenewedHandler(null);
  }, []);

  const startSession = async (user: User, token: string, refresh: string) => {
    setAuthToken(token);
    setRefreshToken(refresh);
    await storage.setItem(TOKEN_KEY, token);
    await storage.setItem(REFRESH_KEY, refresh);
    await storage.setItem(USER_KEY, JSON.stringify(user));
    dispatch({ type: 'LOGIN_SUCCESS', payload: { user, token } });

    // Restaura a flag de onboarding: quem já concluiu não deve refazê-lo ao
    // relogar depois de uma expiração de sessão.
    const onboardingDone = await storage.getItem(ONBOARDING_KEY);
    if (onboardingDone === 'true') {
      dispatch({ type: 'COMPLETE_ONBOARDING' });
    }
  };

  const login = async (email: string, password: string) => {
    const response = await authService.login({ email, password });
    await startSession(response.user, response.token, response.refreshToken);
  };

  const register = async (name: string, email: string, password: string) => {
    const response = await authService.register({ name, email, password });
    await startSession(response.user, response.token, response.refreshToken);
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch {}
    await clearSession(false);
  };

  const updateUser = (user: User) => {
    dispatch({ type: 'UPDATE_USER', payload: user });
    storage.setItem(USER_KEY, JSON.stringify(user));
  };

  const completeOnboarding = async () => {
    await storage.setItem(ONBOARDING_KEY, 'true');
    dispatch({ type: 'COMPLETE_ONBOARDING' });
  };

  return (
    <AuthContext.Provider value={{ ...state, login, register, logout, updateUser, completeOnboarding }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
