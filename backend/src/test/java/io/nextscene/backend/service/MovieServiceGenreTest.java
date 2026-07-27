package io.nextscene.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O app exibe gêneros em português; o catálogo MovieLens usa nomes em inglês.
 * Quando a tradução falha, o filtro devolve lista vazia sem nenhum erro visível
 * — foi assim que "Historia" ficou quebrado sem ninguém perceber.
 */
class MovieServiceGenreTest {

    @ParameterizedTest
    @CsvSource({
            "Acao,               Action",
            "Ficcao Cientifica,  Sci-Fi",
            "Suspense,           Thriller",
            "Terror,             Horror",
            "Horror,             Horror",
            "Infantil,           Children",
            "Noir,               Film-Noir",
            "Guerra,             War",
            "Documentario,       Documentary",
    })
    @DisplayName("traduz os gêneros da interface para o vocabulário do catálogo")
    void translatesUiGenres(String input, String expected) {
        assertThat(MovieService.translateGenre(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("ignora diferenças de caixa e espaços em volta")
    void isCaseAndWhitespaceInsensitive() {
        assertThat(MovieService.translateGenre("  ACAO ")).isEqualTo("Action");
        assertThat(MovieService.translateGenre("aCaO")).isEqualTo("Action");
    }

    @Test
    @DisplayName("repassa sem alteração o gênero que já está no vocabulário do catálogo")
    void passesThroughUnknownGenres() {
        assertThat(MovieService.translateGenre("IMAX")).isEqualTo("IMAX");
        assertThat(MovieService.translateGenre("Adventure")).isEqualTo("Adventure");
    }

    @Test
    @DisplayName("não quebra com entrada nula")
    void handlesNull() {
        assertThat(MovieService.translateGenre(null)).isEmpty();
    }

    /**
     * Trava o acoplamento entre a lista de chips do app (frontend/src/data/genres.ts)
     * e o mapa de tradução: um gênero na tela sem tradução aqui vira um filtro que
     * nunca retorna nada.
     */
    @Test
    @DisplayName("todo gênero oferecido na interface existe no catálogo MovieLens")
    void everyGenreOfferedInTheAppExistsInTheCatalog() {
        // Vocabulário completo de gêneros do MovieLens.
        var catalogGenres = java.util.Set.of(
                "Action", "Adventure", "Animation", "Children", "Comedy", "Crime",
                "Documentary", "Drama", "Fantasy", "Film-Noir", "Horror", "IMAX",
                "Musical", "Mystery", "Romance", "Sci-Fi", "Thriller", "War", "Western"
        );

        // Espelha frontend/src/data/genres.ts.
        String[] genresShownInTheApp = {
                "Acao", "Aventura", "Animacao", "Comedia", "Crime", "Documentario",
                "Drama", "Fantasia", "Ficcao Cientifica", "Guerra", "Infantil",
                "Misterio", "Musical", "Noir", "Romance", "Suspense", "Terror", "Western"
        };

        for (String genre : genresShownInTheApp) {
            assertThat(MovieService.translateGenre(genre))
                    .as("o chip '%s' precisa mapear para um gênero que existe no catálogo, "
                            + "senão o filtro sempre volta vazio", genre)
                    .isIn(catalogGenres);
        }
    }

    @Test
    @DisplayName("'Historia' não existe no MovieLens e por isso não é mais oferecido")
    void historiaWasRemovedBecauseItHasNoCounterpart() {
        // Regressão: o chip existia na interface e traduzia para "History", gênero
        // que o catálogo não possui — clicar nele nunca retornava nenhum filme.
        assertThat(MovieService.translateGenre("historia")).isEqualTo("historia");
    }
}
