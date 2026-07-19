import React, { useState, useEffect, useRef } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, ScrollView, StyleSheet,
  Dimensions, RefreshControl, Linking,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Search, Play } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { GENRES } from '../data/mock-data';
import { MovieCard } from '../components/MovieCard';
import { RootStackParamList, TabParamList } from '../navigation/types';
import { Movie } from '../types';
import { movieService } from '../services/movieService';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { ImageFallback } from '../components/ImageFallback';

const { width } = Dimensions.get('window');

import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';

type Props = CompositeScreenProps<
  BottomTabScreenProps<TabParamList, 'Discover'>,
  NativeStackScreenProps<RootStackParamList>
>;

export function DiscoverScreen({ navigation }: Props) {
  const [selectedGenre, setSelectedGenre] = useState('Todos');
  const [searchQuery, setSearchQuery] = useState('');
  
  const [movies, setMovies] = useState<Movie[]>([]);
  const [featured, setFeatured] = useState<Movie | null>(null);
  
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [isSearching, setIsSearching] = useState(false);

  const searchTimeout = useRef<NodeJS.Timeout | null>(null);

  const loadData = async (genre?: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const moviesRes = await movieService.getMovies(genre);
      setMovies(moviesRes);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Erro ao carregar dados');
    } finally {
      setIsLoading(false);
    }
  };

  // Load featured movie only once on mount
  useEffect(() => {
    const loadFeatured = async () => {
      try {
        const featuredRes = await movieService.getFeaturedMovie();
        setFeatured(featuredRes);
      } catch {
        // Featured is optional, no need to block UI
      }
    };
    loadFeatured();
  }, []);

  useEffect(() => {
    loadData(selectedGenre === 'Todos' ? undefined : selectedGenre);
  }, [selectedGenre]);

  const onRefresh = async () => {
    setRefreshing(true);
    await loadData(selectedGenre === 'Todos' ? undefined : selectedGenre);
    setRefreshing(false);
  };

  const handleSearch = (text: string) => {
    setSearchQuery(text);
    if (searchTimeout.current) clearTimeout(searchTimeout.current);

    if (text.trim() === '') {
      setIsSearching(false);
      loadData(selectedGenre === 'Todos' ? undefined : selectedGenre);
      return;
    }

    setIsSearching(true);
    searchTimeout.current = setTimeout(async () => {
      setIsLoading(true);
      try {
        const results = await movieService.searchMovies(text);
        setMovies(results);
      } catch (err: any) {
        setError(err.message || 'Erro na busca');
      } finally {
        setIsLoading(false);
      }
    }, 300);
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
      >
        <View style={styles.header}>
          <Text style={styles.title}>Descobrir</Text>
          <View style={styles.searchBar}>
            <Search size={20} color={colors.mutedForeground} />
            <TextInput
              style={styles.searchInput}
              placeholder="Buscar filmes, séries..."
              placeholderTextColor={colors.mutedForeground}
              value={searchQuery}
              onChangeText={handleSearch}
            />
          </View>
        </View>

        {!isSearching && featured && (
          <View style={styles.featuredSection}>
            <TouchableOpacity 
              activeOpacity={0.9} 
              onPress={() => navigation.navigate('MovieDetails', { id: featured.id })}
            >
              <View style={styles.featuredImageContainer}>
                <ImageFallback source={{ uri: featured.poster }} style={styles.featuredImage} />
                <View style={styles.featuredOverlay} />
                <View style={styles.featuredContent}>
                  <Text style={styles.featuredTag}>Destaque da Semana</Text>
                  <Text style={styles.featuredTitle}>{featured.title}</Text>
                  <View style={styles.featuredActions}>
                    <TouchableOpacity 
                      style={styles.playButton}
                      onPress={() => {
                        const query = encodeURIComponent(featured.title + ' trailer');
                        Linking.openURL(`https://www.youtube.com/results?search_query=${query}`);
                      }}
                    >
                      <Play size={20} color={colors.primaryForeground} fill={colors.primaryForeground} />
                      <Text style={styles.playText}>Assistir Trailer</Text>
                    </TouchableOpacity>
                  </View>
                </View>
              </View>
            </TouchableOpacity>
          </View>
        )}

        {!isSearching && (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.genresContainer}
          >
            {['Todos', ...GENRES].map((genre) => (
              <TouchableOpacity
                key={genre}
                style={[styles.genreChip, selectedGenre === genre && styles.genreChipActive]}
                onPress={() => setSelectedGenre(genre)}
              >
                <Text style={[styles.genreText, selectedGenre === genre && styles.genreTextActive]}>
                  {genre}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        )}

        <View style={styles.moviesSection}>
          <Text style={styles.sectionTitle}>
            {isSearching ? 'Resultados da Busca' : selectedGenre === 'Todos' ? 'Em Alta' : selectedGenre}
          </Text>
          
          {isLoading ? (
            <LoadingSpinner />
          ) : error ? (
            <ErrorMessage message={error} onRetry={() => loadData(selectedGenre === 'Todos' ? undefined : selectedGenre)} />
          ) : movies.length === 0 ? (
            <Text style={{ color: colors.mutedForeground, textAlign: 'center', marginTop: 20 }}>
              Nenhum filme encontrado.
            </Text>
          ) : (
            <View style={styles.grid}>
              {movies.map((movie) => (
                <MovieCard
                  key={movie.id}
                  movie={movie}
                  onPress={(id) => navigation.navigate('MovieDetails', { id })}
                />
              ))}
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  scrollContent: { paddingBottom: 24 },
  header: { paddingHorizontal: 24, paddingTop: 16, marginBottom: 24 },
  title: { color: colors.white, fontSize: 32, fontWeight: '700', marginBottom: 16 },
  searchBar: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.inputBackground, borderRadius: 14, paddingHorizontal: 16, height: 50, gap: 12 },
  searchInput: { flex: 1, color: colors.white, fontSize: 16 },
  featuredSection: { paddingHorizontal: 24, marginBottom: 24 },
  featuredImageContainer: { width: '100%', height: width * 1.1, borderRadius: 24, overflow: 'hidden' },
  featuredImage: { width: '100%', height: '100%' },
  featuredOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.4)' },
  featuredContent: { position: 'absolute', bottom: 0, left: 0, right: 0, padding: 24 },
  featuredTag: { color: colors.primary, fontSize: 12, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8 },
  featuredTitle: { color: colors.white, fontSize: 32, fontWeight: '700', marginBottom: 16 },
  featuredActions: { flexDirection: 'row', gap: 12 },
  playButton: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.primary, paddingHorizontal: 20, paddingVertical: 12, borderRadius: 12, gap: 8 },
  playText: { color: colors.primaryForeground, fontSize: 16, fontWeight: '600' },
  genresContainer: { paddingHorizontal: 24, marginBottom: 24, gap: 8 },
  genreChip: { paddingHorizontal: 20, paddingVertical: 10, borderRadius: 20, backgroundColor: colors.secondary },
  genreChipActive: { backgroundColor: colors.primary },
  genreText: { color: colors.mutedForeground, fontSize: 14, fontWeight: '500' },
  genreTextActive: { color: colors.primaryForeground, fontWeight: '600' },
  moviesSection: { paddingHorizontal: 24 },
  sectionTitle: { color: colors.white, fontSize: 20, fontWeight: '600', marginBottom: 16 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' },
});
