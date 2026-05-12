import React, { useState } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView, Image, Dimensions,
} from 'react-native';
import { Search, SlidersHorizontal } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { colors } from '../theme/colors';
import { MOVIES } from '../data/mock-data';
import { MovieCard } from '../components/MovieCard';
import { RootStackParamList } from '../navigation/types';

const FILTER_GENRES = ['Todos', 'Acao', 'Drama', 'Suspense', 'Ficcao Cientifica', 'Terror'];

export function DiscoverScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const [activeGenre, setActiveGenre] = useState('Todos');
  const featured = MOVIES[1];
  const filtered = activeGenre === 'Todos' ? MOVIES : MOVIES.filter((m) => m.genre === activeGenre);

  return (
    <View style={styles.container}>
      <ScrollView showsVerticalScrollIndicator={false}>
        {/* Featured */}
        <View style={styles.featured}>
          <Image source={{ uri: featured.poster }} style={styles.featuredImage} />
          <View style={styles.featuredOverlay} />
          <View style={styles.featuredInfo}>
            <View style={styles.genreBadge}>
              <Text style={styles.genreBadgeText}>{featured.genre}</Text>
            </View>
            <Text style={styles.featuredTitle}>{featured.title}</Text>
            <Text style={styles.featuredMeta}>IMDb {featured.imdb} · {featured.year}</Text>
          </View>
        </View>

        {/* Search */}
        <View style={styles.searchContainer}>
          <View style={styles.searchBar}>
            <Search size={20} color={colors.mutedForeground} />
            <TextInput
              placeholder="Buscar filmes..."
              placeholderTextColor={colors.mutedForeground}
              style={styles.searchInput}
            />
            <SlidersHorizontal size={20} color={colors.mutedForeground} />
          </View>
        </View>

        {/* Genre filter */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterScroll} contentContainerStyle={styles.filterContent}>
          {FILTER_GENRES.map((g) => (
            <TouchableOpacity
              key={g}
              onPress={() => setActiveGenre(g)}
              style={[styles.filterChip, activeGenre === g && styles.filterChipActive]}
              activeOpacity={0.7}
            >
              <Text style={[styles.filterText, activeGenre === g && styles.filterTextActive]}>{g}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* Movies */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Aclamados pela Critica</Text>
          <View style={styles.grid}>
            {filtered.map((movie) => (
              <MovieCard key={movie.id} movie={movie} onPress={() => navigation.navigate('MovieDetails', { id: movie.id })} />
            ))}
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  featured: { height: 288, position: 'relative' },
  featuredImage: { width: '100%', height: '100%' },
  featuredOverlay: { position: 'absolute', top: 0, bottom: 0, left: 0, right: 0, backgroundColor: 'rgba(10,10,10,0.4)' },
  featuredInfo: { position: 'absolute', bottom: 24, left: 24, right: 24 },
  genreBadge: { backgroundColor: 'rgba(212,160,23,0.2)', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 12, alignSelf: 'flex-start' },
  genreBadgeText: { color: colors.primary, fontSize: 12 },
  featuredTitle: { color: colors.white, fontSize: 20, fontWeight: '500', marginTop: 8 },
  featuredMeta: { color: colors.mutedForeground, fontSize: 14, marginTop: 4 },
  searchContainer: { paddingHorizontal: 24, marginTop: 16 },
  searchBar: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.secondary, borderRadius: 14, paddingHorizontal: 16, paddingVertical: 14 },
  searchInput: { flex: 1, color: colors.white, fontSize: 14 },
  filterScroll: { marginTop: 16 },
  filterContent: { paddingHorizontal: 24, gap: 8 },
  filterChip: { paddingHorizontal: 16, paddingVertical: 10, borderRadius: 20, backgroundColor: colors.secondary },
  filterChipActive: { backgroundColor: colors.primary },
  filterText: { color: colors.mutedForeground, fontSize: 14 },
  filterTextActive: { color: colors.primaryForeground },
  section: { paddingHorizontal: 24, marginTop: 24, paddingBottom: 100 },
  sectionTitle: { color: colors.white, fontSize: 18, fontWeight: '500', marginBottom: 12 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 16 },
});
