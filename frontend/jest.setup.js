/**
 * Setup dos testes de componente.
 *
 * O `expo-secure-store` é um módulo nativo sem implementação em ambiente de
 * teste. O app já acessa storage por `services/storage.ts`, mas o mock aqui
 * cobre qualquer import indireto.
 */
jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(async () => null),
  setItemAsync: jest.fn(async () => undefined),
  deleteItemAsync: jest.fn(async () => undefined),
}));

// Silencia o aviso de "shadow*" depreciado do react-native-web, que polui a
// saída sem indicar problema real no código testado.
const originalWarn = console.warn;
console.warn = (...args) => {
  if (typeof args[0] === 'string' && args[0].includes('"shadow*" style props')) return;
  originalWarn(...args);
};
