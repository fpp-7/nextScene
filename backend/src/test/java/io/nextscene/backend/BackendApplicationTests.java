package io.nextscene.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O teste que teria pego o bug que impedia a API de subir.
 * <p>
 * Na versão anterior ele não conseguia rodar: sem banco disponível, falhava
 * antes de chegar em qualquer verificação — e por isso o conflito de beans de
 * CORS passou despercebido até a aplicação simplesmente não iniciar.
 */
class BackendApplicationTests extends IntegrationTestBase {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("o contexto da aplicação sobe")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    /**
     * O simples fato de o contexto subir já prova que o conflito de beans
     * acabou — duas definições com o mesmo nome abortam o startup. Aqui a
     * verificação vai além e confere o conteúdo da configuração de CORS, que
     * era permissiva demais na versão do SecurityConfig que venceu o conflito.
     */
    @Test
    @DisplayName("o CORS não libera qualquer origem com credenciais")
    void corsIsNotWideOpen() {
        var source = (CorsConfigurationSource) context.getBean("corsConfigurationSource");

        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/movies");
        var config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOriginPatterns())
                .as("origem curinga combinada com credenciais permite que qualquer site chame a API")
                .doesNotContain("*");
        assertThat(config.getAllowCredentials()).isNotEqualTo(Boolean.TRUE);
    }
}
