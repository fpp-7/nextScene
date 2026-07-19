package io.nextscene.backend.dto;

public record RatingResponse(
        long id,
        int movieId,
        String type
) {}
