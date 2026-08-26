package io.nextscene.backend.dto;

/**
 * @param token        access token JWT, curto (30 min por padrão)
 * @param refreshToken token opaco de longa duração, usado em /api/v1/auth/refresh.
 *                     Vale uma única vez: cada renovação devolve um novo.
 */
public record AuthResponse(
        String token,
        String refreshToken,
        UserResponse user
) {}
