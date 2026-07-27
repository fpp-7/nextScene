package io.nextscene.backend.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    private LoginRateLimiter limiter(int maxAttempts) {
        return new LoginRateLimiter(maxAttempts, 15);
    }

    @Test
    @DisplayName("permite tentativas até o limite e bloqueia a seguinte")
    void blocksAfterLimit() {
        var limiter = limiter(3);

        assertThat(limiter.tryConsume("10.0.0.1")).isTrue();
        assertThat(limiter.tryConsume("10.0.0.1")).isTrue();
        assertThat(limiter.tryConsume("10.0.0.1")).isTrue();
        assertThat(limiter.tryConsume("10.0.0.1")).isFalse();
    }

    @Test
    @DisplayName("conta cada cliente separadamente")
    void countsPerClient() {
        var limiter = limiter(2);

        limiter.tryConsume("10.0.0.1");
        limiter.tryConsume("10.0.0.1");

        assertThat(limiter.tryConsume("10.0.0.1")).isFalse();
        assertThat(limiter.tryConsume("10.0.0.2"))
                .as("um cliente bloqueado não pode bloquear os demais")
                .isTrue();
    }

    @Test
    @DisplayName("login bem-sucedido zera o contador")
    void resetClearsTheCounter() {
        var limiter = limiter(2);

        limiter.tryConsume("10.0.0.1");
        limiter.tryConsume("10.0.0.1");
        assertThat(limiter.tryConsume("10.0.0.1")).isFalse();

        limiter.reset("10.0.0.1");

        assertThat(limiter.tryConsume("10.0.0.1")).isTrue();
    }

    @Test
    @DisplayName("permanece bloqueado enquanto a janela não expira")
    void staysBlockedWithinTheWindow() {
        var limiter = limiter(1);
        limiter.tryConsume("10.0.0.1");

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryConsume("10.0.0.1")).isFalse();
        }
    }
}
