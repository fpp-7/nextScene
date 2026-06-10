import React, { useState } from 'react';
import {
  View, Text, TouchableOpacity, StyleSheet, ScrollView, Image, Dimensions,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Star, ThumbsUp, ThumbsDown, Check } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { MOVIES } from '../data/mock-data';
import { RootStackParamList } from '../navigation/types';
import { useAuth } from '../contexts/AuthContext';
import { ratingService } from '../services/ratingService';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ColdStartRating } from '../types';

const { width } = Dimensions.get('window');
const CARD_WIDTH = (width - 48 - 16) / 2;

type Props = NativeStackScreenProps<RootStackParamList, 'OnboardingColdStart'>;

export function OnboardingColdStartScreen({ navigation }: Props) {
  const { completeOnboarding } = useAuth();
  const [ratings, setRatings] = useState<Record<number, 'like' | 'dislike' | 'seen'>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const setRating = (id: number, type: 'like' | 'dislike' | 'seen') => {
    setRatings((r) => ({ ...r, [id]: r[id] === type ? undefined! : type }));
  };

  const handleFinish = async () => {
    setIsSubmitting(true);
    try {
      const payload: ColdStartRating[] = Object.entries(ratings)
        .filter(([_, type]) => type !== undefined)
        .map(([id, type]) => ({ movieId: Number(id), type }));
      
      await ratingService.submitColdStart(payload);
      await completeOnboarding(); // Triggers navigation to MainTabs
    } catch (err) {
      // could handle error
    } finally {
      setIsSubmitting(false);
    }
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
        <ScrollView style={{ flex: 1 }} showsVerticalScrollIndicator={false}>
          <View style={styles.grid}>
            {MOVIES.slice(0, 8).map((movie) => (
              <View key={movie.id} style={styles.movieItem}>
                <View style={styles.posterWrap}>
                  <Image source={{ uri: movie.poster }} style={styles.poster} />
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
        <TouchableOpacity 
          style={styles.button} 
          onPress={handleFinish} 
          disabled={isSubmitting}
          activeOpacity={0.8}
        >
          {isSubmitting ? (
            <LoadingSpinner size="small" />
          ) : (
            <Text style={styles.buttonText}>Começar a Explorar</Text>
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
  buttonText: { color: colors.primaryForeground, fontSize: 16, fontWeight: '500' },
});
