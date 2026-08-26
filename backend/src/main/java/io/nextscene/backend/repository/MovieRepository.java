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

    /**
     * As leituras do catálogo filtram por {@code displayable} (ver V12): filmes
     * sem tradução pt-BR ou sem elenco no TMDB não têm o que mostrar num card.
     * <p>
     * {@link #findByMovieId} é a exceção deliberada — um filme já na watchlist
     * ou já avaliado precisa continuar abrindo por link direto, mesmo que tenha
     * saído das listas.
     */
    List<Movie> findByGenresContainingIgnoreCaseAndDisplayableTrue(String genre, Pageable pageable);

    List<Movie> findByDisplayableTrue(Pageable pageable);

    List<Movie> findByMovieIdInAndDisplayableTrue(Collection<Integer> movieIds);

    Optional<Movie> findTopByDisplayableTrueOrderByRatingDesc();

    /**
     * Melhor nota <b>entre os filmes que muita gente avaliou</b>.
     * <p>
     * Sem o piso, o destaque era literalmente o maior {@code rating} do
     * catálogo — e nota média mede qualidade percebida, não alcance. O topo
     * ficava com compilações e obscuridades de 174 votos, enquanto clássicos
     * com 30 mil votos e nota parecida nunca apareciam. É o mesmo raciocínio
     * que separou {@code SortBy.POPULAR} de {@code SortBy.RATING}.
     */
    @Query("""
            SELECT m FROM Movie m
            WHERE m.displayable = TRUE AND m.voteCount >= :minVotes
            ORDER BY m.rating DESC NULLS LAST
            """)
    List<Movie> findFeaturedCandidates(@Param("minVotes") int minVotes, Pageable pageable);

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
            WHERE displayable
              AND (immutable_unaccent(title) ILIKE '%' || immutable_unaccent(:term) || '%'
                OR immutable_unaccent(COALESCE(title_pt, '')) ILIKE '%' || immutable_unaccent(:term) || '%')
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
