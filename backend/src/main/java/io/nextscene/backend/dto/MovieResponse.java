package io.nextscene.backend.dto;

import io.nextscene.backend.model.Movie;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;

public record MovieResponse(
        int id,
        String title,
        int year,
        String genre,
        double rating,
        @Schema(description = "Sempre igual a `rating`. Nenhuma tela do app lê este "
                + "campo — mantido pelo DTO, não pelo contrato real.")
        double imdb,
        String poster,
        String synopsis,
        List<String> cast,
        @Schema(description = "Chave do vídeo no YouTube. Ausente quando não há trailer conhecido.",
                nullable = true)
        String trailerKey
) {
    /**
     * O MovieLens não deixa o campo de gêneros vazio: ele grava esta sentinela.
     * Sem tratá-la, a string ia crua para a tela e virava um chip escrito
     * "(no genres listed)" ao lado de "Comédia" e "Drama" — denunciando a
     * origem dos dados num lugar onde o usuário espera um gênero.
     */
    private static final String NO_GENRES_SENTINEL = "(no genres listed)";

    public static MovieResponse from(Movie movie) {
        // Sem o filtro de vazios, um cast_list em branco virava `[""]` — uma
        // lista de um elemento, que a tela renderizava como um avatar sem nome.
        List<String> castList = movie.getCastList() == null
                ? List.of()
                : Arrays.stream(movie.getCastList().split(","))
                        .map(String::trim)
                        .filter(name -> !name.isEmpty())
                        .toList();

        return new MovieResponse(
                movie.getMovieId() != null ? movie.getMovieId() : 0,
                displayTitle(movie),
                movie.getYear() != null ? movie.getYear() : 0,
                genresOf(movie),
                movie.getRating() != null ? movie.getRating() : 0.0,
                movie.getRating() != null ? movie.getRating() : 0.0,
                movie.getPosterUrl() != null ? movie.getPosterUrl() : "",
                movie.getSynopsis() != null ? movie.getSynopsis() : "",
                castList,
                movie.getTrailerKey()
        );
    }

    /**
     * Título exibido no aplicativo: o traduzido quando existe, senão o original.
     * <p>
     * O TMDB devolve o título original quando não há tradução para pt-BR, então
     * a coluna pode conter o mesmo texto — o fallback cobre os filmes que ainda
     * não passaram pelo job de enriquecimento.
     */
    private static String displayTitle(Movie movie) {
        String localized = movie.getTitlePt();
        return (localized != null && !localized.isBlank()) ? localized : movie.getTitle();
    }

    /**
     * Lista vazia em vez da sentinela: o app já não desenha chip nenhum quando
     * a string vem em branco, então não foi preciso mexer na tela.
     */
    private static String genresOf(Movie movie) {
        String genres = movie.getGenres();
        if (genres == null || genres.isBlank() || NO_GENRES_SENTINEL.equalsIgnoreCase(genres.trim())) {
            return "";
        }
        return genres;
    }
}
