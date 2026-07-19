package io.nextscene.backend.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {}
