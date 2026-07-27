/**
 * Gêneros oferecidos na interface.
 *
 * Cada item precisa ter tradução em `MovieService.GENRE_TRANSLATION` no backend,
 * senão o filtro devolve lista vazia. Foi o que acontecia com "Historia": o chip
 * existia aqui, mas o catálogo MovieLens não tem o gênero History, então clicar
 * nele sempre resultava em "Nenhum filme encontrado".
 *
 * Sem acento de propósito — o mapa de tradução do backend usa chaves sem acento.
 */
export const GENRES = [
  'Acao',
  'Aventura',
  'Animacao',
  'Comedia',
  'Crime',
  'Documentario',
  'Drama',
  'Fantasia',
  'Ficcao Cientifica',
  'Guerra',
  'Infantil',
  'Misterio',
  'Musical',
  'Noir',
  'Romance',
  'Suspense',
  'Terror',
  'Western',
];
