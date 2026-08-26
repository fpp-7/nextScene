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
        mockMvc.perform(get("/api/v1/recommendations").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiPicks").isArray())
                .andExpect(jsonPath("$.byGenre").isArray())
                .andExpect(jsonPath("$.aiPicks.length()").value(
                        org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("usuário sem histórico recebe sugestões pelos gêneros preferidos")
    void usesGenrePreferencesWhenThereIsNoHistory() throws Exception {
        String token = bearer();
        var genres = objectMapper.writeValueAsString(Map.of("liked", List.of("Acao", "Drama")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/users/me/genres")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(genres))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/recommendations").header("Authorization", token))
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
                .put("/api/v1/users/me/genres")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(genres));

        String response = mockMvc.perform(get("/api/v1/recommendations").header("Authorization", token))
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(response);
        var aiIds = idsOf(json.get("aiPicks"));
        var genreIds = idsOf(json.get("byGenre"));

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
        return idsOf(track, "id");
    }

    /** Extrai o campo indicado de cada item de um array JSON. */
    private List<Integer> idsOf(tools.jackson.databind.JsonNode array, String field) {
        var ids = new java.util.ArrayList<Integer>();
        array.forEach(item -> ids.add(item.get(field).asInt()));
        return ids;
    }

    @Test
    @DisplayName("cold start sem curtidas ainda responde com sugestões")
    void coldStartWithoutLikesStillResponds() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "ratings", List.of(Map.of("movieId", 1, "type", "dislike"))));

        mockMvc.perform(post("/api/v1/recommendations/cold-start")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiPicks").isArray());
    }

    /**
     * Avaliação é insumo do algoritmo, não resultado dele. Recomendar de volta o
     * filme que a pessoa acabou de curtir é contradição — e era o que a trilha
     * por gênero fazia, com metade das sugestões sendo filmes já avaliados.
     */
    @Test
    @DisplayName("nenhuma trilha recomenda filme que o usuário já avaliou")
    void neverRecommendsAlreadyRatedMovies() throws Exception {
        String token = bearer();

        var genres = objectMapper.writeValueAsString(Map.of("liked", List.of("Acao", "Drama", "Crime")));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/v1/users/me/genres")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(genres));

        // Avalia justamente filmes bem pontuados, que a trilha por gênero
        // escolheria por estarem no topo da ordenação por nota.
        var ratings = objectMapper.writeValueAsString(List.of(
                Map.of("movieId", 318, "type", "like"),
                Map.of("movieId", 858, "type", "like"),
                Map.of("movieId", 296, "type", "like"),
                Map.of("movieId", 2959, "type", "seen"),
                Map.of("movieId", 50, "type", "dislike")
        ));
        mockMvc.perform(post("/api/v1/ratings/batch")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(ratings))
                .andExpect(status().isCreated());

        var rated = idsOf(objectMapper.readTree(
                mockMvc.perform(get("/api/v1/ratings/me").header("Authorization", token))
                        .andReturn().getResponse().getContentAsString()), "movieId");

        var body = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/recommendations").header("Authorization", token))
                        .andReturn().getResponse().getContentAsString());

        org.assertj.core.api.Assertions.assertThat(idsOf(body.get("aiPicks"), "id"))
                .as("trilha do motor")
                .doesNotContainAnyElementsOf(rated);
        org.assertj.core.api.Assertions.assertThat(idsOf(body.get("byGenre"), "id"))
                .as("trilha por gênero")
                .doesNotContainAnyElementsOf(rated);
    }

    @Test
    @DisplayName("recomendações exigem autenticação")
    void recommendationsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations"))
                .andExpect(status().isUnauthorized());
    }
}
