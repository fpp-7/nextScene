package io.nextscene.backend.dto;

import java.util.List;

public record RecommendationResponse(
        List<MovieResponse> aiPicks,
        List<MovieResponse> similarUsers
) {}
