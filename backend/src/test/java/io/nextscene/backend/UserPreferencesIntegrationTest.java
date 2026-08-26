package io.nextscene.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preferências de gênero e troca de e-mail — as duas coisas que o
 * {@code UserService} passou a levar a sério.
 */
class UserPreferencesIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ─── Gêneros excluídos ────────────────────────────────────────────────────

    @Test
    @DisplayName("gêneros excluídos são gravados, não descartados em silêncio")
    void excludedGenresArePersisted() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        mockMvc.perform(put("/api/v1/users/me/genres")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "liked", List.of("Comedia"),
                                "disliked", List.of("Terror")
                        ))))
                .andExpect(status().isOk());

        // Regressão: `disliked` chegava ao backend e era jogado fora sem log.
        var excluded = jdbcTemplate.queryForList(
                "SELECT genres_excluded FROM app_user_genres_excluded", String.class);
        assertThat(excluded).contains("Terror");
    }

    @Test
    @DisplayName("uma segunda chamada substitui a lista, permitindo desfazer a exclusão")
    void excludedGenresAreReplacedNotMerged() throws Exception {
        String email = uniqueEmail();
        String token = registerAndGetToken(email);

        putGenres(token, List.of("Comedia"), List.of("Terror"));
        putGenres(token, List.of("Comedia"), List.of());

        var excluded = jdbcTemplate.queryForList("""
                SELECT e.genres_excluded FROM app_user_genres_excluded e
                  JOIN app_user u ON u.id = e.app_user_id
                 WHERE u.email = ?
                """, String.class, email);
        assertThat(excluded).isEmpty();
    }

    @Test
    @DisplayName("um gênero excluído não volta pelas recomendações")
    void excludedGenreIsAbsentFromRecommendations() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        putGenres(token, List.of("Terror", "Drama"), List.of("Terror"));

        // Sem histórico, a resposta cai no fallback por preferência de gênero —
        // que inclui Terror entre os curtidos. O veto tem que vencer mesmo assim.
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..genre").value(
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.not(
                                        org.hamcrest.Matchers.containsString("Horror")))));
    }

    // ─── Troca de e-mail ──────────────────────────────────────────────────────

    @Test
    @DisplayName("trocar de e-mail encerra as sessões ativas")
    void changingEmailRevokesRefreshTokens() throws Exception {
        String email = uniqueEmail();

        var loginBody = objectMapper.writeValueAsString(Map.of(
                "name", "Usuario Teste", "email", email, "password", "senha123"));
        var registered = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(registered);
        String token = json.get("token").asString();
        String refreshToken = json.get("refreshToken").asString();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", uniqueEmail()))))
                .andExpect(status().isOk());

        // O access token antigo carrega o e-mail antigo nas claims; sem revogar,
        // ele continuaria renovável por 30 dias com um principal desatualizado.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("editar o perfil sem mexer no e-mail preserva a sessão")
    void renamingDoesNotRevokeTheSession() throws Exception {
        String email = uniqueEmail();
        var body = objectMapper.writeValueAsString(Map.of(
                "name", "Usuario Teste", "email", email, "password", "senha123"));
        var registered = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(registered);
        String token = json.get("token").asString();
        String refreshToken = json.get("refreshToken").asString();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Outro Nome", "email", email))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk());
    }

    private void putGenres(String token, List<String> liked, List<String> disliked) throws Exception {
        mockMvc.perform(put("/api/v1/users/me/genres")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("liked", liked, "disliked", disliked))))
                .andExpect(status().isOk());
    }
}
