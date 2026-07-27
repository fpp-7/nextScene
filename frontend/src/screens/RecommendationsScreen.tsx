import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, FlatList, StyleSheet, RefreshControl, TouchableOpacity } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RefreshCw, Sparkles, Users } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { MovieCard } from '../components/MovieCard';
import { RootStackParamList, TabParamList } from '../navigation/types';
import { Movie } from '../types';
import { movieService } from '../services/movieService';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';

import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';

type Props = CompositeScreenProps<
  BottomTabScreenProps<TabParamList, 'Recommendations'>,
  NativeStackScreenProps<RootStackParamList>
>;

export function RecommendationsScreen({ navigation }: Props) {
  const [aiPicks, setAiPicks] = useState<Movie[]>([]);
  const [similarUsers, setSimilarUsers] = useState<Movie[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const loadRecommendations = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await movieService.getRecommendations();
      setAiPicks(data.aiPicks);
      setSimilarUsers(data.similarUsers);
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar recomendações');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadRecommendations();
  }, []);

  const onRefresh = async () => {
    setRefreshing(true);
    await loadRecommendations();
    setRefreshing(false);
  };

  if (isLoading) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Para Você</Text>
        </View>
        <LoadingSpinner />
      </SafeAreaView>
    );
  }

  if (error) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Para Você</Text>
        </View>
        <ErrorMessage message={error} onRetry={loadRecommendations} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView 
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
      >
        <View style={styles.header}>
          <Text style={styles.title}>Para Você</Text>
          <TouchableOpacity
            onPress={onRefresh}
            style={styles.refreshBtn}
            accessibilityRole="button"
            accessibilityLabel="Atualizar recomendações"
          >
            <RefreshCw size={20} color={colors.primary} />
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <View style={styles.iconBox}>
              <Sparkles size={20} color={colors.primary} />
            </View>
            <View>
              <Text style={styles.sectionTitle}>Escolhas da IA</Text>
              <Text style={styles.sectionSubtitle}>Baseado no seu gosto</Text>
            </View>
          </View>
          
          <FlatList
            horizontal
            data={aiPicks}
            keyExtractor={(movie) => `ai-${movie.id}`}
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.horizontalList}
            renderItem={({ item }) => (
              <View style={{ marginRight: 16 }}>
                <MovieCard movie={item} onPress={(id) => navigation.navigate('MovieDetails', { id })} />
              </View>
            )}
            ListEmptyComponent={
              <Text style={styles.emptyText}>
                Avalie alguns filmes para receber recomendações personalizadas.
              </Text>
            }
          />
        </View>

        {/* Antes esta seção se chamava "Usuários Similares" e recebia a mesma
            lista da seção acima, cortada ao meio — o rótulo afirmava algo que a
            API não entregava. Agora o backend manda de fato outro sinal: filmes
            bem avaliados nos gêneros que o usuário escolheu. */}
        {similarUsers.length > 0 && (
          <View style={styles.section}>
            <View style={styles.sectionHeader}>
              <View style={styles.iconBox}>
                <Users size={20} color={colors.primary} />
              </View>
              <View>
                <Text style={styles.sectionTitle}>Dos Seus Gêneros</Text>
                <Text style={styles.sectionSubtitle}>Destaques nos gêneros que você prefere</Text>
              </View>
            </View>

            <FlatList
              horizontal
              data={similarUsers}
              keyExtractor={(movie) => `genre-${movie.id}`}
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.horizontalList}
              renderItem={({ item }) => (
                <View style={{ marginRight: 16 }}>
                  <MovieCard movie={item} onPress={(id) => navigation.navigate('MovieDetails', { id })} />
                </View>
              )}
            />
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 16, marginBottom: 24 },
  title: { color: colors.white, fontSize: 32, fontWeight: '700' },
  refreshBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(212,160,23,0.1)', justifyContent: 'center', alignItems: 'center' },
  section: { marginBottom: 32 },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 24, marginBottom: 16, gap: 12 },
  iconBox: { width: 48, height: 48, borderRadius: 12, backgroundColor: 'rgba(212,160,23,0.1)', justifyContent: 'center', alignItems: 'center' },
  sectionTitle: { color: colors.white, fontSize: 20, fontWeight: '600' },
  sectionSubtitle: { color: colors.mutedForeground, fontSize: 14 },
  horizontalList: { paddingHorizontal: 24 },
  emptyText: { color: colors.mutedForeground, fontSize: 14, paddingHorizontal: 24 },
});
