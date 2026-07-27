/**
 * Cobre os módulos que não dependem de renderização (serviços e cliente HTTP).
 *
 * Testes de componente ficaram de fora por ora: o react-test-renderer ainda não
 * resolve com React 19 neste projeto, e forçar a instalação quebraria o app.
 */
module.exports = {
  preset: 'jest-expo',
  testEnvironment: 'node',
  testMatch: ['**/__tests__/**/*.test.ts'],
  collectCoverageFrom: ['src/services/**/*.ts'],
};
