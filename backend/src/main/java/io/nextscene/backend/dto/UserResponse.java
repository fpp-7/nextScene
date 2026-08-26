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
        List<String> genresExcluded
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                // Cópia, não a coleção do Hibernate: `genresPreference` é uma
                // @ElementCollection lazy, e o DTO costuma ser serializado
                // depois que a transação fecha. Passar a coleção original fazia
                // o Jackson estourar LazyInitializationException fora da sessão.
                // Copiar aqui força a carga enquanto a sessão ainda existe.
                user.getGenresPreference() == null
                        ? List.of()
                        : List.copyOf(user.getGenresPreference()),
                user.getGenresExcluded() == null
                        ? List.of()
                        : List.copyOf(user.getGenresExcluded())
        );
    }
}
