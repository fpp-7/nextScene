package io.nextscene.backend.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Limita tentativas de login por IP, em janela deslizante simples, com
 * contador compartilhado no Redis.
 * <p>
 * Sem isso, {@code /api/v1/auth/login} aceitava força bruta ilimitada: não há
 * bloqueio de conta, captcha nem backoff, e a política de senha exige apenas 6
 * caracteres.
 * <p>
 * Antes o contador era um {@code Caffeine} em memória: com mais de um nó do
 * backend, o limite efetivo multiplicava pelo número de nós, porque cada
 * requisição podia cair num nó diferente com seu próprio contador zerado.
 * Com Redis, todos os nós compartilham a mesma contagem por IP.
 * <p>
 * <b>Se o Redis cair:</b> a tentativa é permitida e o incidente é logado em
 * {@code WARN}, não bloqueado. Derrubar o login inteiro porque o cache caiu
 * trocaria um problema de segurança por uma indisponibilidade — pior para
 * todo mundo, não só para quem estivesse de fato atacando.
 */
@Slf4j
@Component
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "nextscene:login-attempts:";

    private final StringRedisTemplate redis;
    private final int maxAttempts;
    private final Duration window;

    public LoginRateLimiter(
            StringRedisTemplate redis,
            @Value("${app.security.login.max-attempts:10}") int maxAttempts,
            @Value("${app.security.login.window-minutes:15}") long windowMinutes
    ) {
        this.redis = redis;
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    /** Registra uma tentativa de login e diz se ela ainda está dentro do limite. */
    public boolean tryConsume(String clientKey) {
        return tryConsume(clientKey, maxAttempts);
    }

    /**
     * Variante com teto próprio, para chaves que não são login.
     * <p>
     * O cadastro precisa de um limite bem mais folgado que o login: atrás de
     * CGNAT ou da saída de uma empresa, dezenas de pessoas legítimas
     * compartilham o mesmo IP, e o custo de errar aqui é impedir alguém de
     * criar conta — pior que o abuso que o limite tenta conter.
     */
    public boolean tryConsume(String clientKey, int limit) {
        String key = KEY_PREFIX + clientKey;
        try {
            Long count = redis.opsForValue().increment(key);
            // TTL só na primeira tentativa da janela — reaplicar a cada chamada
            // faria a janela deslizar para sempre e o limite nunca valeria.
            if (count != null && count == 1L) {
                redis.expire(key, window);
            }
            return count == null || count <= limit;
        } catch (Exception e) {
            log.warn("Redis indisponível para rate limit de login (chave {}) — permitindo a tentativa.",
                    clientKey, e);
            return true;
        }
    }

    /** Zera o contador — chamado após um login bem-sucedido. */
    public void reset(String clientKey) {
        try {
            redis.delete(KEY_PREFIX + clientKey);
        } catch (Exception e) {
            log.warn("Redis indisponível para resetar o rate limit de login (chave {}).", clientKey, e);
        }
    }
}
