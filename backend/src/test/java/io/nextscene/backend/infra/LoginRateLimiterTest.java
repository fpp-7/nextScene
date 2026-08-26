package io.nextscene.backend.infra;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra um Redis real via Testcontainers — o rate limiter agora depende dele
 * para o contador, então não há como testar sem, no mesmo espírito do Postgres
 * real usado nos testes de integração deste projeto.
 * <p>
 * <b>Requer Docker em execução.</b>
 */
class LoginRateLimiterTest {

    private static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    private static StringRedisTemplate redisTemplate;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
        var connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getFirstMappedPort()));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void stopRedis() {
        REDIS.stop();
    }

    private LoginRateLimiter limiter(int maxAttempts) {
        return new LoginRateLimiter(redisTemplate, maxAttempts, 15);
    }

    /** Prefixo único por teste — os testes compartilham o mesmo Redis. */
    private String client(String suffix) {
        return java.util.UUID.randomUUID() + "-" + suffix;
    }

    @Test
    @DisplayName("permite tentativas até o limite e bloqueia a seguinte")
    void blocksAfterLimit() {
        var limiter = limiter(3);
        String ip = client("10.0.0.1");

        assertThat(limiter.tryConsume(ip)).isTrue();
        assertThat(limiter.tryConsume(ip)).isTrue();
        assertThat(limiter.tryConsume(ip)).isTrue();
        assertThat(limiter.tryConsume(ip)).isFalse();
    }

    @Test
    @DisplayName("conta cada cliente separadamente")
    void countsPerClient() {
        var limiter = limiter(2);
        String ip1 = client("10.0.0.1");
        String ip2 = client("10.0.0.2");

        limiter.tryConsume(ip1);
        limiter.tryConsume(ip1);

        assertThat(limiter.tryConsume(ip1)).isFalse();
        assertThat(limiter.tryConsume(ip2))
                .as("um cliente bloqueado não pode bloquear os demais")
                .isTrue();
    }

    @Test
    @DisplayName("login bem-sucedido zera o contador")
    void resetClearsTheCounter() {
        var limiter = limiter(2);
        String ip = client("10.0.0.1");

        limiter.tryConsume(ip);
        limiter.tryConsume(ip);
        assertThat(limiter.tryConsume(ip)).isFalse();

        limiter.reset(ip);

        assertThat(limiter.tryConsume(ip)).isTrue();
    }

    @Test
    @DisplayName("permanece bloqueado enquanto a janela não expira")
    void staysBlockedWithinTheWindow() {
        var limiter = limiter(1);
        String ip = client("10.0.0.1");
        limiter.tryConsume(ip);

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryConsume(ip)).isFalse();
        }
    }

    @Test
    @DisplayName("o contador é compartilhado entre instâncias diferentes do limiter")
    void counterIsSharedAcrossLimiterInstances() {
        // É o comportamento que motivou a troca do Caffeine pelo Redis: duas
        // instâncias (equivalente a dois nós do backend) enxergam o mesmo estado.
        var nodeA = limiter(2);
        var nodeB = limiter(2);
        String ip = client("10.0.0.1");

        assertThat(nodeA.tryConsume(ip)).isTrue();
        assertThat(nodeB.tryConsume(ip)).isTrue();
        assertThat(nodeA.tryConsume(ip))
                .as("a 3ª tentativa, em qualquer nó, já deve estourar o limite de 2")
                .isFalse();
    }
}
