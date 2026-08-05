import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { MovieCard } from '../MovieCard';
import { Movie } from '../../types';

/**
 * `render` é assíncrono a partir da versão 14 da testing-library: ele devolve
 * uma Promise, e sem `await` as consultas simplesmente não existem.
 */
function movie(overrides: Partial<Movie> = {}): Movie {
  return {
    id: 318,
    title: 'Um Sonho de Liberdade',
    year: 1994,
    genre: 'Crime, Drama',
    rating: 8.726,
    imdb: 8.726,
    poster: 'https://image.tmdb.org/t/p/w500/poster.jpg',
    synopsis: 'Sinopse',
    cast: ['Tim Robbins'],
    ...overrides,
  };
}

describe('MovieCard', () => {
  it('mostra título e gêneros', async () => {
    const { getByText } = await render(<MovieCard movie={movie()} onPress={jest.fn()} />);

    expect(getByText('Um Sonho de Liberdade')).toBeTruthy();
    expect(getByText('Crime, Drama')).toBeTruthy();
  });

  it('arredonda a nota para uma casa decimal', async () => {
    const { getByText, queryByText } = await render(
      <MovieCard movie={movie({ rating: 8.726 })} onPress={jest.fn()} />
    );

    expect(getByText('8.7')).toBeTruthy();
    expect(queryByText('8.726')).toBeNull();
  });

  /**
   * Filme ainda não enriquecido via TMDB chega com nota 0, e "0.0" parece nota
   * péssima em vez de nota ausente.
   */
  it('esconde o selo de nota quando o filme não tem nota', async () => {
    const { queryByText, getByText } = await render(
      <MovieCard movie={movie({ rating: 0 })} onPress={jest.fn()} />
    );

    expect(queryByText('0.0')).toBeNull();
    expect(getByText('Um Sonho de Liberdade')).toBeTruthy();
  });

  it('chama onPress com o id do filme', async () => {
    const onPress = jest.fn();
    const { getByLabelText } = await render(<MovieCard movie={movie({ id: 858 })} onPress={onPress} />);

    fireEvent.press(getByLabelText(/Abrir detalhes/));

    expect(onPress).toHaveBeenCalledWith(858);
  });

  it('anuncia o filme para leitores de tela', async () => {
    const { getByLabelText } = await render(<MovieCard movie={movie()} onPress={jest.fn()} />);

    expect(getByLabelText('Um Sonho de Liberdade. Abrir detalhes')).toBeTruthy();
  });
});
