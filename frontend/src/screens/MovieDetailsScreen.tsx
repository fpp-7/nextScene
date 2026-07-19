import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, Dimensions, Linking, Platform } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ArrowLeft, Star, Calendar, Bookmark, ThumbsUp, ThumbsDown, Play } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { RootStackParamList } from '../navigation/types';
import { Movie } from '../types';
import { movieService } from '../services/movieService';
import { ratingService } from '../services/ratingService';
import { useWatchlist } from '../contexts/WatchlistContext';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { ImageFallback } from '../components/ImageFallback';

const { width } = Dimensions.get('window');

type Props = NativeStackScreenProps<RootStackParamList, 'MovieDetails'>;

export function MovieDetailsScreen({ route, navigation }: Props) {
  const { id } = route.params;
  const { isInWatchlist, addToWatchlist, removeFromWatchlist } = useWatchlist();
  
  const [movie, setMovie] = useState<Movie | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [rating, setRating] = useState<'like' | 'dislike' | null>(null);
  const [isRating, setIsRating] = useState(false);

  const isSaved = isInWatchlist(id);

  const loadMovie = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const movieData = await movieService.getMovieById(id);
      if (!movieData) throw new Error('Filme não encontrado');
      setMovie(movieData);

      // Fetch user ratings resiliently
      try {
        const ratingsData = await ratingService.getMyRatings();
        const userRating = ratingsData?.find((r) => r.movieId === id);
        if (userRating) {
          setRating(userRating.type === 'like' || userRating.type === 'dislike' ? userRating.type : null);
        } else {
          setRating(null);
        }
      } catch (ratingErr) {
        // Fallback silently if ratings call fails (e.g. token expired)
        setRating(null);
      }
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Erro ao carregar filme');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadMovie();
  }, [id]);

  const toggleWatchlist = async () => {
    if (!movie) return;
    if (isSaved) {
      await removeFromWatchlist(id);
    } else {
      await addToWatchlist(movie);
    }
  };

  const handleRate = async (type: 'like' | 'dislike') => {
    if (isRating) return;
    setIsRating(true);
    try {
      if (rating === type) {
        // undo rating - call DELETE on backend
        await ratingService.deleteRating(id);
        setRating(null);
      } else {
        await ratingService.rateMovie(id, type);
        setRating(type);
      }
    } catch (err) {
      // handle error silently
    } finally {
      setIsRating(false);
    }
  };

  if (isLoading) {
    return (
      <View style={[styles.container, { justifyContent: 'center' }]}>
        <LoadingSpinner />
      </View>
    );
  }

  if (error || !movie) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.headerAbsolute}>
          <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
            <ArrowLeft size={24} color={colors.white} />
          </TouchableOpacity>
        </View>
        <ErrorMessage message={error || 'Filme não encontrado'} onRetry={loadMovie} />
      </SafeAreaView>
    );
  }

  return (
    <View style={styles.container}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scrollContent}>
        <View style={styles.heroSection}>
          <ImageFallback source={{ uri: movie.poster }} style={styles.poster} />
          <View style={styles.heroOverlay} />
          
          <SafeAreaView edges={['top']} style={styles.headerAbsolute}>
            <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
              <ArrowLeft size={24} color={colors.white} />
            </TouchableOpacity>
            <TouchableOpacity onPress={toggleWatchlist} style={styles.bookmarkButton}>
              <Bookmark size={24} color={isSaved ? colors.primary : colors.white} fill={isSaved ? colors.primary : 'transparent'} />
            </TouchableOpacity>
          </SafeAreaView>

          <View style={styles.playButtonContainer}>
            <TouchableOpacity 
              style={styles.playButton} 
              activeOpacity={0.8}
              onPress={() => {
                if (movie) {
                  const query = encodeURIComponent(movie.title + ' trailer');
                  Linking.openURL(`https://www.youtube.com/results?search_query=${query}`);
                }
              }}
            >
              <Play size={32} color={colors.primaryForeground} fill={colors.primaryForeground} style={{ marginLeft: 4 }} />
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.content}>
          <Text style={styles.title}>{movie.title}</Text>
          
            <View style={styles.metadataRow}>
            <View style={styles.metadataItem}>
              <Star size={16} color={colors.primary} fill={colors.primary} />
              <Text style={styles.metadataText}>{movie.rating.toFixed(1)}</Text>
            </View>
            <View style={styles.metadataDot} />
            <View style={styles.metadataItem}>
              <Calendar size={16} color={colors.mutedForeground} />
              <Text style={styles.metadataText}>{movie.year}</Text>
            </View>
          </View>

          <View style={styles.genreRow}>
            <View style={styles.genreChip}>
              <Text style={styles.genreText}>{movie.genre}</Text>
            </View>
          </View>

          <View style={styles.actionsRow}>
            <TouchableOpacity 
              style={[styles.actionBtn, rating === 'like' && styles.actionBtnActiveLike]} 
              activeOpacity={0.7}
              onPress={() => handleRate('like')}
            >
              <ThumbsUp size={20} color={rating === 'like' ? colors.primary : colors.mutedForeground} />
              <Text style={[styles.actionBtnText, rating === 'like' && styles.actionBtnTextActive]}>Gostei</Text>
            </TouchableOpacity>
            
            <TouchableOpacity 
              style={[styles.actionBtn, rating === 'dislike' && styles.actionBtnActiveDislike]} 
              activeOpacity={0.7}
              onPress={() => handleRate('dislike')}
            >
              <ThumbsDown size={20} color={rating === 'dislike' ? colors.red400 : colors.mutedForeground} />
              <Text style={[styles.actionBtnText, rating === 'dislike' && { color: colors.red400 }]}>Não Gostei</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Sinopse</Text>
            <Text style={styles.synopsis}>{movie.synopsis}</Text>
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Elenco Principal</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.castList}>
              {movie.cast.map((actor, index) => (
                <View key={index} style={styles.castItem}>
                  <View style={styles.castAvatar}>
                    <Text style={styles.castInitial}>{actor.charAt(0)}</Text>
                  </View>
                  <Text style={styles.castName} numberOfLines={2}>{actor}</Text>
                </View>
              ))}
            </ScrollView>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  scrollContent: { paddingBottom: 32 },
  heroSection: { width: '100%', height: width * 1.2, position: 'relative' },
  poster: { width: '100%', height: '100%' },
  heroOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.5)' },
  headerAbsolute: { position: 'absolute', top: 0, left: 0, right: 0, flexDirection: 'row', justifyContent: 'space-between', paddingHorizontal: 24, paddingTop: Platform.OS === 'ios' ? 32 : 16, zIndex: 10 },
  backButton: { width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
  bookmarkButton: { width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
  playButtonContainer: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, justifyContent: 'center', alignItems: 'center' },
  playButton: { width: 80, height: 80, borderRadius: 40, backgroundColor: colors.primary, justifyContent: 'center', alignItems: 'center', shadowColor: colors.primary, shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.4, shadowRadius: 16, elevation: 10 },
  content: { paddingHorizontal: 24, marginTop: -40 },
  title: { color: colors.white, fontSize: 32, fontWeight: '700', marginBottom: 16 },
  metadataRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 16 },
  metadataItem: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  metadataText: { color: colors.mutedForeground, fontSize: 14, fontWeight: '500' },
  metadataDot: { width: 4, height: 4, borderRadius: 2, backgroundColor: colors.mutedForeground },
  genreRow: { flexDirection: 'row', marginBottom: 24 },
  genreChip: { backgroundColor: 'rgba(212,160,23,0.1)', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 8, borderWidth: 1, borderColor: 'rgba(212,160,23,0.3)' },
  genreText: { color: colors.primary, fontSize: 14, fontWeight: '500' },
  actionsRow: { flexDirection: 'row', gap: 16, marginBottom: 32 },
  actionBtn: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, paddingVertical: 14, borderRadius: 12, backgroundColor: colors.secondary, borderWidth: 1, borderColor: colors.border },
  actionBtnActiveLike: { backgroundColor: 'rgba(212,160,23,0.1)', borderColor: colors.primary },
  actionBtnActiveDislike: { backgroundColor: 'rgba(239,68,68,0.1)', borderColor: colors.red400 },
  actionBtnText: { color: colors.mutedForeground, fontSize: 16, fontWeight: '500' },
  actionBtnTextActive: { color: colors.primary },
  section: { marginBottom: 32 },
  sectionTitle: { color: colors.white, fontSize: 20, fontWeight: '600', marginBottom: 16 },
  synopsis: { color: colors.mutedForeground, fontSize: 16, lineHeight: 24 },
  castList: { gap: 16, paddingRight: 24 },
  castItem: { width: 80, alignItems: 'center', gap: 8 },
  castAvatar: { width: 80, height: 80, borderRadius: 40, backgroundColor: colors.secondary, justifyContent: 'center', alignItems: 'center', borderWidth: 1, borderColor: colors.border },
  castInitial: { color: colors.mutedForeground, fontSize: 24, fontWeight: '600' },
  castName: { color: colors.white, fontSize: 12, textAlign: 'center', fontWeight: '500' },
});
