package io.nextscene.backend.dto;

import io.nextscene.backend.model.AppUser;

import java.util.List;

public record UserResponse(
        long id,
        String name,
        String email,
        List<String> genresPreference,
        int interactionCount
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId().getMostSignificantBits() & Long.MAX_VALUE,
                user.getName(),
                user.getEmail(),
                user.getGenresPreference(),
                user.getInteractionCount()
        );
    }
}
