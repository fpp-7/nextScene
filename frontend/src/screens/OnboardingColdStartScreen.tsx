import React, { useState, useEffect } from 'react';
import { messageFor } from '../services/errors';
import {
  View, Text, TouchableOpacity, StyleSheet, ScrollView, Dimensions, Alert,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Star, ThumbsUp, ThumbsDown, Check } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { RootStackParamList } from '../navigation/types';
import { useAuth } from '../contexts/AuthContext';
import { ratingService } from '../services/ratingService';
import { movieService } from '../services/movieService';
import { ImageFallback } from '../components/ImageFallback';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ColdStartRating, Movie } from '../types';

const { width } = Dimensions.get('window');
const CARD_WIDTH = (width - 48 - 16) / 2;

type Props = NativeStackScreenProps<RootStackParamList, 'OnboardingColdStart'>;

export function OnboardingColdStartScreen({ navigation }: Props) {
  const { completeOnboarding } = useAuth();
  const [ratings, setRatings] = useState<Record<number, 'like' | 'dislike' | 'seen'>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [movies, setMovies] = useState<Movie[]>([]);
  const [isLoadingMovies, setIsLoadingMovies] = useState(true);

  useEffect(() => {
    async function fetchMovies() {
      try {
        const data = await movieService.getMovies();
        setMovies(data);
      } catch (err) {
        console.error('Error fetching onboarding movies:', err);
      } finally {
        setIsLoadingMovies(false);
      }
    }
    fetchMovies();
  }, []);

  const setRating = (id: number, type: 'like' | 'dislike' | 'seen') => {
    setRatings((current) => {
      const next = { ...current };
      if (next[id] === type) {
        // Remove a chave em vez de gravar `undefined!`, que vazava para o payload.
        delete next[id];
      } else {
        next[id] = type;
      }
      return next;
    });
  };

  const ratedCount = Object.keys(ratings).length;

  const handleFinish = async () => {
    setIsSubmitting(true);
    try {
      const payload: ColdStartRating[] = Object.entries(ratings).map(([id, type]) => ({
        movieId: Number(id),
        type,
      }));

      // Grava as avaliações antes de concluir: são elas que alimentam todas as
      // recomendações seguintes.
      await ratingService.submitColdStart(payload);
      await completeOnboarding(); // Dispara a navegação para MainTabs
    } catch (err: any) {
      const errorMsg = messageFor(err, 'Erro ao enviar avaliações iniciais.');
      Alert.alert('Erro', errorMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSkip = () => {
    Alert.alert(
      'Pular esta etapa?',
      'Sem avaliações iniciais, suas primeiras recomendações serão genéricas. Você pode avaliar filmes depois, a qualquer momento.',
      [
        { text: 'Continuar avaliando', style: 'cancel' },
        { text: 'Pular', onPress: () => completeOnboarding() },
      ]
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <View style={styles.header}>
          <Text style={styles.step}>Etapa 2 de 2</Text>
          <Text style={styles.title}>Avalie Filmes</Text>
          <Text style={styles.desc}>Avalie alguns filmes para calibrar suas recomendações</Text>
        </View>
        <View style={styles.progressRow}>
          <View style={[styles.bar, { backgroundColor: colors.primary }]} />
          <View style={[styles.bar, { backgroundColor: colors.primary }]} />
        </View>
        {isLoadingMovies ? (
          <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
            <LoadingSpinner size="large" />
          </View>
        ) : (
          <ScrollView style={{ flex: 1 }} showsVerticalScrollIndicator={false}>
            <View style={styles.grid}>
              {movies.slice(0, 8).map((movie) => (
                <View key={movie.id} style={styles.movieItem}>
                  <View style={styles.posterWrap}>
                    <ImageFallback source={{ uri: movie.poster }} style={styles.poster} />
                    <View style={styles.posterInfo}>
                      <Text style={styles.posterTitle} numberOfLines={1}>{movie.title}</Text>
                      <View style={styles.ratingRow}>
                        <Star size={12} color={colors.primary} fill={colors.primary} />
                        <Text style={styles.ratingText}>{movie.rating}</Text>
                      </View>
                    </View>
                  </View>
                  <View style={styles.actionsRow}>
                    {(['like', 'dislike', 'seen'] as const).map((type) => (
                      <TouchableOpacity
                        key={type}
                        onPress={() => setRating(movie.id, type)}
                        style={[styles.actionBtn, ratings[movie.id] === type && (
                          type === 'like' ? styles.liked : type === 'dislike' ? styles.disliked : styles.seen
                        )]}
                        activeOpacity={0.7}
                        accessibilityRole="button"
                        accessibilityState={{ selected: ratings[movie.id] === type }}
                        accessibilityLabel={
                          `${type === 'like' ? 'Gostei de' : type === 'dislike' ? 'Não gostei de' : 'Já assisti'} ${movie.title}`
                        }
                      >
                        {type === 'like' && <ThumbsUp size={16} color={ratings[movie.id] === 'like' ? colors.primary : colors.mutedForeground} />}
                        {type === 'dislike' && <ThumbsDown size={16} color={ratings[movie.id] === 'dislike' ? colors.red400 : colors.mutedForeground} />}
                        {type === 'seen' && <Check size={16} color={ratings[movie.id] === 'seen' ? colors.white : colors.mutedForeground} />}
                      </TouchableOpacity>
                    ))}
                  </View>
                </View>
              ))}
            </View>
          </ScrollView>
        )}
        <TouchableOpacity
          style={[styles.button, ratedCount === 0 && styles.buttonDisabled]}
          onPress={ratedCount === 0 ? handleSkip : handleFinish}
          disabled={isSubmitting}
          activeOpacity={0.8}
          accessibilityRole="button"
          accessibilityLabel={
            ratedCount === 0
              ? 'Pular avaliações e começar a explorar'
              : `Concluir com ${ratedCount} ${ratedCount === 1 ? 'filme avaliado' : 'filmes avaliados'}`
          }
        >
          {isSubmitting ? (
            <LoadingSpinner size="small" />
          ) : (
            <Text style={styles.buttonText}>
              {ratedCount === 0 ? 'Pular por agora' : `Começar a Explorar (${ratedCount})`}
            </Text>
          )}
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  content: { flex: 1, paddingHorizontal: 24, paddingTop: 16 },
  header: { gap: 8, marginBottom: 8 },
  step: { color: colors.primary, fontSize: 14 },
  title: { color: colors.white, fontSize: 24, fontWeight: '500' },
  desc: { color: colors.mutedForeground, fontSize: 14 },
  progressRow: { flexDirection: 'row', gap: 8, marginBottom: 24, marginTop: 8 },
  bar: { flex: 1, height: 4, borderRadius: 2 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 16, paddingBottom: 16 },
  movieItem: { width: CARD_WIDTH, gap: 8 },
  posterWrap: { width: '100%', aspectRatio: 2 / 3, borderRadius: 14, overflow: 'hidden' },
  poster: { width: '100%', height: '100%' },
  posterInfo: { position: 'absolute', bottom: 0, left: 0, right: 0, padding: 12, backgroundColor: 'rgba(0,0,0,0.6)' },
  posterTitle: { color: colors.white, fontSize: 12 },
  ratingRow: { flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: 2 },
  ratingText: { color: colors.primary, fontSize: 12 },
  actionsRow: { flexDirection: 'row', gap: 8 },
  actionBtn: { flex: 1, paddingVertical: 8, borderRadius: 8, alignItems: 'center', backgroundColor: colors.secondary },
  liked: { backgroundColor: 'rgba(212,160,23,0.2)' },
  disliked: { backgroundColor: 'rgba(239,68,68,0.2)' },
  seen: { backgroundColor: 'rgba(255,255,255,0.2)' },
  button: { backgroundColor: colors.primary, paddingVertical: 16, borderRadius: 14, alignItems: 'center', marginTop: 8, marginBottom: 16 },
  buttonDisabled: { backgroundColor: colors.secondary },
  buttonText: { color: colors.primaryForeground, fontSize: 16, fontWeight: '500' },
});
