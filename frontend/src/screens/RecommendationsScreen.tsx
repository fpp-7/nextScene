import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { Sparkles, RefreshCw } from 'lucide-react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { MOVIES } from '../data/mock-data';
import { MovieCard } from '../components/MovieCard';
import { RootStackParamList } from '../navigation/types';

export function RecommendationsScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const aiPicks = [MOVIES[0], MOVIES[3], MOVIES[4], MOVIES[2]];
  const similar = [MOVIES[5], MOVIES[7], MOVIES[6], MOVIES[8]];

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>
        <View style={styles.headerRow}>
          <View style={styles.headerLeft}>
            <Sparkles size={20} color={colors.primary} />
            <Text style={styles.headerTitle}>Para Voce</Text>
          </View>
          <TouchableOpacity style={styles.refreshBtn} activeOpacity={0.7}>
            <RefreshCw size={16} color={colors.mutedForeground} />
          </TouchableOpacity>
        </View>

        <View style={styles.matchBanner}>
          <Text style={styles.matchTitle}>Baseado no seu perfil</Text>
          <Text style={styles.matchDesc}>Usuarios com gostos parecidos adoraram estes filmes</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>IA Recomenda</Text>
          <View style={styles.grid}>
            {aiPicks.map((m) => (
              <MovieCard key={m.id} movie={m} onPress={() => navigation.navigate('MovieDetails', { id: m.id })} />
            ))}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Usuarios Parecidos Curtiram</Text>
          <View style={styles.grid}>
            {similar.map((m) => (
              <MovieCard key={m.id} movie={m} onPress={() => navigation.navigate('MovieDetails', { id: m.id })} />
            ))}
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  scroll: { paddingHorizontal: 24, paddingBottom: 100 },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 },
  headerLeft: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  headerTitle: { color: colors.white, fontSize: 24, fontWeight: '500' },
  refreshBtn: { padding: 8, backgroundColor: colors.secondary, borderRadius: 20 },
  matchBanner: { borderWidth: 1, borderColor: 'rgba(212,160,23,0.2)', borderRadius: 14, padding: 16, marginBottom: 24, backgroundColor: 'rgba(212,160,23,0.05)' },
  matchTitle: { color: colors.primary, fontSize: 14 },
  matchDesc: { color: colors.mutedForeground, fontSize: 12, marginTop: 4 },
  section: { marginBottom: 24 },
  sectionTitle: { color: colors.white, fontSize: 18, fontWeight: '500', marginBottom: 12 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 16 },
});
