import React from 'react';
import { View, Text, Image, TouchableOpacity, StyleSheet, Dimensions } from 'react-native';
import { Star, Bookmark } from 'lucide-react-native';
import { colors } from '../theme/colors';
import { Movie } from '../data/mock-data';

const { width } = Dimensions.get('window');
const CARD_WIDTH = (width - 48 - 16) / 2;

interface MovieCardProps {
  movie: Movie;
  onPress: () => void;
}

export function MovieCard({ movie, onPress }: MovieCardProps) {
  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.8}>
      <Image source={{ uri: movie.poster }} style={styles.poster} />
      <View style={styles.gradient} />
      <View style={styles.bookmark}>
        <Bookmark size={16} color={colors.white} />
      </View>
      <View style={styles.info}>
        <Text style={styles.title} numberOfLines={1}>{movie.title}</Text>
        <View style={styles.metaRow}>
          <View style={styles.ratingRow}>
            <Star size={12} color={colors.primary} fill={colors.primary} />
            <Text style={styles.rating}>{movie.rating}</Text>
          </View>
          <Text style={styles.year}>{movie.year}</Text>
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: { width: CARD_WIDTH, aspectRatio: 2 / 3, borderRadius: 14, overflow: 'hidden' },
  poster: { width: '100%', height: '100%' },
  gradient: { position: 'absolute', bottom: 0, left: 0, right: 0, height: '60%', backgroundColor: 'rgba(0,0,0,0.5)' },
  bookmark: { position: 'absolute', top: 8, right: 8, backgroundColor: 'rgba(0,0,0,0.5)', borderRadius: 20, padding: 6 },
  info: { position: 'absolute', bottom: 0, left: 0, right: 0, padding: 12 },
  title: { color: colors.white, fontSize: 14 },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 4 },
  ratingRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  rating: { color: colors.primary, fontSize: 12 },
  year: { color: colors.mutedForeground, fontSize: 12 },
});
