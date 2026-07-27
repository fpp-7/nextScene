package io.nextscene.backend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Resposta usada quando o motor de recomendação está fora do ar ou o circuito
 * está aberto.
 * <p>
 * Devolve uma lista vazia em vez de propagar a exceção: o
 * {@code RecommendationService} interpreta o vazio e cai no fallback local
 * (preferências de gênero / melhores avaliados). O usuário sempre recebe
 * alguma sugestão — nunca uma tela de erro por indisponibilidade do ML.
 */
@Slf4j
@Component
public class RecommendationEngineFallback implements FallbackFactory<RecommendationEngineClient> {

    private static final Map<String, Object> EMPTY_RESULT = Map.of("results", List.of());

    @Override
    public RecommendationEngineClient create(Throwable cause) {
        log.warn("Motor de recomendação indisponível — usando fallback local. Causa: {}",
                cause.toString());

        return new RecommendationEngineClient() {
            @Override
            public Map<String, Object> getRecommendationsFromHistory(Map<String, Object> request) {
                return EMPTY_RESULT;
            }

            @Override
            public Map<String, Object> getColdStartRecommendations(Map<String, Object> request) {
                return EMPTY_RESULT;
            }

            @Override
            public Object searchMovies(String query, int limit) {
                return List.of();
            }
        };
    }
}
