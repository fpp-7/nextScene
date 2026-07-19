package io.nextscene.backend.service;

import io.nextscene.backend.client.RecommendationEngineClient;
import io.nextscene.backend.dto.ColdStartRequest;
import io.nextscene.backend.dto.MovieResponse;
import io.nextscene.backend.dto.RecommendationResponse;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.Movie;
import io.nextscene.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationEngineClient engineClient;
    private final MovieRepository movieRepository;
    private final UserService userService;
    private final MovieService movieService;

    public RecommendationResponse getRecommendations(UUID userId) {
        AppUser user = userService.findById(userId);

        try {
            // Call Python engine with user's interaction count as a pseudo user_id
            var engineResponse = engineClient.getRecommendations(
                    user.getInteractionCount() > 0 ? user.getInteractionCount() : 1,
                    20
            );

            List<MovieResponse> allRecs = mapEngineResults(engineResponse);

            // Split into two lists for the frontend
            int half = allRecs.size() / 2;
            List<MovieResponse> aiPicks = allRecs.subList(0, Math.min(half, allRecs.size()));
            List<MovieResponse> similarUsers = allRecs.subList(Math.min(half, allRecs.size()), allRecs.size());

            return new RecommendationResponse(aiPicks, similarUsers);
        } catch (Exception e) {
            log.warn("Engine unavailable, falling back to DB: {}", e.getMessage());
            return getFallbackRecommendations();
        }
    }

    public RecommendationResponse getColdStartRecommendations(ColdStartRequest request) {
        try {
            List<Integer> likedIds = request.ratings().stream()
                    .filter(r -> "like".equalsIgnoreCase(r.type()))
                    .map(ColdStartRequest.ColdStartRating::movieId)
                    .toList();

            if (likedIds.isEmpty()) {
                return getFallbackRecommendations();
            }

            var engineRequest = Map.of(
                    "liked_movie_ids", likedIds,
                    "top_n", 10
            );

            var engineResponse = engineClient.getColdStartRecommendations(engineRequest);
            List<MovieResponse> results = mapEngineResults(engineResponse);

            int half = results.size() / 2;
            return new RecommendationResponse(
                    results.subList(0, Math.min(half, results.size())),
                    results.subList(Math.min(half, results.size()), results.size())
            );
        } catch (Exception e) {
            log.warn("Cold-start engine unavailable, falling back: {}", e.getMessage());
            return getFallbackRecommendations();
        }
    }

    @SuppressWarnings("unchecked")
    private List<MovieResponse> mapEngineResults(Map<String, Object> engineResponse) {
        List<Map<String, Object>> results = (List<Map<String, Object>>) engineResponse.get("results");
        if (results == null) return List.of();

        List<MovieResponse> movies = new ArrayList<>();
        for (var item : results) {
            int movieId = ((Number) item.get("movie_id")).intValue();
            Optional<Movie> movieOpt = movieRepository.findByMovieId(movieId);
            if (movieOpt.isPresent()) {
                Movie movie = movieOpt.get();
                movieService.enrichMovieFromTmdb(movie);
                movies.add(MovieResponse.from(movie));
            } else {
                // Build a minimal response from engine data
                movies.add(new MovieResponse(
                        movieId,
                        (String) item.getOrDefault("title", "Unknown"),
                        item.get("year") != null ? ((Number) item.get("year")).intValue() : 0,
                        (String) item.getOrDefault("genres", ""),
                        item.get("score") != null ? ((Number) item.get("score")).doubleValue() : 0,
                        0,
                        "",
                        "",
                        List.of()
                ));
            }
        }
        return movies;
    }

    private RecommendationResponse getFallbackRecommendations() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "rating"));
        List<Movie> topMovies = movieRepository.findAll(pageable).getContent();
        // Enrich the first 10 fallback movies dynamically
        topMovies.stream().limit(10).forEach(movieService::enrichMovieFromTmdb);

        List<MovieResponse> responses = topMovies.stream().map(MovieResponse::from).toList();
        int half = responses.size() / 2;
        return new RecommendationResponse(
                responses.subList(0, Math.min(half, responses.size())),
                responses.subList(Math.min(half, responses.size()), responses.size())
        );
    }
}
