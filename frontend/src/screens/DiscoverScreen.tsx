import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, FlatList, StyleSheet,
  useWindowDimensions, RefreshControl, Linking, ActivityIndicator,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Search, Play } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { GENRES } from '../data/genres';
import { MovieCard } from '../components/MovieCard';
import { RootStackParamList, TabParamList } from '../navigation/types';
import { Movie } from '../types';
import { movieService, PAGE_SIZE } from '../services/movieService';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { ImageFallback } from '../components/ImageFallback';

import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';

type Props = CompositeScreenProps<
  BottomTabScreenProps<TabParamList, 'Discover'>,
  NativeStackScreenProps<RootStackParamList>
>;

export function DiscoverScreen({ navigation }: Props) {
  // Lido do hook, não do módulo: assim o layout acompanha rotação e split-screen.
  const { width } = useWindowDimensions();

  const [selectedGenre, setSelectedGenre] = useState('Todos');
  const [searchQuery, setSearchQuery] = useState('');

  const [movies, setMovies] = useState<Movie[]>([]);
  const [featured, setFeatured] = useState<Movie | null>(null);

  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [isSearching, setIsSearching] = useState(false);

  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Cancela a requisição anterior: sem isso, a resposta de "mat" podia chegar
  // depois da de "matrix" e sobrescrever o resultado correto.
  const inFlight = useRef<AbortController | null>(null);

  const genreParam = selectedGenre === 'Todos' ? undefined : selectedGenre;

  const fetchPage = useCallback(
    async (pageToLoad: number, query: string, genre?: string) => {
      inFlight.current?.abort();
      const controller = new AbortController();
      inFlight.current = controller;

      return query.trim()
        ? movieService.searchMovies(query, pageToLoad, controller.signal)
        : movieService.getMovies(genre, pageToLoad);
    },
    []
  );

  const loadFirstPage = useCallback(
    async (query: string, genre?: string) => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await fetchPage(0, query, genre);
        setMovies(data);
        setPage(0);
        setHasMore(data.length === PAGE_SIZE);
      } catch (err: any) {
        if (err.name === 'CanceledError' || err.code === 'ERR_CANCELED') return;
        setError(err.response?.data?.error || err.message || 'Erro ao carregar filmes');
      } finally {
        setIsLoading(false);
      }
    },
    [fetchPage]
  );

  const loadMore = useCallback(async () => {
    if (isLoadingMore || isLoading || !hasMore || error) return;

    setIsLoadingMore(true);
    try {
      const next = page + 1;
      const data = await fetchPage(next, searchQuery, genreParam);
      setMovies((current) => [...current, ...data]);
      setPage(next);
      setHasMore(data.length === PAGE_SIZE);
    } catch (err: any) {
      if (err.name !== 'CanceledError' && err.code !== 'ERR_CANCELED') {
        setHasMore(false);
      }
    } finally {
      setIsLoadingMore(false);
    }
  }, [isLoadingMore, isLoading, hasMore, error, page, searchQuery, genreParam, fetchPage]);

  useEffect(() => {
    movieService
      .getFeaturedMovie()
      .then(setFeatured)
      .catch(() => {
        // Destaque é opcional — sua ausência não deve bloquear a tela.
      });
  }, []);

  useEffect(() => {
    loadFirstPage(searchQuery, genreParam);
    // Recarrega ao trocar de gênero; a busca tem seu próprio fluxo com debounce.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedGenre]);

  useEffect(() => () => inFlight.current?.abort(), []);

  const onRefresh = async () => {
    setRefreshing(true);
    await loadFirstPage(searchQuery, genreParam);
    setRefreshing(false);
  };

  const handleSearch = (text: string) => {
    setSearchQuery(text);
    setError(null);
    if (searchTimeout.current) clearTimeout(searchTimeout.current);

    const trimmed = text.trim();
    setIsSearching(trimmed !== '');

    searchTimeout.current = setTimeout(() => {
      loadFirstPage(text, genreParam);
    }, 300);
  };

  const renderHeader = () => (
    <>
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
            accessibilityLabel="Buscar filmes"
            returnKeyType="search"
          />
        </View>
      </View>

      {!isSearching && featured && (
        <View style={styles.featuredSection}>
          <TouchableOpacity
            activeOpacity={0.9}
            onPress={() => navigation.navigate('MovieDetails', { id: featured.id })}
            accessibilityRole="button"
            accessibilityLabel={`Destaque da semana: ${featured.title}. Abrir detalhes`}
          >
            <View style={[styles.featuredImageContainer, { height: width * 1.1 }]}>
              <ImageFallback source={{ uri: featured.poster }} style={styles.featuredImage} />
              <View style={styles.featuredOverlay} />
              <View style={styles.featuredContent}>
                <Text style={styles.featuredTag}>Destaque da Semana</Text>
                <Text style={styles.featuredTitle}>{featured.title}</Text>
                <View style={styles.featuredActions}>
                  <TouchableOpacity
                    style={styles.playButton}
                    onPress={() => {
                      const query = encodeURIComponent(`${featured.title} trailer`);
                      Linking.openURL(`https://www.youtube.com/results?search_query=${query}`);
                    }}
                    accessibilityRole="button"
                    accessibilityLabel={`Buscar trailer de ${featured.title} no YouTube`}
                  >
                    <Play size={20} color={colors.primaryForeground} fill={colors.primaryForeground} />
                    <Text style={styles.playText}>Buscar Trailer</Text>
                  </TouchableOpacity>
                </View>
              </View>
            </View>
          </TouchableOpacity>
        </View>
      )}

      {!isSearching && (
        <FlatList
          horizontal
          data={['Todos', ...GENRES]}
          keyExtractor={(genre) => genre}
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.genresContainer}
          renderItem={({ item: genre }) => (
            <TouchableOpacity
              style={[styles.genreChip, selectedGenre === genre && styles.genreChipActive]}
              onPress={() => setSelectedGenre(genre)}
              accessibilityRole="button"
              accessibilityState={{ selected: selectedGenre === genre }}
              accessibilityLabel={`Filtrar por ${genre}`}
            >
              <Text style={[styles.genreText, selectedGenre === genre && styles.genreTextActive]}>
                {genre}
              </Text>
            </TouchableOpacity>
          )}
        />
      )}

      <Text style={styles.sectionTitle}>
        {isSearching ? 'Resultados da Busca' : selectedGenre === 'Todos' ? 'Em Alta' : selectedGenre}
      </Text>
    </>
  );

  const renderEmpty = () => {
    if (isLoading) return <LoadingSpinner />;
    if (error) {
      return <ErrorMessage message={error} onRetry={() => loadFirstPage(searchQuery, genreParam)} />;
    }
    return (
      <Text style={styles.emptyText}>
        {isSearching ? 'Nenhum filme encontrado para essa busca.' : 'Nenhum filme disponível.'}
      </Text>
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      {/* FlatList virtualiza: antes eram ScrollView + map, montando todos os
          cards de uma vez e travando a rolagem conforme a lista crescia. */}
      <FlatList
        data={movies}
        keyExtractor={(movie) => String(movie.id)}
        numColumns={2}
        columnWrapperStyle={styles.columnWrapper}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        ListHeaderComponent={renderHeader}
        ListEmptyComponent={renderEmpty}
        renderItem={({ item }) => (
          <MovieCard movie={item} onPress={(id) => navigation.navigate('MovieDetails', { id })} />
        )}
        onEndReached={loadMore}
        onEndReachedThreshold={0.5}
        ListFooterComponent={
          isLoadingMore ? <ActivityIndicator style={styles.footer} color={colors.primary} /> : null
        }
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  listContent: { paddingBottom: 24, paddingHorizontal: 24 },
  columnWrapper: { justifyContent: 'space-between' },
  header: { paddingTop: 16, marginBottom: 24 },
  title: { color: colors.white, fontSize: 32, fontWeight: '700', marginBottom: 16 },
  searchBar: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.inputBackground, borderRadius: 14, paddingHorizontal: 16, height: 50, gap: 12 },
  searchInput: { flex: 1, color: colors.white, fontSize: 16 },
  featuredSection: { marginBottom: 24 },
  featuredImageContainer: { width: '100%', borderRadius: 24, overflow: 'hidden' },
  featuredImage: { width: '100%', height: '100%' },
  featuredOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.4)' },
  featuredContent: { position: 'absolute', bottom: 0, left: 0, right: 0, padding: 24 },
  featuredTag: { color: colors.primary, fontSize: 12, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8 },
  featuredTitle: { color: colors.white, fontSize: 32, fontWeight: '700', marginBottom: 16 },
  featuredActions: { flexDirection: 'row', gap: 12 },
  playButton: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.primary, paddingHorizontal: 20, paddingVertical: 12, borderRadius: 12, gap: 8 },
  playText: { color: colors.primaryForeground, fontSize: 16, fontWeight: '600' },
  genresContainer: { marginBottom: 24, gap: 8 },
  genreChip: { paddingHorizontal: 20, paddingVertical: 10, borderRadius: 20, backgroundColor: colors.secondary },
  genreChipActive: { backgroundColor: colors.primary },
  genreText: { color: colors.mutedForeground, fontSize: 14, fontWeight: '500' },
  genreTextActive: { color: colors.primaryForeground, fontWeight: '600' },
  sectionTitle: { color: colors.white, fontSize: 20, fontWeight: '600', marginBottom: 16 },
  emptyText: { color: colors.mutedForeground, textAlign: 'center', marginTop: 20 },
  footer: { marginVertical: 16 },
});
