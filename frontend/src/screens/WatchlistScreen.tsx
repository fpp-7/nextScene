import React from 'react';
import { View, Text, StyleSheet, ScrollView, RefreshControl } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Bookmark } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { MovieCard } from '../components/MovieCard';
import { RootStackParamList, TabParamList } from '../navigation/types';
import { useWatchlist } from '../contexts/WatchlistContext';
import { EmptyState } from '../components/EmptyState';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';

import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';

type Props = CompositeScreenProps<
  BottomTabScreenProps<TabParamList, 'Watchlist'>,
  NativeStackScreenProps<RootStackParamList>
>;

export function WatchlistScreen({ navigation }: Props) {
  const { items, isLoading, error, refreshWatchlist } = useWatchlist();

  if (isLoading && items.length === 0) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Minha Lista</Text>
        </View>
        <LoadingSpinner />
      </SafeAreaView>
    );
  }

  if (error && items.length === 0) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Minha Lista</Text>
        </View>
        <ErrorMessage message={error} onRetry={refreshWatchlist} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView 
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={refreshWatchlist} tintColor={colors.primary} />}
      >
        <View style={styles.header}>
          <Text style={styles.title}>Minha Lista</Text>
          <Text style={styles.subtitle}>{items.length} {items.length === 1 ? 'filme' : 'filmes'}</Text>
        </View>

        {items.length === 0 ? (
          <EmptyState 
            Icon={Bookmark} 
            title="Nenhum filme salvo" 
            description="Adicione filmes a sua watchlist tocando no icone de bookmark" 
          />
        ) : (
          <View style={styles.grid}>
            {items.map((movie) => (
              <MovieCard
                key={movie.id}
                movie={movie}
                onPress={(id) => navigation.navigate('MovieDetails', { id })}
              />
            ))}
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: { paddingHorizontal: 24, paddingTop: 16, marginBottom: 24 },
  title: { color: colors.white, fontSize: 32, fontWeight: '700' },
  subtitle: { color: colors.mutedForeground, fontSize: 14, marginTop: 4 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', paddingHorizontal: 24 },
});
