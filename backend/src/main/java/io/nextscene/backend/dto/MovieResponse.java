package io.nextscene.backend.dto;

import io.nextscene.backend.model.Movie;

import java.util.Arrays;
import java.util.List;

public record MovieResponse(
        int id,
        String title,
        int year,
        String genre,
        double rating,
        double imdb,
        String poster,
        String synopsis,
        List<String> cast
) {
    public static MovieResponse from(Movie movie) {
        List<String> castList = movie.getCastList() != null
                ? Arrays.asList(movie.getCastList().split(","))
                : List.of();

        return new MovieResponse(
                movie.getMovieId() != null ? movie.getMovieId() : 0,
                displayTitle(movie),
                movie.getYear() != null ? movie.getYear() : 0,
                movie.getGenres() != null ? movie.getGenres() : "",
                movie.getRating() != null ? movie.getRating() : 0.0,
                movie.getRating() != null ? movie.getRating() : 0.0,
                movie.getPosterUrl() != null ? movie.getPosterUrl() : "",
                movie.getSynopsis() != null ? movie.getSynopsis() : "",
                castList
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
}
