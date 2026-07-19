package io.nextscene.backend.dto;

import java.util.List;

public record ColdStartRequest(
        List<ColdStartRating> ratings
) {
    public record ColdStartRating(
            int movieId,
            String type
    ) {}
}
