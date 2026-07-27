package io.nextscene.backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(
        name = "recommendation-engine",
        url = "${app.recommendation-engine.url}",
        fallbackFactory = RecommendationEngineFallback.class
)
public interface RecommendationEngineClient {

    /**
     * Recomendações a partir do histórico real do usuário do app.
     * O motor é stateless: recebe as avaliações vindas do Postgres a cada chamada.
     */
    @PostMapping("/api/v1/recommend/history")
    Map<String, Object> getRecommendationsFromHistory(@RequestBody Map<String, Object> request);

    @PostMapping("/api/v1/recommend/cold-start")
    Map<String, Object> getColdStartRecommendations(@RequestBody Map<String, Object> request);

    @GetMapping("/api/v1/movies/search")
    Object searchMovies(@RequestParam("q") String query, @RequestParam("limit") int limit);
}
