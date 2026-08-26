package io.nextscene.backend.repository;

import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Carrega o usuário junto: a renovação precisa dele para emitir o novo token. */
    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoga todos os tokens ativos de um usuário.
     * Usado ao sair da conta e ao detectar reuso de token já rotacionado.
     * <p>
     * Recebe o id, não a entidade: o logout não precisa mais carregar o
     * {@code AppUser} do banco só para revogar tokens.
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken t SET t.revokedAt = :now
            WHERE t.user.id = :userId AND t.revokedAt IS NULL
            """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    /** Remove tokens vencidos há tempo suficiente para não servirem a auditoria. */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") Instant threshold);

    long countByUserAndRevokedAtIsNull(AppUser user);
}
