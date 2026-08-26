import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, KeyboardAvoidingView, Platform, Alert } from 'react-native';
import { messageFor } from '../services/errors';
import { ArrowLeft, User, Mail } from 'lucide-react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../theme/colors';
import { RootStackParamList } from '../navigation/types';
import { useAuth } from '../contexts/AuthContext';
import { userService } from '../services/userService';
import { validateName, validateEmail } from '../utils/validation';
import { LoadingSpinner } from '../components/LoadingSpinner';

type Props = NativeStackScreenProps<RootStackParamList, 'EditProfile'>;

export function EditProfileScreen({ navigation }: Props) {
  const { user, updateUser, logout } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [nameError, setNameError] = useState<string | null>(null);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isSaved, setIsSaved] = useState(false);

  useEffect(() => {
    if (user) {
      setName(user.name);
      setEmail(user.email);
    }
  }, [user]);

  const handleSave = async () => {
    const nErr = validateName(name);
    const eErr = validateEmail(email);
    setNameError(nErr);
    setEmailError(eErr);

    if (nErr || eErr) return;

    // Precisa ser lido antes da chamada: `user` é atualizado logo em seguida.
    const emailChanged = email.trim().toLowerCase() !== (user?.email ?? '').toLowerCase();

    setIsSaving(true);
    try {
      const updatedUser = await userService.updateProfile({ name, email });

      // O backend revoga os refresh tokens quando o e-mail muda — o token de
      // acesso atual ainda vale por até 30 minutos, mas a renovação já não. Sem
      // este ramo, o app seguia normalmente e derrubava o usuário no login meia
      // hora depois, sem relação visível com o que ele fez. Encerrar aqui, com
      // o motivo na tela, é honesto: o efeito fica junto da causa.
      if (emailChanged) {
        Alert.alert(
          'E-mail alterado',
          'Por segurança, sua sessão foi encerrada. Entre novamente com o novo e-mail.',
          [{ text: 'Entendi', onPress: () => { logout(); } }],
          { cancelable: false }
        );
        return;
      }

      updateUser(updatedUser);
      setIsSaved(true);
      setTimeout(() => setIsSaved(false), 2000);
    } catch (err: any) {
      const errorMsg = messageFor(err, 'Erro ao atualizar perfil');
      Alert.alert('Erro', errorMsg);
    } finally {
      setIsSaving(false);
    }
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
        <Text style={styles.title}>Editar Perfil</Text>
      </View>
      <KeyboardAvoidingView style={styles.content} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.form}>
          <View style={styles.fieldGroup}>
            <Text style={styles.label}>Nome</Text>
            <View style={[styles.inputContainer, nameError ? styles.inputError : null]}>
              <User size={20} color={colors.mutedForeground} />
              <TextInput
                style={styles.input}
                value={name}
                onChangeText={setName}
                placeholderTextColor={colors.mutedForeground}
              />
            </View>
            {nameError && <Text style={styles.errorText}>{nameError}</Text>}
          </View>

          <View style={styles.fieldGroup}>
            <Text style={styles.label}>Email</Text>
            <View style={[styles.inputContainer, emailError ? styles.inputError : null]}>
              <Mail size={20} color={colors.mutedForeground} />
              <TextInput
                style={styles.input}
                value={email}
                onChangeText={setEmail}
                keyboardType="email-address"
                autoCapitalize="none"
                placeholderTextColor={colors.mutedForeground}
              />
            </View>
            {emailError && <Text style={styles.errorText}>{emailError}</Text>}
          </View>
        </View>

        <TouchableOpacity 
          style={styles.saveBtn} 
          onPress={handleSave} 
          disabled={isSaving}
          activeOpacity={0.8}
        >
          {isSaving ? (
            <LoadingSpinner size="small" />
          ) : (
            <Text style={styles.saveBtnText}>{isSaved ? 'Salvo!' : 'Salvar Alterações'}</Text>
          )}
        </TouchableOpacity>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 24, paddingVertical: 16, gap: 16 },
  backBtn: { padding: 4 },
  title: { color: colors.white, fontSize: 20, fontWeight: '500' },
  content: { flex: 1, paddingHorizontal: 24, justifyContent: 'space-between', paddingBottom: 24 },
  form: { marginTop: 24, gap: 16 },
  fieldGroup: { gap: 6 },
  label: { color: 'rgba(255,255,255,0.8)', fontSize: 14, fontWeight: '500' },
  inputContainer: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.inputBackground, borderRadius: 14, paddingHorizontal: 16, paddingVertical: 14 },
  input: { flex: 1, color: colors.white, fontSize: 16 },
  inputError: { borderWidth: 1, borderColor: colors.red400 },
  errorText: { color: colors.red400, fontSize: 12 },
  saveBtn: { backgroundColor: colors.primary, paddingVertical: 16, borderRadius: 14, alignItems: 'center', marginTop: 24 },
  saveBtnText: { color: colors.primaryForeground, fontSize: 16, fontWeight: '500' },
});
