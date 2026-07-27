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
    /**
     * O docker-compose usa este endpoint como healthcheck. Ele estava
     * configurado no application.yml, mas a dependência do actuator nunca havia
     * sido declarada — a rota não existia e o container ficava eternamente
     * "unhealthy". Nenhum teste pegava isso porque nenhum a chamava.
     */
    @Test
    @DisplayName("/actuator/health responde e é público")
    void healthEndpointIsAvailable() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/actuator/health"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isOk());
    }

    @Test
    @DisplayName("rota pública inexistente devolve 404, não 500")
    void unknownPublicRouteIsNotFound() throws Exception {
        // Regressão: o handler genérico capturava NoResourceFoundException e
        // devolvia 500. Foi assim que /actuator/health, sem a dependência do
        // actuator, virou "erro interno" em vez de "rota não existe".
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/movies/rota/que/nao/existe"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isNotFound());
    }

    @Test
    @DisplayName("rota protegida inexistente é barrada antes de revelar se existe")
    void unknownProtectedRouteDoesNotLeakExistence() throws Exception {
        // 403 em vez de 404 aqui é intencional: sem autenticação, a resposta não
        // deve permitir mapear quais rotas existem.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/rota/que/nao/existe"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isForbidden());
    }

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
