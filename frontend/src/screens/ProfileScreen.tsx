import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { Settings, LogOut, ChevronRight, User, Star, Film, ThumbsUp } from 'lucide-react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { RootStackParamList, TabParamList } from '../navigation/types';
import { useAuth } from '../contexts/AuthContext';
import { userService } from '../services/userService';
import { UserStats } from '../types';
import { LoadingSpinner } from '../components/LoadingSpinner';

import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';

type Props = CompositeScreenProps<
  BottomTabScreenProps<TabParamList, 'Profile'>,
  NativeStackScreenProps<RootStackParamList>
>;

export function ProfileScreen({ navigation }: Props) {
  const { user, logout } = useAuth();
  const [stats, setStats] = useState<UserStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadStats = async () => {
      try {
        const data = await userService.getStats();
        setStats(data);
      } catch (err: any) {
        console.error('Erro ao carregar estatísticas do perfil:', err);
      } finally {
        setIsLoading(false);
      }
    };
    loadStats();
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Text style={styles.title}>Perfil</Text>
        </View>

        <View style={styles.profileSection}>
          <View style={styles.avatarContainer}>
            <View style={styles.avatar}>
              <User size={40} color={colors.primaryForeground} />
            </View>
          </View>
          <Text style={styles.name}>{user?.name || 'Usuário'}</Text>
          <Text style={styles.email}>{user?.email || 'email@exemplo.com'}</Text>
        </View>

        <View style={styles.statsContainer}>
          {isLoading ? (
            <LoadingSpinner size="small" />
          ) : (
            <>
              <View style={styles.statItem}>
                <View style={[styles.statIcon, { backgroundColor: 'rgba(212,160,23,0.1)' }]}>
                  <Star size={20} color={colors.primary} />
                </View>
                <Text style={styles.statValue}>{stats?.rated || 0}</Text>
                <Text style={styles.statLabel}>Avaliados</Text>
              </View>
              <View style={styles.statDivider} />
              <View style={styles.statItem}>
                <View style={[styles.statIcon, { backgroundColor: 'rgba(212,160,23,0.1)' }]}>
                  <Film size={20} color={colors.primary} />
                </View>
                <Text style={styles.statValue}>{stats?.watched || 0}</Text>
                <Text style={styles.statLabel}>Assistidos</Text>
              </View>
              <View style={styles.statDivider} />
              <View style={styles.statItem}>
                {/* "Curtidos" com o polegar, não "Favoritos" com o marcador: o
                    marcador é o ícone da watchlist em todo o resto do app, e a
                    contagem aqui é de curtidas. Quem via "15 Favoritos" e um
                    filme salvo na lista achava, com razão, que o número estava
                    errado. As três métricas são de avaliação, não de watchlist. */}
                <View style={[styles.statIcon, { backgroundColor: 'rgba(212,160,23,0.1)' }]}>
                  <ThumbsUp size={20} color={colors.primary} />
                </View>
                <Text style={styles.statValue}>{stats?.favorites || 0}</Text>
                <Text style={styles.statLabel}>Curtidos</Text>
              </View>
            </>
          )}
        </View>

        <View style={styles.menuSection}>
          <TouchableOpacity 
            style={styles.menuItem} 
            activeOpacity={0.7}
            onPress={() => navigation.navigate('EditProfile')}
          >
            <View style={styles.menuItemLeft}>
              <View style={styles.menuIconContainer}>
                <User size={20} color={colors.mutedForeground} />
              </View>
              <Text style={styles.menuItemText}>Editar Perfil</Text>
            </View>
            <ChevronRight size={20} color={colors.mutedForeground} />
          </TouchableOpacity>

          <TouchableOpacity 
            style={styles.menuItem} 
            activeOpacity={0.7}
            onPress={() => navigation.navigate('GenrePreferences')}
          >
            <View style={styles.menuItemLeft}>
              <View style={styles.menuIconContainer}>
                <Star size={20} color={colors.mutedForeground} />
              </View>
              <Text style={styles.menuItemText}>Preferências de Gênero</Text>
            </View>
            <ChevronRight size={20} color={colors.mutedForeground} />
          </TouchableOpacity>

          <TouchableOpacity 
            style={styles.menuItem} 
            activeOpacity={0.7}
            onPress={() => navigation.navigate('Settings')}
          >
            <View style={styles.menuItemLeft}>
              <View style={styles.menuIconContainer}>
                <Settings size={20} color={colors.mutedForeground} />
              </View>
              <Text style={styles.menuItemText}>Configurações</Text>
            </View>
            <ChevronRight size={20} color={colors.mutedForeground} />
          </TouchableOpacity>
        </View>

        <TouchableOpacity style={styles.logoutButton} activeOpacity={0.7} onPress={logout}>
          <LogOut size={20} color={colors.red400} />
          <Text style={styles.logoutText}>Sair da Conta</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: { paddingHorizontal: 24, paddingTop: 16 },
  title: { color: colors.white, fontSize: 32, fontWeight: '700' },
  profileSection: { alignItems: 'center', marginTop: 24, marginBottom: 32 },
  avatarContainer: { marginBottom: 16 },
  avatar: { width: 100, height: 100, borderRadius: 50, backgroundColor: colors.primary, justifyContent: 'center', alignItems: 'center' },
  name: { color: colors.white, fontSize: 24, fontWeight: '600', marginBottom: 4 },
  email: { color: colors.mutedForeground, fontSize: 14 },
  statsContainer: { flexDirection: 'row', backgroundColor: colors.secondary, marginHorizontal: 24, borderRadius: 16, padding: 20, marginBottom: 32, minHeight: 100, justifyContent: 'center', alignItems: 'center' },
  statItem: { flex: 1, alignItems: 'center', gap: 8 },
  statIcon: { width: 40, height: 40, borderRadius: 20, justifyContent: 'center', alignItems: 'center' },
  statValue: { color: colors.white, fontSize: 20, fontWeight: '700' },
  statLabel: { color: colors.mutedForeground, fontSize: 12 },
  statDivider: { width: 1, height: '100%', backgroundColor: colors.border },
  menuSection: { paddingHorizontal: 24, gap: 12, marginBottom: 32 },
  menuItem: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: colors.secondary, padding: 16, borderRadius: 14 },
  menuItemLeft: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  menuIconContainer: { width: 40, height: 40, borderRadius: 10, backgroundColor: colors.background, justifyContent: 'center', alignItems: 'center' },
  menuItemText: { color: colors.white, fontSize: 16, fontWeight: '500' },
  logoutButton: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, marginHorizontal: 24, paddingVertical: 16, borderRadius: 14, borderWidth: 1, borderColor: 'rgba(239,68,68,0.3)', marginBottom: 32 },
  logoutText: { color: colors.red400, fontSize: 16, fontWeight: '600' },
});
