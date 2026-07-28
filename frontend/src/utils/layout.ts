import { useWindowDimensions } from 'react-native';

/**
 * Largura dos cards e número de colunas, calculados a partir da janela.
 *
 * O MovieCard fixava `(width - 64) / 2` lido de `Dimensions.get('window')` no
 * escopo do módulo. Duas consequências: o valor era congelado no primeiro
 * render, ignorando rotação e redimensionamento; e "duas colunas" só faz
 * sentido em celular — numa janela de 1920px cada card ficava com 928px de
 * largura e, pelo aspect ratio 2/3, quase 1400px de altura.
 *
 * Aqui o número de colunas cresce com o espaço disponível, respeitando uma
 * largura máxima de card. Pôster de filme não precisa ser maior que isso.
 */

/** Padding lateral das listas. Precisa acompanhar `listContent` nas telas. */
const HORIZONTAL_PADDING = 24;
const GAP = 16;
const MAX_CARD_WIDTH = 220;
const MIN_COLUMNS = 2;

export interface CardLayout {
  columns: number;
  cardWidth: number;
}

export function useCardLayout(): CardLayout {
  const { width } = useWindowDimensions();

  const available = Math.max(width - HORIZONTAL_PADDING * 2, MAX_CARD_WIDTH);
  const fitting = Math.floor((available + GAP) / (MAX_CARD_WIDTH + GAP));
  const columns = Math.max(MIN_COLUMNS, fitting);
  const cardWidth = Math.floor((available - GAP * (columns - 1)) / columns);

  return { columns, cardWidth };
}
