package io.nextscene.backend.dto;

public record UserStatsResponse(
        long rated,
        long watched,
        long favorites
) {}
