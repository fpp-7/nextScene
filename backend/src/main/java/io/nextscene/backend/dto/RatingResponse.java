package io.nextscene.backend.dto;

/** O id é o UUID da avaliação em texto (ver nota em {@link UserResponse}). */
public record RatingResponse(
        String id,
        int movieId,
        String type
) {}
