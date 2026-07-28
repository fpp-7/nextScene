package io.nextscene.backend.service;

import io.nextscene.backend.dto.MovieResponse;
import io.nextscene.backend.model.Movie;
import io.nextscene.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    public static final int MAX_PAGE_SIZE = 50;

    private final MovieRepository movieRepository;

    private static final Map<String, String> GENRE_TRANSLATION = Map.ofEntries(
        Map.entry("acao", "Action"),
        Map.entry("aventura", "Adventure"),
        Map.entry("animacao", "Animation"),
        Map.entry("comedia", "Comedy"),
        Map.entry("crime", "Crime"),
        Map.entry("documentario", "Documentary"),
        Map.entry("drama", "Drama"),
        Map.entry("fantasia", "Fantasy"),
        Map.entry("ficcao cientifica", "Sci-Fi"),
        Map.entry("guerra", "War"),
        Map.entry("horror", "Horror"),
        Map.entry("infantil", "Children"),
        Map.entry("misterio", "Mystery"),
        Map.entry("musical", "Musical"),
        Map.entry("noir", "Film-Noir"),
        Map.entry("romance", "Romance"),
        Map.entry("suspense", "Thriller"),
        Map.entry("terror", "Horror"),
        Map.entry("western", "Western")
    );

    /**
     * Traduz o gênero exibido no app para o vocabulário do catálogo MovieLens.
     * Gêneros sem tradução conhecida são repassados como vieram.
     */
    public static String translateGenre(String genre) {
        if (genre == null) return "";
        return GENRE_TRANSLATION.getOrDefault(genre.toLowerCase().trim(), genre);
    }

    /**
     * Critérios de ordenação do catálogo.
     * <p>
     * Antes existia só um, por nota — e a tela o rotulava como "Em Alta". Nota
     * média mede qualidade percebida, não alcance: um documentário com 60 votos
     * pode ter média 9,0 sem que ninguém esteja assistindo.
     */
    public enum SortBy {
        /** Mais avaliados no TMDB. É o sinal real de "em alta". */
        POPULAR,
        /** Mais recentes primeiro; entre os do mesmo ano, os mais vistos. */
        RECENT,
        /** Melhor nota média. */
        RATING;

        static SortBy from(String value) {
            if (value == null || value.isBlank()) return RATING;
            try {
                return valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Ordenação inválida: '" + value + "'. Use popular, recent ou rating.");
            }
        }

        Sort toSort() {
            return switch (this) {
                // NULLS LAST importa: filmes ainda não enriquecidos têm o campo
                // nulo e não podem encabeçar a lista.
                case POPULAR -> Sort.by(Sort.Order.desc("voteCount").nullsLast());
                case RECENT -> Sort.by(Sort.Order.desc("year").nullsLast(),
                                       Sort.Order.desc("voteCount").nullsLast());
                case RATING -> Sort.by(Sort.Order.desc("rating").nullsLast());
            };
        }
    }

    @Cacheable(value = "movies", key = "#genre + ':' + #sort + ':' + #page + ':' + #size")
    public List<MovieResponse> getMovies(String genre, String sort, int page, int size) {
        Pageable pageable = pageable(page, size, SortBy.from(sort));

        List<Movie> movies;
        if (genre != null && !genre.isBlank() && !genre.equalsIgnoreCase("Todos")) {
            movies = movieRepository.findByGenresContainingIgnoreCase(translateGenre(genre), pageable);
        } else {
            movies = movieRepository.findAll(pageable).getContent();
        }
        return movies.stream().map(MovieResponse::from).toList();
    }

    @Cacheable(value = "movieById", key = "#movieId")
    public MovieResponse getMovieById(Integer movieId) {
        return MovieResponse.from(findEntityByMovieId(movieId));
    }

    @Cacheable("featuredMovie")
    public MovieResponse getFeaturedMovie() {
        Movie movie = movieRepository.findTopByOrderByRatingDesc()
                .orElseThrow(() -> new IllegalArgumentException("Nenhum filme disponível."));
        return MovieResponse.from(movie);
    }

    public List<MovieResponse> searchMovies(String query, int page, int size) {
        if (query == null || query.isBlank()) return List.of();

        int limit  = clampSize(size);
        int offset = Math.max(page, 0) * limit;
        return movieRepository.searchByTitle(query.trim(), limit, offset)
                .stream().map(MovieResponse::from).toList();
    }

    public Movie findEntityByMovieId(Integer movieId) {
        return movieRepository.findByMovieId(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId));
    }

    private Pageable pageable(int page, int size, SortBy sortBy) {
        return PageRequest.of(Math.max(page, 0), clampSize(size), sortBy.toSort());
    }

    private int clampSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /** Sinaliza 404 em vez do 400 genérico que o IllegalArgumentException produzia. */
    public static class MovieNotFoundException extends RuntimeException {
        public MovieNotFoundException(Integer movieId) {
            super("Filme não encontrado: " + movieId);
        }
    }
}
