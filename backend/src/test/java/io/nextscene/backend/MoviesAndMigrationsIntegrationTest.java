package io.nextscene.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catálogo e migrations. Roda contra um Postgres real, então valida coisas que
 * um banco em memória não validaria: a extensão pg_trgm, os índices GIN e o
 * efeito da V4 sobre os filmes-mock do seed.
 */
class MoviesAndMigrationsIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ─── Migrations ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("V5 instala a extensão pg_trgm")
    void trigramExtensionIsInstalled() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'pg_trgm'", Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("V5 cria os índices que sustentam busca e filtro")
    void searchIndexesExist() {
        var indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'movie'", String.class);

        assertThat(indexes)
                .contains("idx_movie_title_unaccent_trgm", "idx_movie_title_pt_unaccent_trgm",
                        "idx_movie_genres_trgm", "idx_movie_rating")
                // O B-tree em genres era inútil para LIKE '%...%' e foi removido.
                .doesNotContain("idx_movie_genres")
                // Substituídos pelas versões sem acento na V7.
                .doesNotContain("idx_movie_title_trgm", "idx_movie_title_pt_trgm");
    }

    @Test
    @DisplayName("V4 remove os filmes-mock do seed V2")
    void mockSeedMoviesAreGone() {
        Integer unsplash = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM movie WHERE poster_url LIKE '%unsplash%'", Integer.class);

        assertThat(unsplash).isZero();
    }

    @Test
    @DisplayName("o importador carrega o catálogo MovieLens sem apagar nada")
    void catalogWasImported() {
        Integer movies = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movie", Integer.class);

        assertThat(movies).isGreaterThan(9_000);
    }

    @Test
    @DisplayName("movie_id continua único depois da importação")
    void movieIdsAreUnique() {
        Integer duplicates = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT movie_id FROM movie GROUP BY movie_id HAVING COUNT(*) > 1
                ) AS d
                """, Integer.class);

        assertThat(duplicates).isZero();
    }

    // ─── Catálogo ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("a paginação devolve páginas diferentes")
    void paginationReturnsDistinctPages() throws Exception {
        String first = mockMvc.perform(get("/api/movies?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get("/api/movies?page=1&size=5"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Regressão: antes o tamanho era fixo em 30 e não havia como avançar —
        // o resto do catálogo era inalcançável pelo app.
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("o tamanho de página é limitado")
    void pageSizeIsCapped() throws Exception {
        mockMvc.perform(get("/api/movies?page=0&size=5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.lessThanOrEqualTo(50)));
    }

    @Test
    @DisplayName("a busca por título encontra o filme")
    void searchFindsByTitle() throws Exception {
        mockMvc.perform(get("/api/movies/search?q=Toy Story"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$[0].title").value(
                        org.hamcrest.Matchers.containsStringIgnoringCase("toy story")));
    }

    @Test
    @DisplayName("a busca ignora a caixa das letras")
    void searchIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/movies/search?q=jUmAnJi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("V6 cria a coluna e o índice do título traduzido")
    void localizedTitleColumnExists() {
        var columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'movie'",
                String.class);
        assertThat(columns).contains("title_pt");

        var indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'movie'", String.class);
        assertThat(indexes).contains("idx_movie_title_pt_unaccent_trgm");
    }

    @Test
    @DisplayName("V7 instala a extensão unaccent")
    void unaccentExtensionIsInstalled() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'unaccent'", Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("a busca encontra pelo título traduzido e pelo original")
    void searchMatchesBothTitles() throws Exception {
        // O job do TMDB é quem preenche title_pt em produção; aqui o valor é
        // gravado direto para o teste não depender de rede.
        jdbcTemplate.update(
                "UPDATE movie SET title_pt = 'O Poderoso Chefão' WHERE movie_id = 858");

        mockMvc.perform(get("/api/movies/search?q=Poderoso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("O Poderoso Chefão"));

        // Buscar pelo nome em inglês continua funcionando.
        mockMvc.perform(get("/api/movies/search?q=Godfather"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("a busca ignora acentos nos dois sentidos")
    void searchIgnoresAccents() throws Exception {
        jdbcTemplate.update(
                "UPDATE movie SET title_pt = 'O Poderoso Chefão' WHERE movie_id = 858");

        // Sem acento encontra o título acentuado — digitar "Chefao" é comum.
        mockMvc.perform(get("/api/movies/search?q=Poderoso Chefao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("O Poderoso Chefão"));

        // E com acento também.
        mockMvc.perform(get("/api/movies/search?q=Chefão"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("O Poderoso Chefão"));
    }

    @Test
    @DisplayName("a busca com caracteres especiais não quebra")
    void searchHandlesSpecialCharacters() throws Exception {
        // A consulta é nativa e parametrizada; entrada estranha não deve virar erro.
        mockMvc.perform(get("/api/movies/search?q=%25%25%25")).andExpect(status().isOk());
        mockMvc.perform(get("/api/movies/search?q=' OR 1=1--")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("o filtro por gênero traduz do português para o catálogo")
    void genreFilterTranslates() throws Exception {
        mockMvc.perform(get("/api/movies?genre=Acao&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$[0].genre").value(
                        org.hamcrest.Matchers.containsString("Action")));
    }

    @Test
    @DisplayName("filme inexistente devolve 404, não 400")
    void unknownMovieIsNotFound() throws Exception {
        mockMvc.perform(get("/api/movies/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("o filme em destaque é público e íntegro")
    void featuredMovieIsPublic() throws Exception {
        mockMvc.perform(get("/api/movies/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.id").isNumber());
    }
}
