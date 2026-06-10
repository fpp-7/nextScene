import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Compass, Sparkles, Bookmark, User } from 'lucide-react-native';
import { colors } from '../theme/colors';
import { RootStackParamList, TabParamList } from './types';
import { useAuth } from '../contexts/AuthContext';
import { LoadingScreen } from '../components/LoadingScreen';

import { LoginScreen } from '../screens/LoginScreen';
import { RegisterScreen } from '../screens/RegisterScreen';
import { OnboardingGenresScreen } from '../screens/OnboardingGenresScreen';
import { OnboardingColdStartScreen } from '../screens/OnboardingColdStartScreen';
import { DiscoverScreen } from '../screens/DiscoverScreen';
import { RecommendationsScreen } from '../screens/RecommendationsScreen';
import { WatchlistScreen } from '../screens/WatchlistScreen';
import { ProfileScreen } from '../screens/ProfileScreen';
import { MovieDetailsScreen } from '../screens/MovieDetailsScreen';
import { EditProfileScreen } from '../screens/EditProfileScreen';
import { GenrePreferencesScreen } from '../screens/GenrePreferencesScreen';
import { SettingsScreen } from '../screens/SettingsScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<TabParamList>();

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: colors.card,
          borderTopColor: colors.border,
          borderTopWidth: 1,
          paddingBottom: 8,
          paddingTop: 8,
          height: 64,
        },
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.mutedForeground,
        tabBarLabelStyle: { fontSize: 10 },
      }}
    >
      <Tab.Screen name="Discover" component={DiscoverScreen} options={{ tabBarLabel: 'Descobrir', tabBarIcon: ({ color, size }) => <Compass size={size} color={color} /> }} />
      <Tab.Screen name="Recommendations" component={RecommendationsScreen} options={{ tabBarLabel: 'Para Voce', tabBarIcon: ({ color, size }) => <Sparkles size={size} color={color} /> }} />
      <Tab.Screen name="Watchlist" component={WatchlistScreen} options={{ tabBarLabel: 'Watchlist', tabBarIcon: ({ color, size }) => <Bookmark size={size} color={color} /> }} />
      <Tab.Screen name="Profile" component={ProfileScreen} options={{ tabBarLabel: 'Perfil', tabBarIcon: ({ color, size }) => <User size={size} color={color} /> }} />
    </Tab.Navigator>
  );
}

export function AppNavigator() {
  const { isLoading, isAuthenticated, hasCompletedOnboarding } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.background }, animation: 'slide_from_right' }}>
        {!isAuthenticated ? (
          // Auth Stack
          <Stack.Group>
            <Stack.Screen name="Login" component={LoginScreen} />
            <Stack.Screen name="Register" component={RegisterScreen} />
          </Stack.Group>
        ) : !hasCompletedOnboarding ? (
          // Onboarding Stack
          <Stack.Group>
            <Stack.Screen name="OnboardingGenres" component={OnboardingGenresScreen} />
            <Stack.Screen name="OnboardingColdStart" component={OnboardingColdStartScreen} />
          </Stack.Group>
        ) : (
          // Main App Stack
          <Stack.Group>
            <Stack.Screen name="MainTabs" component={MainTabs} options={{ animation: 'fade' }} />
            <Stack.Screen name="MovieDetails" component={MovieDetailsScreen} />
            <Stack.Screen name="EditProfile" component={EditProfileScreen} />
            <Stack.Screen name="GenrePreferences" component={GenrePreferencesScreen} />
            <Stack.Screen name="Settings" component={SettingsScreen} />
          </Stack.Group>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
