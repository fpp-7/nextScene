package io.nextscene.backend;

import io.nextscene.backend.infra.MovieCacheEvictor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filmes inexibíveis (V12): sem tradução pt-BR ou sem elenco no TMDB, o card
 * vira título estrangeiro, sinopse em branco e avatares vazios.
 * <p>
 * O par de filmes é montado aqui, não no seed, para que o teste controle
 * exatamente os dois lados da regra.
 */
class CatalogVisibilityIntegrationTest extends IntegrationTestBase {

    private static final int VISIBLE_ID = 90_000_001;
    private static final int HIDDEN_ID = 90_000_002;
    private static final String MARKER = "Zzqxplorable";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Os caches do catálogo são de verdade nos testes; sem limpar, a primeira
     *  leitura de uma classe anterior mascararia as inserções desta. */
    @Autowired
    private MovieCacheEvictor cacheEvictor;

    @BeforeEach
    void seedPair() {
        jdbcTemplate.update("DELETE FROM movie WHERE movie_id IN (?, ?)", VISIBLE_ID, HIDDEN_ID);

        // Nota altíssima nos dois: garante que, se o filtro falhasse, o filme
        // escondido apareceria no topo de /featured e das listas por nota — o
        // teste não passaria por acidente de ordenação.
        jdbcTemplate.update("""
                INSERT INTO movie (movie_id, title, genres, year, synopsis, cast_list,
                                   rating, vote_count, displayable, enriched_at)
                VALUES (?, ?, 'Drama', 1999, 'Tem sinopse.', 'Fulano,Beltrano',
                        10.0, 5000, TRUE, now())
                """, VISIBLE_ID, MARKER + " Visivel");

        jdbcTemplate.update("""
                INSERT INTO movie (movie_id, title, genres, year, synopsis, cast_list,
                                   rating, vote_count, displayable, enriched_at)
                VALUES (?, ?, 'Drama', 1999, '', '',
                        10.0, 5000, FALSE, now())
                """, HIDDEN_ID, MARKER + " Escondido");

        cacheEvictor.evictCatalog();
    }

    @Test
    @DisplayName("filme inexibível some da listagem por gênero")
    void hiddenMovieIsAbsentFromGenreListing() throws Exception {
        mockMvc.perform(get("/api/v1/movies").param("genre", "Drama").param("sort", "rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + VISIBLE_ID + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + HIDDEN_ID + ")]").doesNotExist());
    }

    @Test
    @DisplayName("filme inexibível some da busca por título")
    void hiddenMovieIsAbsentFromSearch() throws Exception {
        mockMvc.perform(get("/api/v1/movies/search").param("q", MARKER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + VISIBLE_ID + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + HIDDEN_ID + ")]").doesNotExist());
    }

    @Test
    @DisplayName("filme inexibível nunca é o destaque, mesmo com a maior nota")
    void hiddenMovieIsNeverFeatured() throws Exception {
        mockMvc.perform(get("/api/v1/movies/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(HIDDEN_ID)));
    }

    @Test
    @DisplayName("mas continua abrindo por link direto")
    void hiddenMovieIsStillReachableById() throws Exception {
        // Um filme que já está na watchlist ou já foi avaliado não pode virar
        // 404 só porque saiu das listas.
        mockMvc.perform(get("/api/v1/movies/" + HIDDEN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(HIDDEN_ID));
    }

    @Test
    @DisplayName("o destaque exige um piso de votos, não só a maior nota")
    void featuredRequiresPopularityFloor() throws Exception {
        // Uma obscuridade com nota 10 e 3 votos. Sem o piso, ela assumiria o
        // destaque na hora — foi o que colocou lá uma compilação de 174 votos.
        int obscureId = 90_000_003;
        jdbcTemplate.update("DELETE FROM movie WHERE movie_id = ?", obscureId);
        jdbcTemplate.update(
                "INSERT INTO movie (movie_id, title, genres, year, synopsis, cast_list, "
                        + "rating, vote_count, displayable, enriched_at) "
                        + "VALUES (?, 'Obscuridade Nota Dez', 'Drama', 1999, 'Tem sinopse.', "
                        + "'Fulano', 10.0, 3, TRUE, now())",
                obscureId);
        cacheEvictor.evictCatalog();

        try {
            mockMvc.perform(get("/api/v1/movies/featured"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(obscureId)));
        } finally {
            jdbcTemplate.update("DELETE FROM movie WHERE movie_id = ?", obscureId);
            cacheEvictor.evictCatalog();
        }
    }

    @Test
    @DisplayName("V12 marca como inexibível o que o enriquecimento já deixou vazio")
    void backfillHidWhatWasAlreadyEnrichedAndEmpty() {
        Integer leaked = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM movie
                 WHERE displayable
                   AND enriched_at IS NOT NULL
                   AND (synopsis IS NULL OR synopsis = ''
                     OR cast_list IS NULL OR cast_list = '')
                """, Integer.class);

        assertThat(leaked)
                .as("nenhum filme já enriquecido e vazio deveria ter sobrado visível")
                .isZero();
    }
}
