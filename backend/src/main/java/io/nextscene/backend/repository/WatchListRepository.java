package io.nextscene.backend.repository;

import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.Movie;
import io.nextscene.backend.model.WatchList;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchListRepository extends JpaRepository<WatchList, UUID> {

    /** Carrega o filme junto para evitar N+1 ao montar a resposta. */
    @EntityGraph(attributePaths = "movie")
    List<WatchList> findByUser(AppUser user);

    Optional<WatchList> findByUserAndMovie(AppUser user, Movie movie);

    boolean existsByUserAndMovie(AppUser user, Movie movie);

    @Transactional
    void deleteByUserAndMovie(AppUser user, Movie movie);

    long countByUser(AppUser user);
}
