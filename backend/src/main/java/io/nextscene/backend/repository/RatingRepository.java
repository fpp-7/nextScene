package io.nextscene.backend.repository;

import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.Movie;
import io.nextscene.backend.model.Rating;
import io.nextscene.backend.model.enums.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    List<Rating> findByUser(AppUser user);

    Optional<Rating> findByUserAndMovie(AppUser user, Movie movie);

    long countByUser(AppUser user);

    long countByUserAndAvaliacao(AppUser user, Avaliacao avaliacao);
}
