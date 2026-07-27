import React, { createContext, useContext, useReducer, useEffect, useCallback, ReactNode } from 'react';
import * as SecureStore from 'expo-secure-store';
import { User } from '../types';
import { authService } from '../services/authService';
import { setAuthToken, setUnauthorizedHandler } from '../services/api';

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
        const token = await SecureStore.getItemAsync(TOKEN_KEY);
        const userData = await SecureStore.getItemAsync(USER_KEY);
        const onboardingDone = await SecureStore.getItemAsync(ONBOARDING_KEY);

        if (token && userData && !token.startsWith('mock-')) {
          const user = JSON.parse(userData);
          setAuthToken(token);
          dispatch({
            type: 'RESTORE_TOKEN',
            payload: { user, token, hasCompletedOnboarding: onboardingDone === 'true' },
          });
        } else {
          await SecureStore.deleteItemAsync(TOKEN_KEY);
          await SecureStore.deleteItemAsync(USER_KEY);
          await SecureStore.deleteItemAsync(ONBOARDING_KEY);
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
    await SecureStore.deleteItemAsync(TOKEN_KEY);
    await SecureStore.deleteItemAsync(USER_KEY);
    if (!keepOnboarding) {
      await SecureStore.deleteItemAsync(ONBOARDING_KEY);
    }
    dispatch({ type: 'LOGOUT' });
  }, []);

  // Token expirado/inválido → volta para a tela de login em vez de deixar o app
  // "logado" mostrando erro em todas as telas.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearSession(true);
    });
    return () => setUnauthorizedHandler(null);
  }, [clearSession]);

  const startSession = async (user: User, token: string) => {
    setAuthToken(token);
    await SecureStore.setItemAsync(TOKEN_KEY, token);
    await SecureStore.setItemAsync(USER_KEY, JSON.stringify(user));
    dispatch({ type: 'LOGIN_SUCCESS', payload: { user, token } });

    // Restaura a flag de onboarding: quem já concluiu não deve refazê-lo ao
    // relogar depois de uma expiração de sessão.
    const onboardingDone = await SecureStore.getItemAsync(ONBOARDING_KEY);
    if (onboardingDone === 'true') {
      dispatch({ type: 'COMPLETE_ONBOARDING' });
    }
  };

  const login = async (email: string, password: string) => {
    const response = await authService.login({ email, password });
    await startSession(response.user, response.token);
  };

  const register = async (name: string, email: string, password: string) => {
    const response = await authService.register({ name, email, password });
    await startSession(response.user, response.token);
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch {}
    await clearSession(false);
  };

  const updateUser = (user: User) => {
    dispatch({ type: 'UPDATE_USER', payload: user });
    SecureStore.setItemAsync(USER_KEY, JSON.stringify(user));
  };

  const completeOnboarding = async () => {
    await SecureStore.setItemAsync(ONBOARDING_KEY, 'true');
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
