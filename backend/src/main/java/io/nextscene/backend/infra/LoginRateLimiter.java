package io.nextscene.backend.infra;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita tentativas de login por IP, em janela deslizante simples.
 * <p>
 * Sem isso, {@code /api/auth/login} aceitava força bruta ilimitada: não há
 * bloqueio de conta, captcha nem backoff, e a política de senha exige apenas 6
 * caracteres.
 * <p>
 * Limitação conhecida: o estado é por instância. Com mais de um nó do backend,
 * o limite efetivo é multiplicado pelo número de nós — para valer de verdade em
 * produção, precisa de um contador compartilhado (Redis).
 */
@Component
public class LoginRateLimiter {

    private final Cache<String, AtomicInteger> attempts;
    private final int maxAttempts;

    public LoginRateLimiter(
            @Value("${app.security.login.max-attempts:10}") int maxAttempts,
            @Value("${app.security.login.window-minutes:15}") long windowMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.attempts = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(windowMinutes))
                .maximumSize(100_000)
                .build();
    }

    /** Registra uma tentativa e diz se ela ainda está dentro do limite. */
    public boolean tryConsume(String clientKey) {
        AtomicInteger counter = attempts.get(clientKey, k -> new AtomicInteger());
        return counter.incrementAndGet() <= maxAttempts;
    }

    /** Zera o contador — chamado após um login bem-sucedido. */
    public void reset(String clientKey) {
        attempts.invalidate(clientKey);
    }
}
