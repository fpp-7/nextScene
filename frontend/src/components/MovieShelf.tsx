import React from 'react';
import { View, Text, FlatList, StyleSheet, ActivityIndicator } from 'react-native';
import { colors } from '../theme/colors';
import { Movie } from '../types';
import { MovieCard } from './MovieCard';

interface Props {
  title: string;
  subtitle: string;
  movies: Movie[];
  isLoading: boolean;
  onPressMovie: (id: number) => void;
}

/**
 * Faixa horizontal de filmes.
 *
 * A tela Descobrir era uma grade única ordenada por nota, rotulada "Em Alta" —
 * na prática, os melhores de todos os tempos, sempre na mesma ordem. Separar em
 * prateleiras deixa cada faixa dizer o que de fato mostra.
 */
export function MovieShelf({ title, subtitle, movies, isLoading, onPressMovie }: Props) {
  if (!isLoading && movies.length === 0) {
    return null; // prateleira vazia não vira espaço morto na tela
  }

  return (
    <View style={styles.shelf}>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.subtitle}>{subtitle}</Text>

      {isLoading ? (
        <ActivityIndicator style={styles.loading} color={colors.primary} />
      ) : (
        <FlatList
          horizontal
          data={movies}
          keyExtractor={(movie) => `${title}-${movie.id}`}
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => (
            <View style={styles.item}>
              <MovieCard movie={item} onPress={onPressMovie} />
            </View>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  shelf: { marginBottom: 28 },
  title: { color: colors.white, fontSize: 20, fontWeight: '600' },
  subtitle: { color: colors.mutedForeground, fontSize: 13, marginTop: 2, marginBottom: 14 },
  list: { paddingRight: 24 },
  item: { marginRight: 16 },
  loading: { marginVertical: 40 },
});
