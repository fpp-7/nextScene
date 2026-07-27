package io.nextscene.backend.model.enums;

public enum Avaliacao {
    LIKE,
    DISLIKE,
    SEEN;

    /**
     * Converte a avaliação para a escala 0–5 usada pelo motor de recomendação
     * (a mesma do MovieLens, em que o modelo foi treinado).
     */
    public double toRatingScale() {
        return switch (this) {
            case LIKE -> 5.0;
            case SEEN -> 2.5;
            case DISLIKE -> 0.0;
        };
    }
}
