import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { MovieShelf } from '../MovieShelf';
import { Movie } from '../../types';

function movie(id: number, title: string): Movie {
  return {
    id,
    title,
    year: 2000,
    genre: 'Drama',
    rating: 8,
    imdb: 8,
    poster: 'https://image.tmdb.org/t/p/w500/p.jpg',
    synopsis: '',
    cast: [],
  };
}

const filmes = [movie(1, 'Matrix'), movie(2, 'Se7en')];

describe('MovieShelf', () => {
  it('mostra título, subtítulo e os filmes', async () => {
    const { getByText } = await render(
      <MovieShelf
        title="Em Alta"
        subtitle="Os mais avaliados pelo público"
        movies={filmes}
        isLoading={false}
        onPressMovie={jest.fn()}
      />
    );

    expect(getByText('Em Alta')).toBeTruthy();
    expect(getByText('Os mais avaliados pelo público')).toBeTruthy();
    expect(getByText('Matrix')).toBeTruthy();
  });

  /**
   * Prateleira vazia viraria um título solto seguido de espaço morto. Some por
   * inteiro em vez de anunciar uma seção sem conteúdo.
   */
  it('não renderiza nada quando não há filmes', async () => {
    const { queryByText } = await render(
      <MovieShelf
        title="Em Alta"
        subtitle="Os mais avaliados"
        movies={[]}
        isLoading={false}
        onPressMovie={jest.fn()}
      />
    );

    expect(queryByText('Em Alta')).toBeNull();
  });

  it('mostra o título enquanto carrega, mesmo sem filmes ainda', async () => {
    const { getByText } = await render(
      <MovieShelf
        title="Mais Recentes"
        subtitle="Os últimos títulos"
        movies={[]}
        isLoading
        onPressMovie={jest.fn()}
      />
    );

    expect(getByText('Mais Recentes')).toBeTruthy();
  });

  it('propaga o toque no filme', async () => {
    const onPressMovie = jest.fn();
    const { getByLabelText } = await render(
      <MovieShelf
        title="Em Alta"
        subtitle="Os mais avaliados"
        movies={filmes}
        isLoading={false}
        onPressMovie={onPressMovie}
      />
    );

    fireEvent.press(getByLabelText('Se7en. Abrir detalhes'));

    expect(onPressMovie).toHaveBeenCalledWith(2);
  });
});
