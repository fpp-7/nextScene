package io.nextscene.backend.service;

import io.nextscene.backend.dto.MovieResponse;
import io.nextscene.backend.model.Movie;
import io.nextscene.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @Value("${app.tmdb.api-key:}")
    private String tmdbApiKey;

    @Value("${app.tmdb.base-url:https://api.themoviedb.org/3}")
    private String tmdbBaseUrl;

    private static final java.util.Map<String, String> GENRE_TRANSLATION = java.util.Map.ofEntries(
        java.util.Map.entry("acao", "Action"),
        java.util.Map.entry("aventura", "Adventure"),
        java.util.Map.entry("animacao", "Animation"),
        java.util.Map.entry("comedia", "Comedy"),
        java.util.Map.entry("crime", "Crime"),
        java.util.Map.entry("documentario", "Documentary"),
        java.util.Map.entry("drama", "Drama"),
        java.util.Map.entry("fantasia", "Fantasy"),
        java.util.Map.entry("ficcao cientifica", "Sci-Fi"),
        java.util.Map.entry("guerra", "War"),
        java.util.Map.entry("historia", "History"),
        java.util.Map.entry("horror", "Horror"),
        java.util.Map.entry("misterio", "Mystery"),
        java.util.Map.entry("musical", "Musical"),
        java.util.Map.entry("romance", "Romance"),
        java.util.Map.entry("suspense", "Thriller"),
        java.util.Map.entry("terror", "Horror"),
        java.util.Map.entry("western", "Western")
    );

    public List<MovieResponse> getMovies(String genre) {
        List<Movie> movies;
        var pageable = org.springframework.data.domain.PageRequest.of(0, 30);
        if (genre != null && !genre.isBlank() && !genre.equalsIgnoreCase("Todos")) {
            String translatedGenre = GENRE_TRANSLATION.getOrDefault(genre.toLowerCase().trim(), genre);
            movies = movieRepository.findByGenresContainingIgnoreCase(translatedGenre, pageable);
        } else {
            movies = movieRepository.findAll(pageable).getContent();
        }
        return movies.stream().map(MovieResponse::from).toList();
    }

    public MovieResponse getMovieById(Integer movieId) {
        Movie movie = movieRepository.findByMovieId(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Filme não encontrado."));
        enrichMovieFromTmdb(movie);
        return MovieResponse.from(movie);
    }

    public MovieResponse getFeaturedMovie() {
        Movie movie = movieRepository.findTopByOrderByRatingDesc()
                .orElseThrow(() -> new IllegalArgumentException("Nenhum filme disponível."));
        enrichMovieFromTmdb(movie);
        return MovieResponse.from(movie);
    }

    public List<MovieResponse> searchMovies(String query) {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 30);
        return movieRepository.findByTitleContainingIgnoreCase(query, pageable)
                .stream().map(MovieResponse::from).toList();
    }

    public Movie findEntityByMovieId(Integer movieId) {
        Movie movie = movieRepository.findByMovieId(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Filme não encontrado: " + movieId));
        enrichMovieFromTmdb(movie);
        return movie;
    }

    @SuppressWarnings("unchecked")
    public void enrichMovieFromTmdb(Movie movie) {
        if (movie.getTmdbId() == null) return;
        if (tmdbApiKey == null || tmdbApiKey.isBlank()) return;
        // Only enrich if synopsis/overview is empty/null or if poster is default placeholder
        if (movie.getSynopsis() != null && !movie.getSynopsis().isBlank() && 
            movie.getPosterUrl() != null && !movie.getPosterUrl().contains("unsplash") &&
            movie.getPosterUrl() != null && !movie.getPosterUrl().contains("placehold")) {
            return;
        }

        try {
            String url = String.format("%s/movie/%d?append_to_response=credits&api_key=%s&language=pt-BR", 
                    tmdbBaseUrl, movie.getTmdbId(), tmdbApiKey);
            
            var response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                String posterPath = (String) response.get("poster_path");
                if (posterPath != null && !posterPath.isBlank()) {
                    movie.setPosterUrl("https://image.tmdb.org/t/p/w500" + posterPath);
                }
                
                String overview = (String) response.get("overview");
                if (overview != null && !overview.isBlank()) {
                    movie.setSynopsis(overview);
                }
                
                Number voteAverage = (Number) response.get("vote_average");
                if (voteAverage != null) {
                    movie.setRating(voteAverage.doubleValue());
                }

                // Extract cast members
                Map<String, Object> credits = (Map<String, Object>) response.get("credits");
                if (credits != null) {
                    List<Map<String, Object>> castList = (List<Map<String, Object>>) credits.get("cast");
                    if (castList != null) {
                        String castString = castList.stream()
                                .limit(5)
                                .map(c -> (String) c.get("name"))
                                .reduce((a, b) -> a + "," + b)
                                .orElse("");
                        movie.setCastList(castString);
                    }
                }
                movieRepository.save(movie);
                log.info("🎬 Filme enriquecido via TMDB: {}", movie.getTitle());
            }
        } catch (Exception e) {
            log.warn("⚠️ Falha ao enriquecer filme '{}' (TMDB ID: {}) via TMDB: {}", movie.getTitle(), movie.getTmdbId(), e.getMessage());
        }
    }
}
