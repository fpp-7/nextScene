package io.nextscene.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-32-caracteres!!";
    private static final long THIRTY_MINUTES = 1_800_000L;

    private final JwtService jwtService = new JwtService(SECRET, THIRTY_MINUTES);

    @Test
    @DisplayName("token gerado carrega o id do usuário e é verificável")
    void generatesVerifiableToken() {
        String userId = UUID.randomUUID().toString();

        String token = jwtService.generateToken(userId, "felipe@exemplo.com");
        var decoded = jwtService.verifyToken(token);

        assertThat(decoded).isNotNull();
        assertThat(decoded.getSubject()).isEqualTo(userId);
        assertThat(decoded.getClaim("email").asString()).isEqualTo("felipe@exemplo.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("token assinado com outro segredo é rejeitado")
    void rejectsTokenSignedWithAnotherSecret() {
        var attacker = new JwtService("outro-segredo-completamente-diferente!!!!", THIRTY_MINUTES);
        String forged = attacker.generateToken(UUID.randomUUID().toString(), "invasor@exemplo.com");

        assertThat(jwtService.verifyToken(forged)).isNull();
    }

    @Test
    @DisplayName("token expirado é rejeitado")
    void rejectsExpiredToken() {
        var expiring = new JwtService(SECRET, -1_000L); // já nasce vencido
        String token = expiring.generateToken(UUID.randomUUID().toString(), "felipe@exemplo.com");

        assertThat(jwtService.verifyToken(token)).isNull();
    }

    @Test
    @DisplayName("token adulterado é rejeitado")
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(UUID.randomUUID().toString(), "felipe@exemplo.com");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThat(jwtService.verifyToken(tampered)).isNull();
    }

    @Test
    @DisplayName("lixo no lugar do token não estoura exceção")
    void handlesGarbageInput() {
        assertThat(jwtService.verifyToken("isto-nao-e-um-jwt")).isNull();
        assertThat(jwtService.extractUserId("isto-nao-e-um-jwt")).isNull();
    }
}
