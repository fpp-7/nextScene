import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, Alert } from 'react-native';
import { ArrowLeft, Info, Trash2, Moon } from 'lucide-react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { RootStackParamList } from '../navigation/types';
import { useAuth } from '../contexts/AuthContext';

type Props = NativeStackScreenProps<RootStackParamList, 'Settings'>;

export function SettingsScreen({ navigation }: Props) {
  const { logout } = useAuth();

  /**
   * Antes isto se chamava "Limpar Cache": apagava as chaves do SecureStore por
   * fora do AuthContext e pedia para "reiniciar o aplicativo" — a tela seguia
   * aberta e o estado em memória ainda dizia que havia sessão ativa. É um
   * logout, então agora tem o nome certo e passa pelo fluxo correto.
   */
  const handleSignOut = () => {
    Alert.alert(
      'Encerrar sessão',
      'Seus dados de login serão apagados deste aparelho e você voltará para a tela de entrada.',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Encerrar',
          style: 'destructive',
          onPress: async () => {
            try {
              await logout();
            } catch {
              Alert.alert('Erro', 'Não foi possível encerrar a sessão.');
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


  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          style={styles.backBtn}
          accessibilityRole="button"
          accessibilityLabel="Voltar"
        >
          <ArrowLeft size={24} color={colors.white} />
        </TouchableOpacity>
        <Text style={styles.title}>Configurações</Text>
      </View>
      <ScrollView style={styles.content}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Aplicativo</Text>
          {/* O toggle de tema foi removido: era um Switch que só abria um alerta
              dizendo que não fazia nada. Um controle que não controla nada é
              pior que a ausência dele. */}
          <View style={styles.item}>
            <Moon size={20} color={colors.mutedForeground} />
            <Text style={styles.itemText}>Tema</Text>
            <Text style={styles.itemValue}>Escuro</Text>
          </View>
          <TouchableOpacity
            style={styles.item}
            activeOpacity={0.7}
            onPress={handleSignOut}
            accessibilityRole="button"
            accessibilityLabel="Encerrar sessão neste aparelho"
          >
            <Trash2 size={20} color={colors.red400} />
            <Text style={[styles.itemText, { color: colors.red400 }]}>Encerrar sessão</Text>
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
