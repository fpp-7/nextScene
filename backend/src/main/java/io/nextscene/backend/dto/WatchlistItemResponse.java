package io.nextscene.backend.dto;

/** O id é o UUID do item em texto (ver a nota em {@link UserResponse}). */
public record WatchlistItemResponse(
        String id,
        int movieId,
        MovieResponse movie,
        String addedAt
) {}
