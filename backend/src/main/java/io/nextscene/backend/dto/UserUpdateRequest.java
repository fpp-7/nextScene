package io.nextscene.backend.dto;

public record UserUpdateRequest(
        String name,
        String email,
        String password
) {}
