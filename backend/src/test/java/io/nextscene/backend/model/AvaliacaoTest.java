package io.nextscene.backend.model;

import io.nextscene.backend.model.enums.Avaliacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A escala 0–5 precisa bater com os limiares do motor
 * (LIKED_THRESHOLD = 3.5 e DISLIKED_THRESHOLD = 1.5 em hybrid.py).
 * Se estes valores mudarem sem que os limiares mudem junto, o motor passa a
 * classificar as avaliações de forma errada — silenciosamente.
 */
class AvaliacaoTest {

    private static final double LIKED_THRESHOLD = 3.5;
    private static final double DISLIKED_THRESHOLD = 1.5;

    @Test
    @DisplayName("LIKE fica acima do limiar de curtida do motor")
    void likeIsAboveLikedThreshold() {
        assertThat(Avaliacao.LIKE.toRatingScale()).isGreaterThanOrEqualTo(LIKED_THRESHOLD);
    }

    @Test
    @DisplayName("DISLIKE fica abaixo do limiar de rejeição do motor")
    void dislikeIsBelowDislikedThreshold() {
        assertThat(Avaliacao.DISLIKE.toRatingScale()).isLessThanOrEqualTo(DISLIKED_THRESHOLD);
    }

    @Test
    @DisplayName("SEEN é neutro: não entra no perfil nem na penalização")
    void seenIsNeutral() {
        double seen = Avaliacao.SEEN.toRatingScale();

        assertThat(seen).isLessThan(LIKED_THRESHOLD);
        assertThat(seen).isGreaterThan(DISLIKED_THRESHOLD);
    }

    @Test
    @DisplayName("todos os valores ficam dentro da escala aceita pelo motor")
    void allValuesAreWithinScale() {
        for (Avaliacao avaliacao : Avaliacao.values()) {
            assertThat(avaliacao.toRatingScale())
                    .as("%s deve ficar entre 0 e 5", avaliacao)
                    .isBetween(0.0, 5.0);
        }
    }
}
