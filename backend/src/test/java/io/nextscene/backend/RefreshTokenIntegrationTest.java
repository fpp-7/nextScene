package io.nextscene.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O access token dura 30 minutos e não havia como renová-lo: o usuário voltava à
 * tela de login a cada meia hora de uso.
 */
class RefreshTokenIntegrationTest extends IntegrationTestBase {

    private record Session(String access, String refresh) {}

    private Session signUp() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "name", "Felipe", "email", uniqueEmail(), "password", "senha123"));

        var json = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString());

        return new Session(json.get("token").asString(), json.get("refreshToken").asString());
    }

    private String refreshWith(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", token))))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("cadastro e login devolvem refresh token")
    void authReturnsRefreshToken() throws Exception {
        var session = signUp();

        assertThat(session.refresh()).isNotBlank();
        assertThat(session.refresh()).isNotEqualTo(session.access());
    }

    @Test
    @DisplayName("o refresh devolve um novo par de tokens que funciona")
    void refreshIssuesWorkingTokens() throws Exception {
        var session = signUp();

        var json = objectMapper.readTree(refreshWith(session.refresh()));
        String newAccess = json.get("token").asString();

        assertThat(newAccess).isNotBlank();
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("cada renovação devolve um refresh token diferente")
    void refreshTokenIsRotated() throws Exception {
        var session = signUp();

        var json = objectMapper.readTree(refreshWith(session.refresh()));

        assertThat(json.get("refreshToken").asString()).isNotEqualTo(session.refresh());
    }

    @Test
    @DisplayName("o refresh token usado não vale uma segunda vez")
    void usedRefreshTokenIsRejected() throws Exception {
        var session = signUp();
        refreshWith(session.refresh());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", session.refresh()))))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Apresentar um token já rotacionado significa repetição de requisição ou
     * roubo — e não há como distinguir. Trata-se como roubo: toda a sessão cai.
     */
    @Test
    @DisplayName("reutilizar um token antigo derruba a sessão inteira")
    void reuseRevokesEveryToken() throws Exception {
        var session = signUp();
        String segundo = objectMapper.readTree(refreshWith(session.refresh()))
                .get("refreshToken").asString();

        // O atacante apresenta o token antigo.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", session.refresh()))))
                .andExpect(status().isUnauthorized());

        // O token legítimo do dono também deixa de valer.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", segundo))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token desconhecido é rejeitado")
    void unknownTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", "token-que-nunca-existiu"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("sair da conta invalida o refresh token no servidor")
    void logoutRevokesRefreshToken() throws Exception {
        var session = signUp();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + session.access()))
                .andExpect(status().isOk());

        // Regressão: antes o logout era um no-op e o aparelho continuava
        // conseguindo renovar o acesso indefinidamente.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", session.refresh()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("o refresh de um usuário não serve para outro")
    void refreshIsScopedToItsOwner() throws Exception {
        var alice = signUp();
        var bob = signUp();

        String email = objectMapper.readTree(refreshWith(alice.refresh()))
                .get("user").get("email").asString();
        String bobEmail = objectMapper.readTree(refreshWith(bob.refresh()))
                .get("user").get("email").asString();

        assertThat(email).isNotEqualTo(bobEmail);
    }

    @Test
    @DisplayName("o endpoint de refresh é público")
    void refreshEndpointIsPublic() throws Exception {
        var session = signUp();

        // Sem header Authorization: é justamente quem está com o access vencido
        // que precisa chamar aqui.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", session.refresh()))))
                .andExpect(status().isOk());
    }
}
