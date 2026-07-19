import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, Alert, Switch } from 'react-native';
import { ArrowLeft, Info, Trash2, Moon } from 'lucide-react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as SecureStore from 'expo-secure-store';
import { colors } from '../theme/colors';
import { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Settings'>;

export function SettingsScreen({ navigation }: Props) {
  const [darkMode, setDarkMode] = useState(true); // App is dark-mode only for now

  const handleClearCache = () => {
    Alert.alert(
      'Limpar Cache',
      'Tem certeza que deseja limpar o cache do aplicativo? Você precisará fazer login novamente.',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Limpar',
          style: 'destructive',
          onPress: async () => {
            try {
              await SecureStore.deleteItemAsync('nextscene_auth_token');
              await SecureStore.deleteItemAsync('nextscene_user_data');
              await SecureStore.deleteItemAsync('nextscene_onboarding_complete');
              Alert.alert('Sucesso', 'Cache limpo com sucesso. Reinicie o aplicativo.');
            } catch {
              Alert.alert('Erro', 'Não foi possível limpar o cache.');
            }
          },
        },
      ]
    );
  };

  const handleAbout = () => {
    Alert.alert(
      'Sobre o NextScene',
      'NextScene v1.0.0\n\nSistema inteligente de recomendação de filmes com IA.\n\nDesenvolvido como projeto acadêmico.\n\n© 2026 NextScene',
      [{ text: 'OK' }]
    );
  };

  const handleToggleDarkMode = () => {
    if (darkMode) {
      Alert.alert('Tema Escuro', 'O tema claro ainda não está disponível. O aplicativo utiliza tema escuro por padrão.');
    }
  };

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
          <TouchableOpacity style={styles.item} activeOpacity={0.7} onPress={handleToggleDarkMode}>
            <Moon size={20} color={colors.mutedForeground} />
            <Text style={styles.itemText}>Tema Escuro</Text>
            <Switch
              value={darkMode}
              onValueChange={handleToggleDarkMode}
              trackColor={{ false: colors.secondary, true: colors.primary }}
              thumbColor={colors.white}
            />
          </TouchableOpacity>
          <TouchableOpacity style={styles.item} activeOpacity={0.7} onPress={handleClearCache}>
            <Trash2 size={20} color={colors.red400} />
            <Text style={[styles.itemText, { color: colors.red400 }]}>Limpar Cache</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Sobre</Text>
          <TouchableOpacity style={styles.item} activeOpacity={0.7} onPress={handleAbout}>
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
