import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Clapperboard, Mail, Lock, User } from 'lucide-react-native';
import { colors } from '../theme/colors';
import { RootStackParamList } from '../navigation/types';
import { useAuth } from '../contexts/AuthContext';
import { validateName, validateEmail, validatePassword } from '../utils/validation';
import { LoadingSpinner } from '../components/LoadingSpinner';

type Props = NativeStackScreenProps<RootStackParamList, 'Register'>;

export function RegisterScreen({ navigation }: Props) {
  const { register } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nameError, setNameError] = useState<string | null>(null);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleRegister = async () => {
    const nErr = validateName(name);
    const eErr = validateEmail(email);
    const pErr = validatePassword(password);
    
    setNameError(nErr);
    setEmailError(eErr);
    setPasswordError(pErr);
    setGeneralError(null);

    if (nErr || eErr || pErr) return;

    setIsSubmitting(true);
    try {
      await register(name, email, password);
      // AuthContext will handle navigation
    } catch (err: any) {
      setGeneralError(err.message || 'Erro ao criar conta. Tente novamente.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.inner}>
        <View style={styles.logoContainer}>
          <View style={styles.logoIcon}>
            <Clapperboard size={28} color={colors.primaryForeground} />
          </View>
          <Text style={styles.title}>Criar Conta</Text>
          <Text style={styles.subtitle}>Junte-se ao NextScene</Text>
        </View>

        <View style={styles.form}>
          {generalError && (
            <View style={styles.generalErrorBox}>
              <Text style={styles.generalErrorText}>{generalError}</Text>
            </View>
          )}

          <View style={styles.fieldGroup}>
            <Text style={styles.label}>Nome</Text>
            <View style={[styles.inputContainer, nameError ? styles.inputError : null]}>
              <User size={20} color={colors.mutedForeground} />
              <TextInput
                style={styles.input}
                placeholder="Seu nome"
                placeholderTextColor={colors.mutedForeground}
                value={name}
                onChangeText={setName}
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
                placeholder="seu@email.com"
                placeholderTextColor={colors.mutedForeground}
                value={email}
                onChangeText={setEmail}
                keyboardType="email-address"
                autoCapitalize="none"
              />
            </View>
            {emailError && <Text style={styles.errorText}>{emailError}</Text>}
          </View>

          <View style={styles.fieldGroup}>
            <Text style={styles.label}>Senha</Text>
            <View style={[styles.inputContainer, passwordError ? styles.inputError : null]}>
              <Lock size={20} color={colors.mutedForeground} />
              <TextInput
                style={styles.input}
                placeholder="********"
                placeholderTextColor={colors.mutedForeground}
                value={password}
                onChangeText={setPassword}
                secureTextEntry
              />
            </View>
            {passwordError && <Text style={styles.errorText}>{passwordError}</Text>}
          </View>

          <TouchableOpacity
            style={styles.button}
            onPress={handleRegister}
            disabled={isSubmitting}
            activeOpacity={0.8}
          >
            {isSubmitting ? (
              <LoadingSpinner size="small" />
            ) : (
              <Text style={styles.buttonText}>Cadastrar</Text>
            )}
          </TouchableOpacity>

          <View style={styles.registerRow}>
            <Text style={styles.registerText}>Já tem uma conta? </Text>
            <TouchableOpacity onPress={() => navigation.navigate('Login')}>
              <Text style={styles.registerLink}>Entrar</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  inner: { flex: 1, justifyContent: 'center', alignItems: 'center', paddingHorizontal: 24 },
  logoContainer: { alignItems: 'center', gap: 12, marginBottom: 32 },
  logoIcon: { width: 56, height: 56, backgroundColor: colors.primary, borderRadius: 14, justifyContent: 'center', alignItems: 'center' },
  title: { color: colors.white, fontSize: 24, fontWeight: '500' },
  subtitle: { color: colors.mutedForeground, fontSize: 14 },
  form: { width: '100%', gap: 16 },
  fieldGroup: { gap: 6 },
  label: { color: 'rgba(255,255,255,0.8)', fontSize: 14, fontWeight: '500' },
  inputContainer: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.inputBackground, borderRadius: 14, paddingHorizontal: 16, paddingVertical: 14 },
  input: { flex: 1, color: colors.white, fontSize: 16 },
  inputError: { borderWidth: 1, borderColor: colors.red400 },
  errorText: { color: colors.red400, fontSize: 12 },
  generalErrorBox: { backgroundColor: 'rgba(239,68,68,0.1)', padding: 12, borderRadius: 8, borderWidth: 1, borderColor: 'rgba(239,68,68,0.3)' },
  generalErrorText: { color: colors.red400, fontSize: 14, textAlign: 'center' },
  button: { backgroundColor: colors.primary, paddingVertical: 16, borderRadius: 14, alignItems: 'center', marginTop: 8 },
  buttonText: { color: colors.primaryForeground, fontSize: 16, fontWeight: '500' },
  registerRow: { flexDirection: 'row', justifyContent: 'center', marginTop: 8 },
  registerText: { color: colors.mutedForeground, fontSize: 14 },
  registerLink: { color: colors.primary, fontSize: 14, textDecorationLine: 'underline' },
});
