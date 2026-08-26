package io.nextscene.backend;

import io.nextscene.backend.infra.MovieCacheEvictor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O cache do catálogo era populado e nunca invalidado: o job de enriquecimento
 * gravava pôster, sinopse e título em português, e a API continuava servindo a
 * versão anterior até o TTL de 10 minutos vencer.
 * <p>
 * O teste roda contra o Redis de verdade e passa pelo proxy do Spring — é a
 * armadilha real aqui, porque {@code @CacheEvict} chamado de dentro da própria
 * classe é ignorado em silêncio.
 */
class MovieCacheIntegrationTest extends IntegrationTestBase {

    private static final int MOVIE_ID = 90_000_101;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MovieCacheEvictor cacheEvictor;

    @Test
    @DisplayName("invalidar o cache faz a API enxergar o filme já enriquecido")
    void evictionMakesFreshDataVisible() throws Exception {
        jdbcTemplate.update("DELETE FROM movie WHERE movie_id = ?", MOVIE_ID);
        jdbcTemplate.update("""
                INSERT INTO movie (movie_id, title, genres, year, synopsis, cast_list,
                                   rating, vote_count, displayable, enriched_at)
                VALUES (?, 'Antes Do Enriquecimento', 'Drama', 1999, 'Sinopse.',
                        'Fulano', 7.0, 100, TRUE, now())
                """, MOVIE_ID);
        cacheEvictor.evictCatalog();

        // Popula o cache com o estado antigo.
        mockMvc.perform(get("/api/v1/movies/" + MOVIE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Antes Do Enriquecimento"));

        // O job grava o título em português direto no banco, sem passar pela API.
        jdbcTemplate.update("UPDATE movie SET title_pt = ? WHERE movie_id = ?",
                "Depois Do Enriquecimento", MOVIE_ID);

        // Sem invalidação, esta leitura devolveria o título antigo — era o bug.
        mockMvc.perform(get("/api/v1/movies/" + MOVIE_ID))
                .andExpect(jsonPath("$.title").value("Antes Do Enriquecimento"));

        cacheEvictor.evictCatalog();

        mockMvc.perform(get("/api/v1/movies/" + MOVIE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Depois Do Enriquecimento"));
    }

}
