import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, FlatList, ScrollView, StyleSheet,
  useWindowDimensions, RefreshControl, Linking, ActivityIndicator,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Search, Play } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { messageFor } from '../services/errors';
import { colors } from '../theme/colors';
import { GENRES } from '../data/genres';
import { MovieCard } from '../components/MovieCard';
import { MovieShelf } from '../components/MovieShelf';
import { RootStackParamList, TabParamList } from '../navigation/types';
import { Movie } from '../types';
import { movieService, PAGE_SIZE } from '../services/movieService';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { ImageFallback } from '../components/ImageFallback';
import { useCardLayout } from '../utils/layout';

import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';

type Props = CompositeScreenProps<
  BottomTabScreenProps<TabParamList, 'Discover'>,
  NativeStackScreenProps<RootStackParamList>
>;

/**
 * A tela tem dois modos.
 *
 * Sem busca e sem filtro de gênero, mostra prateleiras — cada uma com critério
 * próprio. Antes era uma grade única ordenada por nota e rotulada "Em Alta",
 * que na prática entregava os melhores de todos os tempos (os cinco primeiros
 * de 1994, 1972, 1974, 1993 e 1957), sempre na mesma ordem.
 *
 * Com busca ou gênero selecionado, vira grade paginada: aí o usuário quer
 * varrer um conjunto específico, não navegar por curadoria.
 */
