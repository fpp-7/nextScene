package io.nextscene.backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(
        name = "recommendation-engine",
        url = "${app.recommendation-engine.url}"
)
public interface RecommendationEngineClient {

    @GetMapping("/api/v1/recommend/{userId}")
    Map<String, Object> getRecommendations(
            @PathVariable("userId") int userId,
            @RequestParam("top_n") int topN
    );

    @PostMapping("/api/v1/recommend/cold-start")
    Map<String, Object> getColdStartRecommendations(@RequestBody Map<String, Object> request);

    @GetMapping("/api/v1/movies/search")
    Object searchMovies(@RequestParam("q") String query, @RequestParam("limit") int limit);
}
