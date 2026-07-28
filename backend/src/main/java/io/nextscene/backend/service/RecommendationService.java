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

    /**
     * Quantas sugestões pedir ao motor.
     * <p>
     * Bem mais que o necessário porque o motor conhece um catálogo maior que o
     * importado pelo backend, e tudo que ele sugere fora desse catálogo é
     * descartado. Limitado ao teto aceito pelo motor (MAX_TOP_N = 50).
     */
    private static final int ENGINE_CANDIDATES = 50;

    /** Sugestões exibidas em cada trilha. */
    private static final int RESULTS_PER_TRACK = 10;

    /** Quantas posições do topo saem sempre, sem sorteio (ver withVariety). */
    private static final int GUARANTEED_TOP = 4;

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

        // Recomendar algo que a pessoa já avaliou é contradição: a avaliação é o
        // insumo do algoritmo, não um resultado dele. O motor já exclui o que foi
        // visto; a trilha por gênero não excluía, e metade das sugestões dela
        // eram filmes que o usuário acabara de curtir.
        Set<Integer> alreadyRated = history.stream()
                .map(Rating::getMovie)
                .filter(m -> m != null && m.getMovieId() != null)
                .map(Movie::getMovieId)
                .collect(java.util.stream.Collectors.toSet());

        if (history.isEmpty()) {
            log.debug("Usuário {} ainda não avaliou nada — usando preferências de gênero.", userId);
            return buildResponse(recommendByGenrePreference(user, alreadyRated), user, alreadyRated);
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
                    "top_n", ENGINE_CANDIDATES
            ));

            List<MovieResponse> recs = mapEngineResults(engineResponse);
            if (recs.isEmpty()) {
                return buildResponse(recommendByGenrePreference(user, alreadyRated), user, alreadyRated);
            }
            return buildResponse(recs, user, alreadyRated);

        } catch (Exception e) {
            log.warn("Motor de recomendação indisponível para o usuário {} — usando fallback local. Causa: {}",
                    userId, e.toString());
            return buildResponse(recommendByGenrePreference(user, alreadyRated), user, alreadyRated);
        }
    }

    public RecommendationResponse getColdStartRecommendations(ColdStartRequest request) {
        // Tudo que foi avaliado no onboarding sai das sugestões, curtido ou não.
        Set<Integer> alreadyRated = request.ratings().stream()
                .map(ColdStartRequest.ColdStartRating::movieId)
                .collect(java.util.stream.Collectors.toSet());
        try {
            List<Integer> likedIds = request.ratings().stream()
                    .filter(r -> "like".equalsIgnoreCase(r.type()))
                    .map(ColdStartRequest.ColdStartRating::movieId)
                    .toList();

            if (likedIds.isEmpty()) {
                return buildResponse(topRatedMovies(alreadyRated), null, alreadyRated);
            }

            var engineResponse = engineClient.getColdStartRecommendations(Map.of(
                    "liked_movie_ids", likedIds,
                    "top_n", ENGINE_CANDIDATES
            ));
            List<MovieResponse> recs = mapEngineResults(engineResponse);
            return buildResponse(recs.isEmpty() ? topRatedMovies(alreadyRated) : recs, null, alreadyRated);

        } catch (Exception e) {
            log.warn("Motor indisponível no cold start — usando fallback local. Causa: {}", e.toString());
            return buildResponse(topRatedMovies(alreadyRated), null, alreadyRated);
        }
    }

    /**
     * Monta a resposta em duas trilhas. A primeira são as recomendações do motor;
     * a segunda são filmes bem avaliados nos gêneros preferidos do usuário — um
     * sinal de fato diferente, e não a mesma lista cortada ao meio como antes.
     */
    private RecommendationResponse buildResponse(List<MovieResponse> primary, AppUser user,
                                                 Set<Integer> alreadyRated) {
        List<MovieResponse> firstTrack = withVariety(primary.stream()
                .filter(m -> !alreadyRated.contains(m.id()))
                .toList());

        Set<Integer> alreadyShown = new HashSet<>(alreadyRated);
        firstTrack.forEach(m -> alreadyShown.add(m.id()));

        List<MovieResponse> byGenre =
                (user == null ? List.<MovieResponse>of() : recommendByGenrePreference(user, alreadyShown))
                .stream()
                .filter(m -> !alreadyShown.contains(m.id()))
                .limit(RESULTS_PER_TRACK)
                .toList();

        return new RecommendationResponse(firstTrack, byGenre);
    }

    /**
     * Escolhe as sugestões finais dentro de um conjunto maior de candidatos.
     * <p>
     * O item-item é determinístico: mesmo histórico, mesmo resultado. A resposta
     * saía byte a byte idêntica a cada atualização, e o botão de recarregar
     * prometia algo que nunca acontecia.
     * <p>
     * As primeiras posições são preservadas — são as de maior afinidade e não
     * faz sentido escondê-las. O restante é sorteado entre os candidatos
     * seguintes, que já passaram pelo mesmo filtro de qualidade. Assim a lista
     * muda a cada atualização sem cair em sugestão ruim.
     */
    private List<MovieResponse> withVariety(List<MovieResponse> candidates) {
        if (candidates.size() <= RESULTS_PER_TRACK) {
            return candidates;
        }

        List<MovieResponse> chosen = new ArrayList<>(candidates.subList(0, GUARANTEED_TOP));

        List<MovieResponse> rest = new ArrayList<>(
                candidates.subList(GUARANTEED_TOP, candidates.size()));
        Collections.shuffle(rest);
        rest.stream().limit(RESULTS_PER_TRACK - GUARANTEED_TOP).forEach(chosen::add);

        return chosen;
    }

    /** Filmes mais bem avaliados nos gêneros que o usuário marcou como preferidos. */
    private List<MovieResponse> recommendByGenrePreference(AppUser user, Set<Integer> excluded) {
        List<String> preferences = user.getGenresPreference();
        if (preferences == null || preferences.isEmpty()) {
            return topRatedMovies(excluded);
        }

        // Busca mais que o necessário porque parte será descartada pelo filtro.
        var pageable = PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "rating"));
        List<MovieResponse> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();

        for (String preference : preferences) {
            String genre = MovieService.translateGenre(preference);
            for (Movie movie : movieRepository.findByGenresContainingIgnoreCase(genre, pageable)) {
                Integer id = movie.getMovieId();
                if (id != null && !excluded.contains(id) && seen.add(id)) {
                    result.add(MovieResponse.from(movie));
                }
            }
        }
        return result.isEmpty() ? topRatedMovies(excluded) : result;
    }

    /**
     * O motor devolve gêneros separados por barra ("Crime|Drama"); o catálogo
     * local usa vírgula. Sem normalizar, a mesma tela mostrava os dois formatos.
     */
    private String normalizeGenres(String genres) {
        return genres == null ? "" : genres.replace("|", ", ");
    }

    private List<MovieResponse> topRatedMovies(Set<Integer> excluded) {
        var pageable = PageRequest.of(0, RECOMMENDATION_COUNT + excluded.size(),
                Sort.by(Sort.Direction.DESC, "rating"));
        return movieRepository.findAll(pageable).getContent().stream()
                .filter(m -> m.getMovieId() != null && !excluded.contains(m.getMovieId()))
                .map(MovieResponse::from)
                .limit(RECOMMENDATION_COUNT)
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

            // Filmes fora do catálogo local são descartados.
            //
            // O motor é treinado sobre um dataset maior que o importado pelo
            // backend, então sugere títulos que este não conhece. Antes eles eram
            // devolvidos com os dados mínimos vindos do motor, e o resultado era
            // um card sem pôster, sem nota, e que ao ser tocado levava a
            // "Filme não encontrado" — porque GET /api/movies/{id} responde 404.
            //
            // Melhor entregar menos sugestões e todas navegáveis. Quando os
            // catálogos forem sincronizados, o descarte deixa de acontecer.
            Movie movie = catalog.get(movieId);
            if (movie != null) {
                movies.add(MovieResponse.from(movie));
            }
        }
        return movies;
    }
}
