package io.nextscene.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recomendações com o motor fora do ar.
 * <p>
 * A URL aponta para uma porta onde não há nada escutando, então todo teste aqui
 * exercita o caminho de fallback: o app não pode devolver 500 nem tela vazia
 * quando o serviço de ML está indisponível.
 */
@TestPropertySource(properties = "app.recommendation-engine.url=http://localhost:59999")
class RecommendationsIntegrationTest extends IntegrationTestBase {

    private String bearer() throws Exception {
        return "Bearer " + registerAndGetToken(uniqueEmail());
    }

    @Test
    @DisplayName("com o motor fora do ar, ainda devolve recomendações")
    void fallsBackWhenEngineIsDown() throws Exception {
        mockMvc.perform(get("/api/recommendations").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiPicks").isArray())
                .andExpect(jsonPath("$.similarUsers").isArray())
                .andExpect(jsonPath("$.aiPicks.length()").value(
                        org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("usuário sem histórico recebe sugestões pelos gêneros preferidos")
    void usesGenrePreferencesWhenThereIsNoHistory() throws Exception {
        String token = bearer();
        var genres = objectMapper.writeValueAsString(Map.of("liked", List.of("Acao", "Drama")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/users/me/genres")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(genres))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/recommendations").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiPicks.length()").value(
                        org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("as duas trilhas não repetem os mesmos filmes")
    void theTwoTracksDoNotOverlap() throws Exception {
        String token = bearer();
        var genres = objectMapper.writeValueAsString(Map.of("liked", List.of("Acao")));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/users/me/genres")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(genres));

        String response = mockMvc.perform(get("/api/recommendations").header("Authorization", token))
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(response);
        var aiIds = idsOf(json.get("aiPicks"));
        var genreIds = idsOf(json.get("similarUsers"));

        org.assertj.core.api.Assertions.assertThat(aiIds)
                .as("sem a primeira trilha o teste não prova nada")
                .isNotEmpty();

        // Antes as duas seções eram a mesma lista cortada ao meio, e a segunda
        // ainda se chamava "Usuários Similares".
        if (!genreIds.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(aiIds).doesNotContainAnyElementsOf(genreIds);
        }
    }

    /** Extrai os ids dos filmes de uma das trilhas da resposta. */
    private List<Integer> idsOf(tools.jackson.databind.JsonNode track) {
        var ids = new java.util.ArrayList<Integer>();
        track.forEach(movie -> ids.add(movie.get("id").asInt()));
        return ids;
    }

    @Test
    @DisplayName("cold start sem curtidas ainda responde com sugestões")
    void coldStartWithoutLikesStillResponds() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "ratings", List.of(Map.of("movieId", 1, "type", "dislike"))));

        mockMvc.perform(post("/api/recommendations/cold-start")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiPicks").isArray());
    }

    @Test
    @DisplayName("recomendações exigem autenticação")
    void recommendationsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().is4xxClientError());
    }
}