export function DiscoverScreen({ navigation }: Props) {
  const { width } = useWindowDimensions();
  const { columns } = useCardLayout();

  const [selectedGenre, setSelectedGenre] = useState('Todos');
  const [searchQuery, setSearchQuery] = useState('');

  const [featured, setFeatured] = useState<Movie | null>(null);
  const [popular, setPopular] = useState<Movie[]>([]);
  const [recent, setRecent] = useState<Movie[]>([]);
  const [topRated, setTopRated] = useState<Movie[]>([]);
  const [shelvesLoading, setShelvesLoading] = useState(true);

  const [movies, setMovies] = useState<Movie[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Cancela a requisição anterior: sem isso, a resposta de "mat" podia chegar
  // depois da de "matrix" e sobrescrever o resultado correto.
  const inFlight = useRef<AbortController | null>(null);

  const genreParam = selectedGenre === 'Todos' ? undefined : selectedGenre;
  const isBrowsing = searchQuery.trim() !== '' || genreParam !== undefined;

  // ─── Prateleiras ──────────────────────────────────────────────────────────

  const loadShelves = useCallback(async () => {
    setShelvesLoading(true);
    try {
      const [pop, rec, top] = await Promise.all([
        movieService.getMovies(undefined, 0, 'popular'),
        movieService.getMovies(undefined, 0, 'recent'),
        movieService.getMovies(undefined, 0, 'rating'),
      ]);
      setPopular(pop);
      setRecent(rec);
      setTopRated(top);
      setError(null);
    } catch (err: any) {
      setError(messageFor(err, 'Erro ao carregar filmes'));
    } finally {
      setShelvesLoading(false);
    }
  }, []);

  useEffect(() => {
    loadShelves();
    movieService.getFeaturedMovie().then(setFeatured).catch(() => {
      // Destaque é opcional — sua ausência não deve bloquear a tela.
    });
  }, [loadShelves]);

  // ─── Grade (busca ou gênero) ──────────────────────────────────────────────

  const fetchPage = useCallback(
    async (pageToLoad: number, query: string, genre?: string) => {
      inFlight.current?.abort();
      const controller = new AbortController();
      inFlight.current = controller;

      return query.trim()
        ? movieService.searchMovies(query, pageToLoad, controller.signal)
        : movieService.getMovies(genre, pageToLoad, 'popular');
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
        setError(messageFor(err, 'Erro ao carregar filmes'));
      } finally {
        setIsLoading(false);
      }
    },
    [fetchPage]
  );

  const loadMore = useCallback(async () => {
    if (isLoadingMore || isLoading || !hasMore || error || !isBrowsing) return;

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
  }, [isLoadingMore, isLoading, hasMore, error, page, searchQuery, genreParam, isBrowsing, fetchPage]);

  useEffect(() => {
    if (genreParam) loadFirstPage(searchQuery, genreParam);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedGenre]);

  useEffect(() => () => inFlight.current?.abort(), []);

  const onRefresh = async () => {
    setRefreshing(true);
    await (isBrowsing ? loadFirstPage(searchQuery, genreParam) : loadShelves());
    setRefreshing(false);
  };

  const handleSearch = (text: string) => {
    setSearchQuery(text);
    setError(null);
    if (searchTimeout.current) clearTimeout(searchTimeout.current);

    if (text.trim() === '' && !genreParam) {
      return; // volta para as prateleiras, que já estão carregadas
    }

    searchTimeout.current = setTimeout(() => loadFirstPage(text, genreParam), 300);
  };

  const openMovie = (id: number) => navigation.navigate('MovieDetails', { id });

  // ─── Render ───────────────────────────────────────────────────────────────

  const header = (
    <>
      <View style={styles.header}>
        <Text style={styles.title}>Descobrir</Text>
        <View style={styles.searchBar}>
          <Search size={20} color={colors.mutedForeground} />
          <TextInput
            style={styles.searchInput}
            placeholder="Buscar filmes..."
            placeholderTextColor={colors.mutedForeground}
            value={searchQuery}
            onChangeText={handleSearch}
            accessibilityLabel="Buscar filmes"
            returnKeyType="search"
          />
        </View>
      </View>

      {!isBrowsing && featured && (
        <View style={styles.featuredSection}>
          <TouchableOpacity
            activeOpacity={0.9}
            onPress={() => openMovie(featured.id)}
            accessibilityRole="button"
            accessibilityLabel={`Destaque da semana: ${featured.title}. Abrir detalhes`}
          >
            <View style={[styles.featuredImageContainer, { height: Math.min(width * 1.1, 460) }]}>
              <ImageFallback source={{ uri: featured.poster }} style={styles.featuredImage} />
              <View style={styles.featuredOverlay} />
              <View style={styles.featuredContent}>
                <Text style={styles.featuredTag}>Destaque da Semana</Text>
                <Text style={styles.featuredTitle} numberOfLines={2}>{featured.title}</Text>
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
          </TouchableOpacity>
        </View>
      )}

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
    </>
  );

  if (error && !isBrowsing) {
    return (
      <SafeAreaView style={styles.container}>
        <ErrorMessage message={error} onRetry={loadShelves} />
      </SafeAreaView>
    );
  }

  // Modo curadoria: prateleiras.
  if (!isBrowsing) {
    return (
      <SafeAreaView style={styles.container}>
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.scrollContent}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />
          }
        >
          {header}
          <MovieShelf
            title="Em Alta"
            subtitle="Os mais avaliados pelo público"
            movies={popular}
            isLoading={shelvesLoading}
            onPressMovie={openMovie}
          />
          <MovieShelf
            title="Mais Recentes"
            subtitle="Os últimos títulos do catálogo"
            movies={recent}
            isLoading={shelvesLoading}
            onPressMovie={openMovie}
          />
          <MovieShelf
            title="Bem Avaliados"
            subtitle="As melhores notas de todos os tempos"
            movies={topRated}
            isLoading={shelvesLoading}
            onPressMovie={openMovie}
          />
        </ScrollView>
      </SafeAreaView>
    );
  }

  // Modo exploração: grade paginada.
  return (
    <SafeAreaView style={styles.container}>
      <FlatList
        key={`grid-${columns}`} // numColumns não muda em tempo de execução sem remontar
        data={movies}
        keyExtractor={(movie) => String(movie.id)}
        numColumns={columns}
        columnWrapperStyle={columns > 1 ? styles.columnWrapper : undefined}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        ListHeaderComponent={
          <>
            {header}
            <Text style={styles.sectionTitle}>
              {searchQuery.trim() ? 'Resultados da Busca' : selectedGenre}
            </Text>
          </>
        }
        ListEmptyComponent={
          isLoading ? (
            <LoadingSpinner />
          ) : error ? (
            <ErrorMessage message={error} onRetry={() => loadFirstPage(searchQuery, genreParam)} />
          ) : (
            <Text style={styles.emptyText}>Nenhum filme encontrado.</Text>
          )
        }
        renderItem={({ item }) => <MovieCard movie={item} onPress={openMovie} />}
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
  scrollContent: { paddingBottom: 24, paddingLeft: 24 },
  listContent: { paddingBottom: 24, paddingHorizontal: 24 },
  columnWrapper: { justifyContent: 'space-between' },
  header: { paddingTop: 16, marginBottom: 24, paddingRight: 24 },
  title: { color: colors.white, fontSize: 32, fontWeight: '700', marginBottom: 16 },
  searchBar: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.inputBackground, borderRadius: 14, paddingHorizontal: 16, height: 50, gap: 12 },
  searchInput: { flex: 1, color: colors.white, fontSize: 16 },
  featuredSection: { marginBottom: 28, paddingRight: 24 },
  featuredImageContainer: { width: '100%', borderRadius: 24, overflow: 'hidden' },
  featuredImage: { width: '100%', height: '100%' },
  featuredOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.45)' },
  featuredContent: { position: 'absolute', bottom: 0, left: 0, right: 0, padding: 24 },
  featuredTag: { color: colors.primary, fontSize: 12, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8 },
  featuredTitle: { color: colors.white, fontSize: 30, fontWeight: '700', marginBottom: 16 },
  playButton: { flexDirection: 'row', alignSelf: 'flex-start', alignItems: 'center', backgroundColor: colors.primary, paddingHorizontal: 20, paddingVertical: 12, borderRadius: 12, gap: 8 },
  playText: { color: colors.primaryForeground, fontSize: 16, fontWeight: '600' },
  genresContainer: { marginBottom: 28, gap: 8, paddingRight: 24 },
  genreChip: { paddingHorizontal: 20, paddingVertical: 10, borderRadius: 20, backgroundColor: colors.secondary },
  genreChipActive: { backgroundColor: colors.primary },
  genreText: { color: colors.mutedForeground, fontSize: 14, fontWeight: '500' },
  genreTextActive: { color: colors.primaryForeground, fontWeight: '600' },
  sectionTitle: { color: colors.white, fontSize: 20, fontWeight: '600', marginBottom: 16 },
  emptyText: { color: colors.mutedForeground, textAlign: 'center', marginTop: 20 },
  footer: { marginVertical: 16 },
});
