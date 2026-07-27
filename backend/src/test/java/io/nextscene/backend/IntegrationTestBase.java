package io.nextscene.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import tools.jackson.databind.ObjectMapper;

/**
 * Base dos testes de integração.
 * <p>
 * Sobe um Postgres real via Testcontainers, então as migrations do Flyway rodam
 * de verdade — inclusive a V5, que cria a extensão pg_trgm e os índices GIN.
 * Um banco em memória aceitaria o SQL mas não provaria nada sobre o Postgres.
 * <p>
 * <b>Requer Docker em execução.</b>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    /**
     * Container singleton, iniciado uma vez para toda a execução.
     * <p>
     * De propósito <b>sem</b> {@code @Testcontainers}/{@code @Container}: essas
     * anotações amarram o ciclo de vida do container à classe de teste, e o
     * Spring reaproveita o contexto entre classes. Na primeira versão o container
     * era derrubado ao fim da primeira classe e as seguintes tentavam falar com
     * uma porta morta ("Connection refused"). Sem gerenciamento do JUnit, o
     * container vive enquanto a JVM viver e o Ryuk o remove no final.
     */
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** Registra um usuário novo e devolve o token dele. */
    protected String registerAndGetToken(String email) throws Exception {
        var body = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Usuario Teste",
                "email", email,
                "password", "senha123"
        ));

        var response = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/auth/register")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(body))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asString();
    }

    /** E-mail único por teste, para não colidir com a constraint de unicidade. */
    protected String uniqueEmail() {
        return "teste-" + java.util.UUID.randomUUID() + "@exemplo.com";
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // A aplicação se recusa a subir sem JWT_SECRET — comportamento desejado,
        // então o teste fornece um explicitamente.
        registry.add("app.jwt.secret", () -> "segredo-de-teste-com-mais-de-32-caracteres!!");
        // Sem chave do TMDB o job de enriquecimento não sai chamando a rede.
        registry.add("app.tmdb.api-key", () -> "");
        registry.add("app.tmdb.enabled", () -> false);
    }
}
