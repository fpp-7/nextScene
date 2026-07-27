package io.nextscene.backend.repository;

import io.nextscene.backend.model.Movie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<Movie, UUID> {

    Optional<Movie> findByMovieId(Integer movieId);

    /** Busca em lote — evita uma consulta por filme ao montar recomendações. */
    List<Movie> findByMovieIdIn(Collection<Integer> movieIds);

    List<Movie> findByGenresContainingIgnoreCase(String genre, Pageable pageable);

    List<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Optional<Movie> findTopByOrderByRatingDesc();

    /**
     * Filmes que ainda não passaram pelo TMDB e têm tmdb_id conhecido.
     * Usado pelo job de enriquecimento em background (casa com o índice parcial
     * idx_movie_pending_enrichment).
     */
    @Query("SELECT m FROM Movie m WHERE m.tmdbId IS NOT NULL AND m.enrichedAt IS NULL")
    List<Movie> findPendingEnrichment(Pageable pageable);

    @Query("SELECT COUNT(m) FROM Movie m WHERE m.tmdbId IS NOT NULL AND m.enrichedAt IS NULL")
    long countPendingEnrichment();

    /**
     * Busca por título usando similaridade por trigrama, apoiada no índice GIN
     * criado na migration V5. Bem mais rápida que LIKE '%termo%' em varredura.
     */
    @Query(value = """
            SELECT * FROM movie
            WHERE title ILIKE '%' || :term || '%'
            ORDER BY similarity(title, :term) DESC, rating DESC NULLS LAST
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Movie> searchByTitle(@Param("term") String term,
                              @Param("limit") int limit,
                              @Param("offset") int offset);
}
