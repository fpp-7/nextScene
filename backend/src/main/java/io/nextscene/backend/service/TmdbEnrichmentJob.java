package io.nextscene.backend.service;

import io.nextscene.backend.infra.MovieCacheEvictor;
import io.nextscene.backend.model.Movie;
import io.nextscene.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Preenche pôster, sinopse, nota, elenco e trailer a partir do TMDB.
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
    private final MovieCacheEvictor cacheEvictor;

    @Value("${app.tmdb.api-key:}")
    private String tmdbApiKey;

    @Value("${app.tmdb.base-url:https://api.themoviedb.org/3}")
    private String tmdbBaseUrl;

    /** Filmes por execução. O TMDB limita ~50 req/s; ficamos bem abaixo disso. */
    @Value("${app.tmdb.batch-size:40}")
    private int batchSize;

    @Value("${app.tmdb.enabled:true}")
    private boolean enabled;

    /**
     * Ciclos ignorados depois de encontrar a fila vazia.
     * <p>
     * O intervalo curto existe para drenar a carga inicial do catálogo. Depois
     * disso, consultar o banco a cada 20 segundos para não achar nada é ruído.
     * Com 90 ciclos, a verificação passa a ocorrer a cada ~30 minutos.
     */
    private static final int IDLE_BACKOFF_CYCLES = 90;

    private int idleCycles = 0;

    @Scheduled(
            initialDelayString = "${app.tmdb.initial-delay-ms:30000}",
            fixedDelayString = "${app.tmdb.interval-ms:60000}"
    )
    public void enrichPendingMovies() {
        if (!enabled || tmdbApiKey == null || tmdbApiKey.isBlank()) {
            return;
        }

        // Com o catálogo já processado, não há por que consultar o banco a cada
        // ciclo curto. O intervalo é dimensionado para a carga inicial; depois
        // dela, só interessa capturar filmes novos, o que é raro.
        if (idleCycles > 0) {
            idleCycles--;
            return;
        }

        List<Movie> pending = movieRepository.findPendingEnrichment(PageRequest.of(0, batchSize));
        if (pending.isEmpty()) {
            idleCycles = IDLE_BACKOFF_CYCLES;
            return;
        }

        int ok = 0;
        for (Movie movie : pending) {
            if (enrich(movie)) ok++;
        }

        // Uma invalidação por lote, não por filme: os caches são invalidados
        // por inteiro, então repetir a cada um dos 40 filmes só multiplicaria
        // idas ao Redis para chegar ao mesmo estado.
        if (ok > 0) {
            cacheEvictor.evictCatalog();
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
            // include_video_language cobre o trailer original quando não há um
            // dublado/legendado em pt-BR — sem isso, a maioria dos filmes fica
            // sem vídeo, porque o TMDB não filtra por padrão pelo idioma da
            // consulta.
            String url = String.format(
                    "%s/movie/%d?append_to_response=credits,videos&api_key=%s"
                            + "&language=pt-BR&include_video_language=pt-BR,en,null",
                    tmdbBaseUrl, movie.getTmdbId(), tmdbApiKey);

            var response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                return false;
            }

            // A consulta usa language=pt-BR, então "title" já vem traduzido.
            // O título original é preservado: o motor de recomendação trabalha
            // com ele, e a busca continua aceitando o nome em inglês.
            String localizedTitle = (String) response.get("title");
            if (localizedTitle != null && !localizedTitle.isBlank()) {
                movie.setTitlePt(localizedTitle);
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

            Number voteCount = (Number) response.get("vote_count");
            if (voteCount != null) {
                movie.setVoteCount(voteCount.intValue());
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

            movie.setTrailerKey(extractTrailerKey(response));

            // A consulta pediu language=pt-BR. O TMDB não erra quando não há
            // tradução: ele devolve `title` no idioma original e `overview`
            // vazio. A sinopse em branco é, portanto, o sinal confiável de
            // "não existe em português" — e sem elenco o card fica com avatares
            // vazios. Nos dois casos não há o que mostrar; ver V12.
            movie.setDisplayable(hasText(movie.getSynopsis()) && hasText(movie.getCastList()));

            movie.setEnrichedAt(Instant.now());
            movieRepository.save(movie);
            return true;

        } catch (HttpClientErrorException.NotFound e) {
            // O TMDB não conhece este id: o filme foi removido, fundido com
            // outro, ou o link do MovieLens está desatualizado. Não adianta
            // tentar de novo — marcamos como processado para que saia da fila.
            //
            // Sem isto o filme falharia para sempre e, quando só restassem
            // casos assim, o job giraria em falso a cada ciclo consumindo
            // requisições à toa.
            log.debug("TMDB não conhece '{}' (tmdb_id={}) — marcado como resolvido.",
                    movie.getTitle(), movie.getTmdbId());
            // Sem metadados não há card possível: o TMDB é a única fonte de
            // pôster, sinopse e elenco, e este filme não existe lá.
            movie.setDisplayable(false);
            movie.setEnrichedAt(Instant.now());
            movieRepository.save(movie);
            return false;

        } catch (Exception e) {
            // Falha transitória (rede, 429, 5xx): deixa pendente para a próxima
            // rodada.
            log.debug("TMDB indisponível para '{}' (tmdb_id={}): {}",
                    movie.getTitle(), movie.getTmdbId(), e.getMessage());
            return false;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Escolhe o trailer do YouTube entre os vídeos do filme, preferindo o
     * oficial. Null quando não há nenhum — o app esconde o botão de play nesse
     * caso, em vez de cair numa busca.
     * <p>
     * Visibilidade de pacote de propósito: é lógica pura, testada diretamente
     * em {@code TmdbEnrichmentJobTest} sem precisar montar o resto da classe.
     */
    @SuppressWarnings("unchecked")
    String extractTrailerKey(Map<String, Object> response) {
        Map<String, Object> videos = (Map<String, Object>) response.get("videos");
        if (videos == null) {
            return null;
        }

        var results = (List<Map<String, Object>>) videos.get("results");
        if (results == null) {
            return null;
        }

        return results.stream()
                .filter(v -> "YouTube".equals(v.get("site")) && "Trailer".equals(v.get("type")))
                .sorted(Comparator.comparing(v -> !Boolean.TRUE.equals(v.get("official"))))
                .findFirst()
                .map(v -> (String) v.get("key"))
                .orElse(null);
    }
}
