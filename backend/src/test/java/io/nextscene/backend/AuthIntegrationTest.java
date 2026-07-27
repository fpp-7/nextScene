package io.nextscene.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    private static final String UUID_PATTERN =
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    @Test
    @DisplayName("cadastro devolve 201 com token e id em formato UUID")
    void registerReturnsTokenAndUuidId() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "name", "Felipe", "email", uniqueEmail(), "password", "senha123"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                // Regressão: o id era um long truncado do UUID, que perdia metade
                // dos bits e não servia para localizar o registro de volta.
                .andExpect(jsonPath("$.user.id").value(org.hamcrest.Matchers.matchesPattern(UUID_PATTERN)));
    }

    @Test
    @DisplayName("senha errada devolve 401, não 400")
    void wrongPasswordIsUnauthorized() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email);

        var body = objectMapper.writeValueAsString(Map.of("email", email, "password", "senha-errada"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("e-mail inexistente devolve a mesma mensagem de senha errada")
    void unknownEmailDoesNotLeakExistence() throws Exception {
        var body = objectMapper.writeValueAsString(
                Map.of("email", uniqueEmail(), "password", "seja-o-que-for"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas."));
    }

    @Test
    @DisplayName("senha curta demais é recusada na validação")
    void shortPasswordIsRejected() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "name", "Felipe", "email", uniqueEmail(), "password", "123"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rota protegida sem token é barrada")
    void protectedRouteRequiresToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("rota protegida com token válido responde o perfil")
    void protectedRouteAcceptsValidToken() throws Exception {
        String email = uniqueEmail();
        String token = registerAndGetToken(email);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.matchesPattern(UUID_PATTERN)));
    }

    @Test
    @DisplayName("token forjado é rejeitado")
    void forgedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer nao.e.um.token"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("catálogo é público, mas só para leitura")
    void catalogIsPublicForReadsOnly() throws Exception {
        mockMvc.perform(get("/api/movies?size=1")).andExpect(status().isOk());
        mockMvc.perform(post("/api/watchlist/1")).andExpect(status().is4xxClientError());
    }
}
