import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

/**
 * Armazenamento de sessão com implementação por plataforma.
 *
 * O `expo-secure-store` é um módulo nativo: no iOS usa o Keychain e no Android o
 * EncryptedSharedPreferences. Na web ele simplesmente não existe, e chamá-lo
 * falha com "setValueWithKeyAsync is not a function" — o que quebrava o cadastro
 * e o login em `expo start --web`, alvo que o projeto declara suportar.
 *
 * Aviso de segurança: no navegador o token fica em localStorage, que **não é
 * criptografado** e é acessível a qualquer script rodando na página. É o que a
 * plataforma oferece sem um backend de sessão por cookie httpOnly. Em nativo, o
 * armazenamento continua sendo o seguro.
 */

const isWeb = Platform.OS === 'web';

function webStorage(): Storage | null {
  try {
    return typeof localStorage !== 'undefined' ? localStorage : null;
  } catch {
    // localStorage pode lançar quando cookies/armazenamento estão bloqueados.
    return null;
  }
}

export const storage = {
  async getItem(key: string): Promise<string | null> {
    if (isWeb) {
      return webStorage()?.getItem(key) ?? null;
    }
    return SecureStore.getItemAsync(key);
  },

  async setItem(key: string, value: string): Promise<void> {
    if (isWeb) {
      webStorage()?.setItem(key, value);
      return;
    }
    await SecureStore.setItemAsync(key, value);
  },

  async removeItem(key: string): Promise<void> {
    if (isWeb) {
      webStorage()?.removeItem(key);
      return;
    }
    await SecureStore.deleteItemAsync(key);
  },
};
