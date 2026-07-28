package io.nextscene.backend.dto;

import io.nextscene.backend.model.Movie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O catálogo do MovieLens só tem títulos em inglês; o título em português vem do
 * TMDB e pode não existir para todo filme. A resposta precisa preferir o
 * traduzido sem nunca ficar sem título.
 */
class MovieResponseTitleTest {

    private Movie movie(String title, String titlePt) {
        var movie = new Movie();
        movie.setMovieId(1);
        movie.setTitle(title);
        movie.setTitlePt(titlePt);
        return movie;
    }

    @Test
    @DisplayName("usa o título em português quando existe")
    void prefersLocalizedTitle() {
        var response = MovieResponse.from(movie("The Godfather", "O Poderoso Chefão"));

        assertThat(response.title()).isEqualTo("O Poderoso Chefão");
    }

    @Test
    @DisplayName("cai para o original quando não há tradução")
    void fallsBackToOriginal() {
        var response = MovieResponse.from(movie("Shawshank Redemption, The", null));

        assertThat(response.title()).isEqualTo("Shawshank Redemption, The");
    }

    @Test
    @DisplayName("trata tradução em branco como ausente")
    void treatsBlankAsMissing() {
        assertThat(MovieResponse.from(movie("Heat", "")).title()).isEqualTo("Heat");
        assertThat(MovieResponse.from(movie("Heat", "   ")).title()).isEqualTo("Heat");
    }

    @Test
    @DisplayName("o título nunca vem vazio")
    void neverReturnsEmptyTitle() {
        for (String localized : new String[]{null, "", "  ", "Fogo Contra Fogo"}) {
            assertThat(MovieResponse.from(movie("Heat", localized)).title()).isNotBlank();
        }
    }
}
