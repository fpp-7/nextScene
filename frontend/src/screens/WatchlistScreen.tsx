import React from 'react';
import { View, Text, StyleSheet, FlatList, RefreshControl } from 'react-native';
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
      {/* FlatList em vez de ScrollView + map: a watchlist cresce sem limite e
          renderizar tudo de uma vez trava a rolagem. */}
      <FlatList
        data={items}
        keyExtractor={(movie) => String(movie.id)}
        numColumns={2}
        columnWrapperStyle={styles.columnWrapper}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl refreshing={isLoading} onRefresh={refreshWatchlist} tintColor={colors.primary} />
        }
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.title}>Minha Lista</Text>
            <Text style={styles.subtitle}>
              {items.length} {items.length === 1 ? 'filme' : 'filmes'}
            </Text>
          </View>
        }
        ListEmptyComponent={
          <EmptyState
            Icon={Bookmark}
            title="Nenhum filme salvo"
            description="Adicione filmes à sua watchlist tocando no ícone de bookmark"
          />
        }
        renderItem={({ item }) => (
          <MovieCard movie={item} onPress={(id) => navigation.navigate('MovieDetails', { id })} />
        )}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: { paddingTop: 16, marginBottom: 24 },
  title: { color: colors.white, fontSize: 32, fontWeight: '700' },
  subtitle: { color: colors.mutedForeground, fontSize: 14, marginTop: 4 },
  listContent: { paddingHorizontal: 24, paddingBottom: 24 },
  columnWrapper: { justifyContent: 'space-between' },
});
