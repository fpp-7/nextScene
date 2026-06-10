import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Dimensions } from 'react-native';
import { Star } from 'lucide-react-native';
import { colors } from '../theme/colors';
import { Movie } from '../types';
import { ImageFallback } from './ImageFallback';

const { width } = Dimensions.get('window');
const CARD_WIDTH = (width - 48 - 16) / 2;

interface Props {
  movie: Movie;
  onPress: (id: number) => void;
}

export function MovieCard({ movie, onPress }: Props) {
  return (
    <TouchableOpacity 
      style={styles.card} 
      onPress={() => onPress(movie.id)}
      activeOpacity={0.8}
    >
      <View style={styles.posterContainer}>
        <ImageFallback 
          source={{ uri: movie.poster }} 
          style={styles.poster} 
        />
        <View style={styles.ratingBadge}>
          <Star size={12} color={colors.primaryForeground} fill={colors.primaryForeground} />
          <Text style={styles.ratingText}>{movie.rating}</Text>
        </View>
      </View>
      <Text style={styles.title} numberOfLines={1}>{movie.title}</Text>
      <Text style={styles.genre} numberOfLines={1}>{movie.genre}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    width: CARD_WIDTH,
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
