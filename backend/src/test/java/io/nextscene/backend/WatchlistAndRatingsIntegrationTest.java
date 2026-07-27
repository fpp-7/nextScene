package io.nextscene.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre os dois endpoints que respondiam 500 por LazyInitializationException,
 * e o fluxo de avaliações do onboarding, que não persistia nada.
 */
class WatchlistAndRatingsIntegrationTest extends IntegrationTestBase {

    private static final int MOVIE_TOY_STORY = 1;
    private static final int MOVIE_JUMANJI = 2;
    private static final int MOVIE_HEAT = 6;

    private String bearer() throws Exception {
        return "Bearer " + registerAndGetToken(uniqueEmail());
    }

    // ─── Watchlist ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /watchlist devolve o filme completo, sem LazyInitializationException")
    void watchlistReturnsMoviePayload() throws Exception {
        String token = bearer();

        mockMvc.perform(post("/api/watchlist/" + MOVIE_TOY_STORY).header("Authorization", token))
                .andExpect(status().isCreated());

        // Regressão: o serviço não era transacional e `open-in-view` está desligado,
        // então acessar wl.getMovie() fora da transação estourava 500.
        mockMvc.perform(get("/api/watchlist").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movieId").value(MOVIE_TOY_STORY))
                .andExpect(jsonPath("$[0].movie.title").isNotEmpty())
                .andExpect(jsonPath("$[0].id").isString());
    }

    @Test
    @DisplayName("adicionar duas vezes não duplica o item")
    void addingTwiceIsIdempotent() throws Exception {
        String token = bearer();

        mockMvc.perform(post("/api/watchlist/" + MOVIE_JUMANJI).header("Authorization", token));
        mockMvc.perform(post("/api/watchlist/" + MOVIE_JUMANJI).header("Authorization", token));

        mockMvc.perform(get("/api/watchlist").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("remover tira o item da lista")
    void removeDropsTheItem() throws Exception {
        String token = bearer();
        mockMvc.perform(post("/api/watchlist/" + MOVIE_HEAT).header("Authorization", token));

        mockMvc.perform(delete("/api/watchlist/" + MOVIE_HEAT).header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/watchlist").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("a watchlist de um usuário não vaza para outro")
    void watchlistIsScopedToTheUser() throws Exception {
        String alice = bearer();
        String bob = bearer();

        mockMvc.perform(post("/api/watchlist/" + MOVIE_TOY_STORY).header("Authorization", alice));

        mockMvc.perform(get("/api/watchlist").header("Authorization", bob))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── Avaliações ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /ratings/me devolve as avaliações, sem LazyInitializationException")
    void myRatingsReturnsPayload() throws Exception {
        String token = bearer();
        var body = objectMapper.writeValueAsString(Map.of("movieId", MOVIE_TOY_STORY, "type", "like"));

        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/ratings/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movieId").value(MOVIE_TOY_STORY))
                .andExpect(jsonPath("$[0].type").value("like"))
                .andExpect(jsonPath("$[0].id").isString());
    }

    @Test
    @DisplayName("o lote do onboarding persiste todas as avaliações")
    void batchPersistsEveryRating() throws Exception {
        String token = bearer();
        var body = objectMapper.writeValueAsString(List.of(
                Map.of("movieId", MOVIE_TOY_STORY, "type", "like"),
                Map.of("movieId", MOVIE_JUMANJI, "type", "dislike"),
                Map.of("movieId", MOVIE_HEAT, "type", "seen")
        ));

        // Regressão: o onboarding só chamava /recommendations/cold-start, que não
        // grava nada — o histórico do usuário nascia vazio.
        mockMvc.perform(post("/api/ratings/batch")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/ratings/me").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/users/me").header("Authorization", token))
                .andExpect(jsonPath("$.interactionCount").value(3));
    }

    @Test
    @DisplayName("reavaliar o mesmo filme atualiza em vez de duplicar")
    void reRatingUpdatesInsteadOfDuplicating() throws Exception {
        String token = bearer();

        for (String type : List.of("like", "dislike")) {
            var body = objectMapper.writeValueAsString(Map.of("movieId", MOVIE_JUMANJI, "type", type));
            mockMvc.perform(post("/api/ratings")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }

        mockMvc.perform(get("/api/ratings/me").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("dislike"));

        // O contador mede interações distintas: reavaliar não pode inflá-lo.
        mockMvc.perform(get("/api/users/me").header("Authorization", token))
                .andExpect(jsonPath("$.interactionCount").value(1));
    }

    @Test
    @DisplayName("tipo de avaliação inválido devolve 400 com mensagem útil")
    void invalidRatingTypeIsRejected() throws Exception {
        String token = bearer();
        var body = objectMapper.writeValueAsString(Map.of("movieId", MOVIE_TOY_STORY, "type", "adorei"));

        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("adorei")));
    }

    @Test
    @DisplayName("avaliar filme inexistente devolve 404, não 400")
    void ratingUnknownMovieIsNotFound() throws Exception {
        String token = bearer();
        var body = objectMapper.writeValueAsString(Map.of("movieId", 99_999_999, "type", "like"));

        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("as estatísticas do perfil refletem as avaliações")
    void statsReflectRatings() throws Exception {
        String token = bearer();
        var body = objectMapper.writeValueAsString(List.of(
                Map.of("movieId", MOVIE_TOY_STORY, "type", "like"),
                Map.of("movieId", MOVIE_JUMANJI, "type", "like"),
                Map.of("movieId", MOVIE_HEAT, "type", "seen")
        ));
        mockMvc.perform(post("/api/ratings/batch")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body));

        mockMvc.perform(get("/api/users/me/stats").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rated").value(3))
                .andExpect(jsonPath("$.favorites").value(2))
                .andExpect(jsonPath("$.watched").value(1));
    }
}
