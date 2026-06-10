import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { ArrowLeft, Info, Trash2, Moon } from 'lucide-react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Settings'>;

export function SettingsScreen({ navigation }: Props) {
  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <ArrowLeft size={24} color={colors.white} />
        </TouchableOpacity>
        <Text style={styles.title}>Configurações</Text>
      </View>
      <ScrollView style={styles.content}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Aplicativo</Text>
          <TouchableOpacity style={styles.item} activeOpacity={0.7}>
            <Moon size={20} color={colors.mutedForeground} />
            <Text style={styles.itemText}>Tema Escuro</Text>
            <Text style={styles.itemValue}>Ativado</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.item} activeOpacity={0.7}>
            <Trash2 size={20} color={colors.red400} />
            <Text style={[styles.itemText, { color: colors.red400 }]}>Limpar Cache</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Sobre</Text>
          <TouchableOpacity style={styles.item} activeOpacity={0.7}>
            <Info size={20} color={colors.mutedForeground} />
            <Text style={styles.itemText}>Sobre o NextScene</Text>
          </TouchableOpacity>
          <View style={styles.item}>
            <Text style={styles.itemText}>Versão</Text>
            <Text style={styles.itemValue}>1.0.0</Text>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 24, paddingVertical: 16, gap: 16 },
  backBtn: { padding: 4 },
  title: { color: colors.white, fontSize: 20, fontWeight: '500' },
  content: { flex: 1, paddingHorizontal: 24 },
  section: { marginTop: 24 },
  sectionTitle: { color: colors.primary, fontSize: 14, fontWeight: '500', marginBottom: 12, textTransform: 'uppercase', letterSpacing: 1 },
  item: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.secondary, padding: 16, borderRadius: 14, marginBottom: 8, gap: 12 },
  itemText: { flex: 1, color: colors.white, fontSize: 16 },
  itemValue: { color: colors.mutedForeground, fontSize: 14 },
});
