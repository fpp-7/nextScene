package io.nextscene.backend.dto;

import java.util.List;

public record GenrePreferenceRequest(
        List<String> liked,
        List<String> disliked
) {}
