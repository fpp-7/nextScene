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
     * Usado pelo job de enriquecimento em background.
     * <p>
     * A ordem espelha a das prateleiras do aplicativo. Sem isso o job percorria
     * o catálogo em ordem arbitrária, e os filmes que o usuário realmente vê só
     * ganhavam pôster, sinopse e título em português depois de horas.
     * <p>
     * O desempate por ano existe para a prateleira "Mais Recentes": filmes ainda
     * não enriquecidos têm nota 0 e ficariam todos no fim da fila, deixando
     * aquela faixa sem pôster por muito tempo. Ordenando o grupo de nota 0 do
     * mais novo para o mais antigo, ela se preenche junto com as demais.
     */
    @Query("""
            SELECT m FROM Movie m
            WHERE m.tmdbId IS NOT NULL AND m.enrichedAt IS NULL
            ORDER BY m.rating DESC NULLS LAST, m.year DESC NULLS LAST, m.movieId ASC
            """)
    List<Movie> findPendingEnrichment(Pageable pageable);

    @Query("SELECT COUNT(m) FROM Movie m WHERE m.tmdbId IS NOT NULL AND m.enrichedAt IS NULL")
    long countPendingEnrichment();

    /**
     * Busca por título usando similaridade por trigrama, apoiada nos índices GIN
     * das migrations V5 e V6. Bem mais rápida que LIKE '%termo%' em varredura.
     * <p>
     * Considera o título original e o traduzido, ignorando acentos: "The
     * Godfather", "O Poderoso Chefão" e "Poderoso Chefao" chegam ao mesmo filme.
     */
    @Query(value = """
            SELECT * FROM movie
            WHERE immutable_unaccent(title) ILIKE '%' || immutable_unaccent(:term) || '%'
               OR immutable_unaccent(COALESCE(title_pt, '')) ILIKE '%' || immutable_unaccent(:term) || '%'
            ORDER BY GREATEST(
                         similarity(immutable_unaccent(title), immutable_unaccent(:term)),
                         similarity(immutable_unaccent(COALESCE(title_pt, '')), immutable_unaccent(:term))
                     ) DESC,
                     rating DESC NULLS LAST
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Movie> searchByTitle(@Param("term") String term,
                              @Param("limit") int limit,
                              @Param("offset") int offset);
}
