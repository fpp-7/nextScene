import React, { useState } from 'react';
import {
  View, Text, TouchableOpacity, StyleSheet, ScrollView, Image, Dimensions,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ArrowLeft, Star, Bookmark, ThumbsUp, ThumbsDown, Share2 } from 'lucide-react-native';
import { colors } from '../theme/colors';
import { MOVIES } from '../data/mock-data';
import { RootStackParamList } from '../navigation/types';

const { width } = Dimensions.get('window');

type Props = NativeStackScreenProps<RootStackParamList, 'MovieDetails'>;

export function MovieDetailsScreen({ navigation, route }: Props) {
  const movie = MOVIES.find((m) => m.id === route.params.id) || MOVIES[0];
  const [saved, setSaved] = useState(false);

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      {/* Hero */}
      <View style={styles.hero}>
        <Image source={{ uri: movie.poster }} style={styles.heroImage} />
        <View style={styles.heroOverlay} />
        <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()} activeOpacity={0.7}>
          <ArrowLeft size={20} color={colors.white} />
        </TouchableOpacity>
        <TouchableOpacity style={styles.saveBtn} onPress={() => setSaved(!saved)} activeOpacity={0.7}>
          <Bookmark size={20} color={saved ? colors.primary : colors.white} fill={saved ? colors.primary : 'transparent'} />
        </TouchableOpacity>
      </View>

      {/* Content */}
      <View style={styles.content}>
        <View style={styles.genreBadge}>
          <Text style={styles.genreBadgeText}>{movie.genre}</Text>
        </View>
        <Text style={styles.title}>{movie.title}</Text>
        <Text style={styles.year}>{movie.year}</Text>

        {/* Ratings */}
        <View style={styles.ratingsRow}>
          <View style={styles.ratingItem}>
            <Star size={20} color={colors.primary} fill={colors.primary} />
            <View>
              <Text style={styles.ratingValue}>{movie.imdb}</Text>
              <Text style={styles.ratingLabel}>IMDb</Text>
            </View>
          </View>
          <View style={styles.ratingItem}>
            <Star size={20} color={colors.primary} fill={colors.primary} />
            <View>
              <Text style={styles.ratingValue}>{movie.rating}</Text>
              <Text style={styles.ratingLabel}>NextScene</Text>
            </View>
          </View>
        </View>

        {/* Actions */}
        <View style={styles.actionsRow}>
          <TouchableOpacity style={styles.likeBtn} activeOpacity={0.8}>
            <ThumbsUp size={16} color={colors.primaryForeground} />
            <Text style={styles.likeBtnText}>Curti</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.dislikeBtn} activeOpacity={0.8}>
            <ThumbsDown size={16} color={colors.mutedForeground} />
            <Text style={styles.dislikeBtnText}>Nao Curti</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.shareBtn} activeOpacity={0.8}>
            <Share2 size={16} color={colors.mutedForeground} />
          </TouchableOpacity>
        </View>

        {/* Synopsis */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Sinopse</Text>
          <Text style={styles.synopsis}>{movie.synopsis}</Text>
        </View>

        {/* Cast */}
        <View style={[styles.section, { paddingBottom: 40 }]}>
          <Text style={styles.sectionTitle}>Elenco</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            <View style={styles.castRow}>
              {movie.cast.map((actor) => (
                <View key={actor} style={styles.castChip}>
                  <Text style={styles.castName}>{actor}</Text>
                </View>
              ))}
            </View>
          </ScrollView>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  hero: { height: 384, position: 'relative' },
  heroImage: { width: '100%', height: '100%' },
  heroOverlay: { position: 'absolute', top: 0, bottom: 0, left: 0, right: 0, backgroundColor: 'rgba(10,10,10,0.3)' },
  backBtn: { position: 'absolute', top: 48, left: 16, backgroundColor: 'rgba(0,0,0,0.5)', borderRadius: 20, padding: 8 },
  saveBtn: { position: 'absolute', top: 48, right: 16, backgroundColor: 'rgba(0,0,0,0.5)', borderRadius: 20, padding: 8 },
  content: { paddingHorizontal: 24, marginTop: -64, position: 'relative', zIndex: 10 },
  genreBadge: { backgroundColor: 'rgba(212,160,23,0.2)', paddingHorizontal: 12, paddingVertical: 4, borderRadius: 12, alignSelf: 'flex-start' },
  genreBadgeText: { color: colors.primary, fontSize: 12 },
  title: { color: colors.white, fontSize: 24, fontWeight: '500', marginTop: 12 },
  year: { color: colors.mutedForeground, fontSize: 14, marginTop: 4 },
  ratingsRow: { flexDirection: 'row', gap: 24, marginTop: 16 },
  ratingItem: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  ratingValue: { color: colors.white, fontSize: 14 },
  ratingLabel: { color: colors.mutedForeground, fontSize: 10 },
  actionsRow: { flexDirection: 'row', gap: 12, marginTop: 24 },
  likeBtn: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, backgroundColor: colors.primary, paddingVertical: 14, borderRadius: 14 },
  likeBtnText: { color: colors.primaryForeground, fontSize: 16, fontWeight: '500' },
  dislikeBtn: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, backgroundColor: colors.secondary, paddingVertical: 14, borderRadius: 14 },
  dislikeBtnText: { color: colors.mutedForeground, fontSize: 16, fontWeight: '500' },
  shareBtn: { backgroundColor: colors.secondary, padding: 14, borderRadius: 14, alignItems: 'center', justifyContent: 'center' },
  section: { marginTop: 24 },
  sectionTitle: { color: colors.white, fontSize: 18, fontWeight: '500', marginBottom: 8 },
  synopsis: { color: colors.mutedForeground, fontSize: 14, lineHeight: 22 },
  castRow: { flexDirection: 'row', gap: 12 },
  castChip: { backgroundColor: colors.secondary, borderRadius: 14, paddingHorizontal: 16, paddingVertical: 10 },
  castName: { color: colors.white, fontSize: 14 },
});
