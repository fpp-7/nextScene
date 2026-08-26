package io.nextscene.backend.infra;

import io.nextscene.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressão: o filtro montava o principal
 * carregando o {@code AppUser} do banco a cada requisição autenticada — duas
 * queries no mínimo, com {@code genresPreference} sendo
 * {@code @ElementCollection(fetch = EAGER)}. Estes testes travam que o
 * {@link AuthenticatedUser} vem só das claims do token, sem repositório
 * nenhum envolvido: o construtor do filtro nem aceita um.
 */
class JwtAuthFilterTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-32-caracteres!!";
    private static final long THIRTY_MINUTES = 1_800_000L;

    private final JwtService jwtService = new JwtService(SECRET, THIRTY_MINUTES);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("monta o principal a partir das claims do token, sem ir ao banco")
    void buildsPrincipalFromTokenClaims() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId.toString(), "felipe@exemplo.com");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

        var principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.id()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("felipe@exemplo.com");
    }

    @Test
    @DisplayName("sem header Authorization, não autentica")
    void noHeaderNoAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("token inválido não autentica")
    void invalidTokenNoAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token.invalido.aqui");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
