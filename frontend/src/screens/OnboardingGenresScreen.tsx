import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ArrowRight, Heart, X } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { GENRES } from '../data/mock-data';
import { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'OnboardingGenres'>;

export function OnboardingGenresScreen({ navigation }: Props) {
  const [liked, setLiked] = useState<string[]>([]);
  const [disliked, setDisliked] = useState<string[]>([]);

  const toggle = (genre: string, list: 'liked' | 'disliked') => {
    if (list === 'liked') {
      setDisliked((d) => d.filter((g) => g !== genre));
      setLiked((l) => l.includes(genre) ? l.filter((g) => g !== genre) : [...l, genre]);
    } else {
      setLiked((l) => l.filter((g) => g !== genre));
      setDisliked((d) => d.includes(genre) ? d.filter((g) => g !== genre) : [...d, genre]);
    }
  };

  const getState = (genre: string) => {
    if (liked.includes(genre)) return 'liked';
    if (disliked.includes(genre)) return 'disliked';
    return 'neutral';
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.step}>Etapa 1 de 2</Text>
          <Text style={styles.title}>Seus Generos</Text>
          <Text style={styles.description}>
            Toque uma vez para{' '}
            <Text style={{ color: colors.primary }}>curtir</Text>, duas vezes para{' '}
            <Text style={{ color: colors.red400 }}>excluir</Text>
          </Text>
        </View>

        {/* Progress */}
        <View style={styles.progressRow}>
          <View style={[styles.progressBar, { backgroundColor: colors.primary }]} />
          <View style={[styles.progressBar, { backgroundColor: colors.secondary }]} />
        </View>

        {/* Genre grid */}
        <ScrollView style={styles.genreScroll} showsVerticalScrollIndicator={false}>
          <View style={styles.genreGrid}>
            {GENRES.map((genre) => {
              const state = getState(genre);
              return (
                <TouchableOpacity
                  key={genre}
                  onPress={() => {
                    if (state === 'neutral') toggle(genre, 'liked');
                    else if (state === 'liked') toggle(genre, 'disliked');
                    else setDisliked((d) => d.filter((g) => g !== genre));
                  }}
                  style={[
                    styles.genreChip,
                    state === 'liked' && styles.genreChipLiked,
                    state === 'disliked' && styles.genreChipDisliked,
                  ]}
                  activeOpacity={0.7}
                >
                  {state === 'liked' && <Heart size={14} color={colors.primary} fill={colors.primary} />}
                  {state === 'disliked' && <X size={14} color={colors.red400} />}
                  <Text
                    style={[
                      styles.genreText,
                      state === 'liked' && styles.genreTextLiked,
                      state === 'disliked' && styles.genreTextDisliked,
                    ]}
                  >
                    {genre}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </ScrollView>

        {/* CTA */}
        <TouchableOpacity
          style={styles.button}
          onPress={() => navigation.navigate('OnboardingColdStart')}
          activeOpacity={0.8}
        >
          <Text style={styles.buttonText}>Continuar</Text>
          <ArrowRight size={20} color={colors.primaryForeground} />
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    flex: 1,
    paddingHorizontal: 24,
    paddingTop: 16,
  },
  header: {
    gap: 8,
    marginBottom: 8,
  },
  step: {
    color: colors.primary,
    fontSize: 14,
  },
  title: {
    color: colors.white,
    fontSize: 24,
    fontWeight: '500',
  },
  description: {
    color: colors.mutedForeground,
    fontSize: 14,
  },
  progressRow: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 24,
    marginTop: 8,
  },
  progressBar: {
    flex: 1,
    height: 4,
    borderRadius: 2,
  },
  genreScroll: {
    flex: 1,
  },
  genreGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
    paddingBottom: 16,
  },
  genreChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.secondary,
  },
  genreChipLiked: {
    borderColor: colors.primary,
    backgroundColor: 'rgba(212,160,23,0.2)',
  },
  genreChipDisliked: {
    borderColor: 'rgba(239,68,68,0.5)',
    backgroundColor: 'rgba(239,68,68,0.1)',
  },
  genreText: {
    color: 'rgba(255,255,255,0.7)',
    fontSize: 14,
  },
  genreTextLiked: {
    color: colors.primary,
  },
  genreTextDisliked: {
    color: colors.red400,
  },
  button: {
    flexDirection: 'row',
    backgroundColor: colors.primary,
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    marginTop: 8,
    marginBottom: 16,
  },
  buttonText: {
    color: colors.primaryForeground,
    fontSize: 16,
    fontWeight: '500',
  },
});
