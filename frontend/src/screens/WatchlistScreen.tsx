import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { Bookmark } from 'lucide-react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { MOVIES } from '../data/mock-data';
import { MovieCard } from '../components/MovieCard';
import { RootStackParamList } from '../navigation/types';

export function WatchlistScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const saved = [MOVIES[0], MOVIES[2], MOVIES[4], MOVIES[7]];

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>
        <View style={styles.headerRow}>
          <Bookmark size={20} color={colors.primary} />
          <Text style={styles.headerTitle}>Watchlist</Text>
        </View>
        <Text style={styles.count}>{saved.length} filmes salvos</Text>
        <View style={styles.grid}>
          {saved.map((m) => (
            <MovieCard key={m.id} movie={m} onPress={() => navigation.navigate('MovieDetails', { id: m.id })} />
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  scroll: { paddingHorizontal: 24, paddingBottom: 100 },
  headerRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 24 },
  headerTitle: { color: colors.white, fontSize: 24, fontWeight: '500' },
  count: { color: colors.mutedForeground, fontSize: 14, marginBottom: 24 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 16 },
});
