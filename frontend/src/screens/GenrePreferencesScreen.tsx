import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ArrowLeft, Heart, X } from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { GENRES } from '../data/mock-data';
import { RootStackParamList } from '../navigation/types';
import { userService } from '../services/userService';
import { LoadingSpinner } from '../components/LoadingSpinner';

type Props = NativeStackScreenProps<RootStackParamList, 'GenrePreferences'>;

export function GenrePreferencesScreen({ navigation }: Props) {
  const [liked, setLiked] = useState<string[]>([]);
  const [disliked, setDisliked] = useState<string[]>([]);
  const [isSaving, setIsSaving] = useState(false);
  const [isSaved, setIsSaved] = useState(false);

  // No mock load in mock data yet, starting fresh or assuming empty
  useEffect(() => {
    // In a real app we'd get existing prefs from user data
  }, []);

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

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await userService.updateGenres({ liked, disliked });
      setIsSaved(true);
      setTimeout(() => setIsSaved(false), 2000);
    } catch (e) {
      // handle error
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <ArrowLeft size={24} color={colors.white} />
        </TouchableOpacity>
        <Text style={styles.title}>Preferências de Gênero</Text>
      </View>
      <View style={styles.content}>
        <ScrollView style={styles.genreScroll} showsVerticalScrollIndicator={false}>
          <Text style={styles.description}>
            Toque uma vez para <Text style={{ color: colors.primary }}>curtir</Text>, duas vezes para{' '}
            <Text style={{ color: colors.red400 }}>excluir</Text>
          </Text>
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

        <TouchableOpacity
          style={styles.button}
          onPress={handleSave}
          disabled={isSaving}
          activeOpacity={0.8}
        >
          {isSaving ? (
            <LoadingSpinner size="small" />
          ) : (
            <Text style={styles.buttonText}>{isSaved ? 'Salvo!' : 'Salvar'}</Text>
          )}
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 24, paddingVertical: 16, gap: 16 },
  backBtn: { padding: 4 },
  title: { color: colors.white, fontSize: 20, fontWeight: '500' },
  content: { flex: 1, paddingHorizontal: 24, paddingBottom: 24 },
  description: { color: colors.mutedForeground, fontSize: 14, marginBottom: 16 },
  genreScroll: { flex: 1 },
  genreGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12, paddingBottom: 16 },
  genreChip: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16, paddingVertical: 12, borderRadius: 24, borderWidth: 1, borderColor: colors.border, backgroundColor: colors.secondary },
  genreChipLiked: { borderColor: colors.primary, backgroundColor: 'rgba(212,160,23,0.2)' },
  genreChipDisliked: { borderColor: 'rgba(239,68,68,0.5)', backgroundColor: 'rgba(239,68,68,0.1)' },
  genreText: { color: 'rgba(255,255,255,0.7)', fontSize: 14 },
  genreTextLiked: { color: colors.primary },
  genreTextDisliked: { color: colors.red400 },
  button: { backgroundColor: colors.primary, paddingVertical: 16, borderRadius: 14, alignItems: 'center', marginTop: 16 },
  buttonText: { color: colors.primaryForeground, fontSize: 16, fontWeight: '500' },
});
