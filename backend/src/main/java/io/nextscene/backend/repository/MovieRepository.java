package io.nextscene.backend.repository;

import io.nextscene.backend.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<Movie, UUID> {

    Optional<Movie> findByMovieId(Integer movieId);

    List<Movie> findByGenresContainingIgnoreCase(String genre, org.springframework.data.domain.Pageable pageable);

    List<Movie> findByTitleContainingIgnoreCase(String title, org.springframework.data.domain.Pageable pageable);

    Optional<Movie> findTopByOrderByRatingDesc();
}
