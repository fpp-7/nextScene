package io.nextscene.backend.service;

import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.RefreshToken;
import io.nextscene.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Emite, valida e revoga refresh tokens.
 *
 * <h2>Rotação</h2>
 * Cada renovação consome o token apresentado e devolve um novo. Um refresh
 * token vale exatamente uma vez, o que limita a janela de uso caso ele vaze.
 *
 * <h2>Detecção de reuso</h2>
 * Se um token já revogado for apresentado, ou o cliente está repetindo uma
 * requisição antiga ou alguém roubou o token. Não há como distinguir os dois
 * casos, então tratamos como roubo e revogamos <b>todos</b> os tokens do
 * usuário — quem tiver o token roubado perde o acesso, e o dono real refaz o
 * login. É o comportamento recomendado pela RFC 6819 para clientes públicos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;

    @Value("${app.jwt.refresh-expiration-days:30}")
    private long expirationDays;

    /** Emite um token novo. O valor em claro é devolvido uma única vez. */
    @Transactional
    public String issue(AppUser user) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        var entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(token));
        entity.setExpiresAt(Instant.now().plus(Duration.ofDays(expirationDays)));
        repository.save(entity);

        return token;
    }

    /**
     * Consome o token apresentado e emite um substituto.
     *
     * @return o usuário e o novo refresh token
     * @throws InvalidRefreshTokenException se o token for desconhecido, expirado
     *                                      ou já utilizado
     */
    /*
     * `noRollbackFor` é essencial aqui, não um detalhe.
     *
     * Ao detectar reuso, revogamos todos os tokens do usuário e sinalizamos o
     * erro com uma exceção. Só que RuntimeException dispara rollback por padrão
     * — e o rollback desfazia justamente a revogação. A resposta de segurança
     * era anulada pelo sinal que a acionou, e o atacante seguia com acesso.
     *
     * A exceção aqui é fluxo de controle esperado, não falha de infraestrutura.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotated rotate(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken stored = repository.findByTokenHash(hash(presentedToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isRevoked()) {
            // Token já rotacionado sendo apresentado de novo: trata-se como
            // vazamento e derruba a sessão inteira.
            log.warn("Refresh token reutilizado (usuário {}) — revogando todos os tokens.",
                    stored.getUser().getId());
            repository.revokeAllForUser(stored.getUser(), Instant.now());
            throw new InvalidRefreshTokenException();
        }

        if (stored.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        stored.setRevokedAt(Instant.now());
        repository.save(stored);

        AppUser user = stored.getUser();
        return new Rotated(user, issue(user));
    }

    /** Encerra a sessão em todos os dispositivos do usuário. */
    @Transactional
    public void revokeAll(AppUser user) {
        int revoked = repository.revokeAllForUser(user, Instant.now());
        log.debug("{} refresh tokens revogados para o usuário {}.", revoked, user.getId());
    }

    /**
     * Remove tokens vencidos. Sem isto a tabela cresce indefinidamente: cada
     * renovação deixa para trás o token consumido.
     */
    @Scheduled(cron = "${app.jwt.refresh-cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void purgeExpired() {
        // Margem de alguns dias para que um token recém-vencido ainda apareça
        // em investigação de incidente.
        Instant threshold = Instant.now().minus(Duration.ofDays(7));
        int removed = repository.deleteExpiredBefore(threshold);
        if (removed > 0) {
            log.info("🧹 {} refresh tokens expirados removidos.", removed);
        }
    }

    private String hash(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", e);
        }
    }

    public record Rotated(AppUser user, String refreshToken) {}

    /** Mensagem genérica: não revela se o token é inexistente, expirado ou usado. */
    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException() {
            super("Sessão inválida. Entre novamente.");
        }
    }
}
