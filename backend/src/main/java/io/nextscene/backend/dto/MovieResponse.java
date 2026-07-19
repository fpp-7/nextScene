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
                movie.getTitle(),
                movie.getYear() != null ? movie.getYear() : 0,
                movie.getGenres() != null ? movie.getGenres() : "",
                movie.getRating() != null ? movie.getRating() : 0.0,
                movie.getRating() != null ? movie.getRating() : 0.0,
                movie.getPosterUrl() != null ? movie.getPosterUrl() : "",
                movie.getSynopsis() != null ? movie.getSynopsis() : "",
                castList
        );
    }
}
