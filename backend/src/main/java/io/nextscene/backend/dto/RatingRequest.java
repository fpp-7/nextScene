package io.nextscene.backend.dto;

import jakarta.validation.constraints.NotNull;

public record RatingRequest(
        @NotNull Integer movieId,
        @NotNull String type
) {}
