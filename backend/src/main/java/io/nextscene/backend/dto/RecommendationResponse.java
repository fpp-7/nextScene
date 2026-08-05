package io.nextscene.backend.dto;

import java.util.List;

/**
 * As duas trilhas da tela "Para Você".
 *
 * @param aiPicks    sugestões do motor de recomendação
 * @param byGenre    destaques nos gêneros que o usuário marcou como preferidos
 *
 * <p>O segundo campo se chamava {@code similarUsers}. O nome vinha de uma
 * intenção original — "o que usuários parecidos assistiram" — que nunca chegou a
 * ser implementada: primeiro a trilha era a mesma lista do motor cortada ao
 * meio, e depois passou a ser curadoria por gênero. A interface já exibia o
 * rótulo correto; o contrato da API é que continuava descrevendo outra coisa.
 */
public record RecommendationResponse(
        List<MovieResponse> aiPicks,
        List<MovieResponse> byGenre
) {}
