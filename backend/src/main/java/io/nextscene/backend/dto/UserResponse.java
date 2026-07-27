package io.nextscene.backend.dto;

import io.nextscene.backend.model.AppUser;

import java.util.List;

/**
 * O id é a representação textual do UUID, não um número.
 * <p>
 * A versão anterior truncava o UUID com {@code getMostSignificantBits()} para
 * caber num {@code number} do JavaScript: metade dos bits era descartada, o id
 * devolvido não servia para buscar o registro de volta e havia risco real de
 * colisão entre usuários distintos.
 */
public record UserResponse(
        String id,
        String name,
        String email,
        List<String> genresPreference,
        int interactionCount
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                user.getGenresPreference(),
                user.getInteractionCount()
        );
    }
}
