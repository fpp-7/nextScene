import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Star } from 'lucide-react-native';
import { colors } from '../theme/colors';
import { Movie } from '../types';
import { ImageFallback } from './ImageFallback';
import { useCardLayout } from '../utils/layout';

interface Props {
  movie: Movie;
  onPress: (id: number) => void;
}

export function MovieCard({ movie, onPress }: Props) {
  // Do hook, não de `Dimensions.get('window')` no escopo do módulo: aquele
  // valor era congelado no primeiro render e assumia sempre duas colunas. Numa
  // janela de 1920px cada card ficava com 928px de largura e, pelo aspect ratio
  // 2/3, quase 1400px de altura.
  const { cardWidth } = useCardLayout();

  return (
    <TouchableOpacity
      style={[styles.card, { width: cardWidth }]}
      onPress={() => onPress(movie.id)}
      activeOpacity={0.8}
      accessibilityRole="button"
      accessibilityLabel={`${movie.title}. Abrir detalhes`}
    >
      <View style={styles.posterContainer}>
        <ImageFallback 
          source={{ uri: movie.poster }} 
          style={styles.poster} 
        />
        {/* Sem nota, não há selo: filme ainda não enriquecido via TMDB
            chega com 0, e "0.0" parece nota péssima em vez de nota ausente. */}
        {movie.rating > 0 && (
          <View style={styles.ratingBadge}>
            <Star size={12} color={colors.primaryForeground} fill={colors.primaryForeground} />
            <Text style={styles.ratingText}>{movie.rating.toFixed(1)}</Text>
          </View>
        )}
      </View>
      <Text style={styles.title} numberOfLines={1}>{movie.title}</Text>
      <Text style={styles.genre} numberOfLines={1}>{movie.genre}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    marginBottom: 20,
  },
  posterContainer: {
    width: '100%',
    aspectRatio: 2 / 3,
    borderRadius: 16,
    overflow: 'hidden',
    marginBottom: 12,
  },
  poster: {
    width: '100%',
    height: '100%',
  },
  ratingBadge: {
    position: 'absolute',
    top: 8,
    right: 8,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.primary,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    gap: 4,
  },
  ratingText: {
    color: colors.primaryForeground,
    fontSize: 12,
    fontWeight: '600',
  },
  title: {
    color: colors.foreground,
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 4,
  },
  genre: {
    color: colors.mutedForeground,
    fontSize: 14,
  },
});
