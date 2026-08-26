package io.nextscene.backend.infra;

import java.util.UUID;

/**
 * O principal autenticado, montado direto das claims do JWT.
 * <p>
 * Antes o {@link JwtAuthFilter} carregava o {@code AppUser} inteiro do banco
 * em toda requisição autenticada — duas queries no mínimo, já que
 * {@code genresPreference} é {@code @ElementCollection(fetch = EAGER)} —
 * só para os controllers chamarem {@code user.getId()}. O token já carrega o
 * id (subject) e o e-mail; não há por que ir ao banco buscar o que já está
 * ali.
 * <p>
 * Efeito colateral aceito: um usuário excluído continua com o token válido
 * até ele vencer (30 minutos), porque não há mais verificação de existência a
 * cada requisição. Não há hoje exclusão de conta no produto; se um dia houver,
 * ela precisa revogar os refresh tokens do usuário, como o logout já faz.
 */
public record AuthenticatedUser(UUID id, String email) {
}
