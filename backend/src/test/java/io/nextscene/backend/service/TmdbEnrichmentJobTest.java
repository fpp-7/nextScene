package io.nextscene.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre a escolha do trailer entre os vídeos que o TMDB devolve — a parte
 * pura de {@link TmdbEnrichmentJob}, sem precisar de rede nem de banco.
 */
class TmdbEnrichmentJobTest {

    private final TmdbEnrichmentJob job = new TmdbEnrichmentJob(null, null, null);

    @Test
    @DisplayName("prefere o trailer oficial entre vários candidatos")
    void prefersOfficialTrailer() {
        var response = responseWithVideos(
                video("abc123", "YouTube", "Trailer", false),
                video("def456", "YouTube", "Trailer", true)
        );

        assertThat(job.extractTrailerKey(response)).isEqualTo("def456");
    }

    @Test
    @DisplayName("usa o não-oficial quando é o único disponível")
    void fallsBackToUnofficialTrailer() {
        var response = responseWithVideos(video("abc123", "YouTube", "Trailer", false));

        assertThat(job.extractTrailerKey(response)).isEqualTo("abc123");
    }

    @Test
    @DisplayName("ignora vídeos que não são trailer do YouTube")
    void ignoresNonTrailerVideos() {
        var response = responseWithVideos(
                video("teaser1", "YouTube", "Teaser", true),
                video("clip1", "Vimeo", "Trailer", true)
        );

        assertThat(job.extractTrailerKey(response)).isNull();
    }

    @Test
    @DisplayName("sem vídeos, devolve null")
    void nullWhenNoVideos() {
        assertThat(job.extractTrailerKey(Map.of())).isNull();
    }

    @Test
    @DisplayName("results ausente dentro de videos não quebra")
    void nullWhenResultsMissing() {
        assertThat(job.extractTrailerKey(Map.of("videos", Map.of()))).isNull();
    }

    private Map<String, Object> responseWithVideos(Map<String, Object>... videos) {
        return Map.of("videos", Map.of("results", List.of(videos)));
    }

    private Map<String, Object> video(String key, String site, String type, boolean official) {
        return Map.of("key", key, "site", site, "type", type, "official", official);
    }
}
