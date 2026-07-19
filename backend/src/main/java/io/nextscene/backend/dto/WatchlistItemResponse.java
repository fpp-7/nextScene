package io.nextscene.backend.dto;

public record WatchlistItemResponse(
        long id,
        int movieId,
        MovieResponse movie,
        String addedAt
) {}
