package io.nextscene.backend.service;

import io.nextscene.backend.model.Movie;
import io.nextscene.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Preenche pôster, sinopse, nota e elenco a partir do TMDB.
 * <p>
 * Antes isso acontecia dentro das requisições dos usuários: cada tela de
 * recomendação disparava até 20 chamadas HTTP sequenciais ao TMDB, somando
 * dezenas de segundos de latência e estourando o rate limit. Agora roda em
 * background, em lotes, e as telas apenas leem o que já está no banco.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbEnrichmentJob {

    private final MovieRepository movieRepository;
    private final RestTemplate restTemplate;

    @Value("${app.tmdb.api-key:}")
    private String tmdbApiKey;

    @Value("${app.tmdb.base-url:https://api.themoviedb.org/3}")
    private String tmdbBaseUrl;

    /** Filmes por execução. O TMDB limita ~50 req/s; ficamos bem abaixo disso. */
    @Value("${app.tmdb.batch-size:40}")
    private int batchSize;

    @Value("${app.tmdb.enabled:true}")
    private boolean enabled;

    @Scheduled(
            initialDelayString = "${app.tmdb.initial-delay-ms:30000}",
            fixedDelayString = "${app.tmdb.interval-ms:60000}"
    )
    public void enrichPendingMovies() {
        if (!enabled || tmdbApiKey == null || tmdbApiKey.isBlank()) {
            return;
        }

        List<Movie> pending = movieRepository.findPendingEnrichment(PageRequest.of(0, batchSize));
        if (pending.isEmpty()) {
            return;
        }

        int ok = 0;
        for (Movie movie : pending) {
            if (enrich(movie)) ok++;
        }

        log.info("🎬 TMDB: {} de {} filmes enriquecidos neste lote ({} ainda pendentes).",
                ok, pending.size(), movieRepository.countPendingEnrichment());
    }

    /**
     * Enriquece um filme e marca {@code enrichedAt}. Falhas de rede deixam o
     * filme pendente para a próxima rodada.
     * <p>
     * Sem {@code @Transactional} de propósito: o método é chamado de dentro da
     * própria classe, então o proxy do Spring não seria aplicado de qualquer
     * forma. O {@code save()} do repositório já roda na sua própria transação,
     * e cada filme sendo independente é exatamente o comportamento desejado —
     * uma falha no filme 30 não desfaz os 29 anteriores.
     */
    @SuppressWarnings("unchecked")
    private boolean enrich(Movie movie) {
        try {
            String url = String.format(
                    "%s/movie/%d?append_to_response=credits&api_key=%s&language=pt-BR",
                    tmdbBaseUrl, movie.getTmdbId(), tmdbApiKey);

            var response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                return false;
            }

            String posterPath = (String) response.get("poster_path");
            if (posterPath != null && !posterPath.isBlank()) {
                movie.setPosterUrl("https://image.tmdb.org/t/p/w500" + posterPath);
            }

            String overview = (String) response.get("overview");
            if (overview != null && !overview.isBlank()) {
                movie.setSynopsis(overview);
            }

            Number voteAverage = (Number) response.get("vote_average");
            if (voteAverage != null) {
                movie.setRating(voteAverage.doubleValue());
            }

            Map<String, Object> credits = (Map<String, Object>) response.get("credits");
            if (credits != null) {
                var castList = (List<Map<String, Object>>) credits.get("cast");
                if (castList != null) {
                    movie.setCastList(castList.stream()
                            .limit(5)
                            .map(c -> (String) c.get("name"))
                            .reduce((a, b) -> a + "," + b)
                            .orElse(""));
                }
            }

            movie.setEnrichedAt(Instant.now());
            movieRepository.save(movie);
            return true;

        } catch (Exception e) {
            log.debug("TMDB indisponível para '{}' (tmdb_id={}): {}",
                    movie.getTitle(), movie.getTmdbId(), e.getMessage());
            return false;
        }
    }
}
