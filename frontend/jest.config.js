/**
 * Dois ambientes de teste no mesmo projeto.
 *
 * Serviços e utilitários não tocam em React e rodam mais rápido no ambiente
 * node. Componentes precisam do preset do Expo, que instala os mocks dos
 * módulos nativos e o renderer.
 */
module.exports = {
  projects: [
    {
      displayName: 'unit',
      preset: 'jest-expo',
      testEnvironment: 'node',
      testMatch: ['**/src/services/__tests__/**/*.test.ts', '**/src/utils/__tests__/**/*.test.ts'],
    },
    {
      displayName: 'components',
      preset: 'jest-expo',
      testMatch: ['**/src/components/__tests__/**/*.test.tsx', '**/src/screens/__tests__/**/*.test.tsx'],
      setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
      // Bibliotecas do ecossistema Expo/RN são publicadas em ESM e precisam
      // passar pelo Babel.
      transformIgnorePatterns: [
        'node_modules/(?!(?:jest-)?react-native|@react-native|expo(nent)?|@expo(nent)?/.*|react-navigation|@react-navigation/.*|lucide-react-native|react-native-svg|react-native-safe-area-context)',
      ],
    },
  ],
  collectCoverageFrom: ['src/**/*.{ts,tsx}', '!src/**/__tests__/**'],
};
