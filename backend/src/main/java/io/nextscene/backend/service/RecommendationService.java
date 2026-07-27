package io.nextscene.backend.service;

import io.nextscene.backend.client.RecommendationEngineClient;
import io.nextscene.backend.dto.ColdStartRequest;
import io.nextscene.backend.dto.MovieResponse;
import io.nextscene.backend.dto.RecommendationResponse;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.Movie;
import io.nextscene.backend.model.Rating;
import io.nextscene.backend.repository.MovieRepository;
import io.nextscene.backend.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int RECOMMENDATION_COUNT = 20;

    private final RecommendationEngineClient engineClient;
    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final UserService userService;

    /**
     * Recomendações personalizadas para um usuário do aplicativo.
     * <p>
     * O histórico de avaliações é lido do Postgres e enviado ao motor a cada
     * chamada — o motor é stateless e não precisa conhecer o usuário. Isso
     * substitui o esquema anterior, que enviava {@code interactionCount} como se
     * fosse o id de um usuário do MovieLens: todos os usuários com o mesmo
     * contador recebiam exatamente as mesmas sugestões.
     */
    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(UUID userId) {
        AppUser user = userService.findById(userId);
        List<Rating> history = ratingRepository.findByUser(user);

        if (history.isEmpty()) {
            log.debug("Usuário {} ainda não avaliou nada — usando preferências de gênero.", userId);
            return buildResponse(recommendByGenrePreference(user), user);
        }

        try {
            List<Map<String, Object>> payload = history.stream()
                    .filter(r -> r.getMovie() != null && r.getMovie().getMovieId() != null)
                    .<Map<String, Object>>map(r -> Map.of(
                            "movie_id", r.getMovie().getMovieId(),
                            "rating", r.getAvaliacao().toRatingScale()
                    ))
                    .toList();

            var engineResponse = engineClient.getRecommendationsFromHistory(Map.of(
                    "ratings", payload,
                    "top_n", RECOMMENDATION_COUNT
            ));

            List<MovieResponse> recs = mapEngineResults(engineResponse);
            if (recs.isEmpty()) {
                return buildResponse(recommendByGenrePreference(user), user);
            }
            return buildResponse(recs, user);

        } catch (Exception e) {
            log.warn("Motor de recomendação indisponível para o usuário {} — usando fallback local. Causa: {}",
                    userId, e.toString());
            return buildResponse(recommendByGenrePreference(user), user);
        }
    }

    public RecommendationResponse getColdStartRecommendations(ColdStartRequest request) {
        try {
            List<Integer> likedIds = request.ratings().stream()
                    .filter(r -> "like".equalsIgnoreCase(r.type()))
                    .map(ColdStartRequest.ColdStartRating::movieId)
                    .toList();

            if (likedIds.isEmpty()) {
                return buildResponse(topRatedMovies(), null);
            }

            var engineResponse = engineClient.getColdStartRecommendations(Map.of(
                    "liked_movie_ids", likedIds,
                    "top_n", RECOMMENDATION_COUNT
            ));
            List<MovieResponse> recs = mapEngineResults(engineResponse);
            return buildResponse(recs.isEmpty() ? topRatedMovies() : recs, null);

        } catch (Exception e) {
            log.warn("Motor indisponível no cold start — usando fallback local. Causa: {}", e.toString());
            return buildResponse(topRatedMovies(), null);
        }
    }

    /**
     * Monta a resposta em duas trilhas. A primeira são as recomendações do motor;
     * a segunda são filmes bem avaliados nos gêneros preferidos do usuário — um
     * sinal de fato diferente, e não a mesma lista cortada ao meio como antes.
     */
    private RecommendationResponse buildResponse(List<MovieResponse> primary, AppUser user) {
        Set<Integer> alreadyShown = new HashSet<>();
        primary.forEach(m -> alreadyShown.add(m.id()));

        List<MovieResponse> byGenre = (user == null ? List.<MovieResponse>of() : recommendByGenrePreference(user))
                .stream()
                .filter(m -> !alreadyShown.contains(m.id()))
                .limit(10)
                .toList();

        return new RecommendationResponse(primary.stream().limit(10).toList(), byGenre);
    }

    /** Filmes mais bem avaliados nos gêneros que o usuário marcou como preferidos. */
    private List<MovieResponse> recommendByGenrePreference(AppUser user) {
        List<String> preferences = user.getGenresPreference();
        if (preferences == null || preferences.isEmpty()) {
            return topRatedMovies();
        }

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "rating"));
        List<MovieResponse> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();

        for (String preference : preferences) {
            String genre = MovieService.translateGenre(preference);
            for (Movie movie : movieRepository.findByGenresContainingIgnoreCase(genre, pageable)) {
                if (movie.getMovieId() != null && seen.add(movie.getMovieId())) {
                    result.add(MovieResponse.from(movie));
                }
            }
        }
        return result.isEmpty() ? topRatedMovies() : result;
    }

    /**
     * O motor devolve gêneros separados por barra ("Crime|Drama"); o catálogo
     * local usa vírgula. Sem normalizar, a mesma tela mostrava os dois formatos.
     */
    private String normalizeGenres(String genres) {
        return genres == null ? "" : genres.replace("|", ", ");
    }

    private List<MovieResponse> topRatedMovies() {
        var pageable = PageRequest.of(0, RECOMMENDATION_COUNT, Sort.by(Sort.Direction.DESC, "rating"));
        return movieRepository.findAll(pageable).getContent().stream()
                .map(MovieResponse::from)
                .toList();
    }

    /**
     * Converte a resposta do motor em DTOs, buscando os metadados no catálogo local.
     * Faz uma única consulta para todos os ids, em vez de uma por filme.
     */
    @SuppressWarnings("unchecked")
    private List<MovieResponse> mapEngineResults(Map<String, Object> engineResponse) {
        var results = (List<Map<String, Object>>) engineResponse.get("results");
        if (results == null || results.isEmpty()) return List.of();

        List<Integer> movieIds = results.stream()
                .map(item -> item.get("movie_id"))
                .filter(Objects::nonNull)
                .map(id -> ((Number) id).intValue())
                .toList();

        Map<Integer, Movie> catalog = new HashMap<>();
        movieRepository.findByMovieIdIn(movieIds)
                .forEach(movie -> catalog.put(movie.getMovieId(), movie));

        List<MovieResponse> movies = new ArrayList<>();
        for (var item : results) {
            if (item.get("movie_id") == null) continue;
            int movieId = ((Number) item.get("movie_id")).intValue();

            Movie movie = catalog.get(movieId);
            if (movie != null) {
                movies.add(MovieResponse.from(movie));
            } else {
                // O motor conhece um filme que não está no catálogo local — ele é
                // treinado sobre um dataset maior que o importado pelo backend.
                // Devolve o mínimo que ele forneceu, para não sumir com a
                // recomendação.
                //
                // A nota vai como 0 (desconhecida), e não como o score do motor:
                // score é similaridade em [0,1] e nota é escala 0–10. Preencher um
                // com o outro fazia a interface exibir "0.42292984170696807" onde o
                // usuário lê nota do filme.
                movies.add(new MovieResponse(
                        movieId,
                        (String) item.getOrDefault("title", "Desconhecido"),
                        item.get("year") != null ? ((Number) item.get("year")).intValue() : 0,
                        normalizeGenres((String) item.getOrDefault("genres", "")),
                        0,
                        0,
                        "",
                        "",
                        List.of()
                ));
            }
        }
        return movies;
    }
}
