import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { User, Settings, Star, Film, Heart, LogOut, ChevronRight } from 'lucide-react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { RootStackParamList } from '../navigation/types';

export function ProfileScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();

  const stats = [
    { label: 'Avaliados', value: 47, Icon: Star },
    { label: 'Assistidos', value: 62, Icon: Film },
    { label: 'Favoritos', value: 15, Icon: Heart },
  ];

  const menuItems = [
    { label: 'Editar Perfil', Icon: User },
    { label: 'Preferencias de Genero', Icon: Settings },
    { label: 'Configuracoes', Icon: Settings },
  ];

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>
        {/* Avatar */}
        <View style={styles.avatarSection}>
          <View style={styles.avatar}>
            <User size={32} color={colors.primary} />
          </View>
          <Text style={styles.userName}>Usuario NextScene</Text>
          <Text style={styles.userEmail}>usuario@email.com</Text>
        </View>

        {/* Stats */}
        <View style={styles.statsRow}>
          {stats.map((s) => (
            <View key={s.label} style={styles.statCard}>
              <s.Icon size={20} color={colors.primary} />
              <Text style={styles.statValue}>{s.value}</Text>
              <Text style={styles.statLabel}>{s.label}</Text>
            </View>
          ))}
        </View>

        {/* Menu */}
        <View style={styles.menu}>
          {menuItems.map((item) => (
            <TouchableOpacity key={item.label} style={styles.menuItem} activeOpacity={0.7}>
              <item.Icon size={20} color={colors.mutedForeground} />
              <Text style={styles.menuText}>{item.label}</Text>
              <ChevronRight size={16} color={colors.mutedForeground} />
            </TouchableOpacity>
          ))}
          <TouchableOpacity style={[styles.menuItem, { marginTop: 16 }]} onPress={() => navigation.navigate('Login')} activeOpacity={0.7}>
            <LogOut size={20} color={colors.red400} />
            <Text style={[styles.menuText, { color: colors.red400 }]}>Sair</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  scroll: { paddingHorizontal: 24, paddingBottom: 100 },
  avatarSection: { alignItems: 'center', gap: 8, marginBottom: 32 },
  avatar: { width: 80, height: 80, borderRadius: 40, backgroundColor: 'rgba(212,160,23,0.2)', borderWidth: 2, borderColor: colors.primary, justifyContent: 'center', alignItems: 'center' },
  userName: { color: colors.white, fontSize: 20, fontWeight: '500' },
  userEmail: { color: colors.mutedForeground, fontSize: 14 },
  statsRow: { flexDirection: 'row', gap: 12, marginBottom: 32 },
  statCard: { flex: 1, backgroundColor: colors.secondary, borderRadius: 14, padding: 16, alignItems: 'center', gap: 4 },
  statValue: { color: colors.white, fontSize: 18, fontWeight: '500' },
  statLabel: { color: colors.mutedForeground, fontSize: 12 },
  menu: { gap: 8 },
  menuItem: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.secondary, borderRadius: 14, paddingHorizontal: 16, paddingVertical: 16 },
  menuText: { flex: 1, color: colors.white, fontSize: 14 },
});
