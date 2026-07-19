package io.nextscene.backend.service;

import io.nextscene.backend.dto.RatingRequest;
import io.nextscene.backend.dto.RatingResponse;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.Movie;
import io.nextscene.backend.model.Rating;
import io.nextscene.backend.model.enums.Avaliacao;
import io.nextscene.backend.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserService userService;
    private final MovieService movieService;
    private final io.nextscene.backend.repository.AppUserRepository userRepository;

    @Transactional
    public RatingResponse createRating(UUID userId, RatingRequest request) {
        AppUser user = userService.findById(userId);
        Movie movie = movieService.findEntityByMovieId(request.movieId());

        Avaliacao avaliacao = Avaliacao.valueOf(request.type().toUpperCase());

        // Upsert: update if exists, create if not
        Rating rating = ratingRepository.findByUserAndMovie(user, movie)
                .orElse(new Rating());

        rating.setUser(user);
        rating.setMovie(movie);
        rating.setAvaliacao(avaliacao);
        rating.setScore(avaliacao == Avaliacao.LIKE ? 1.0f : avaliacao == Avaliacao.DISLIKE ? -1.0f : 0.5f);

        rating = ratingRepository.save(rating);

        // Increment interaction count
        user.setInteractionCount(user.getInteractionCount() + 1);
        userRepository.save(user);

        return new RatingResponse(
                rating.getId().getMostSignificantBits() & Long.MAX_VALUE,
                movie.getMovieId(),
                request.type()
        );
    }

    public List<RatingResponse> getMyRatings(UUID userId) {
        AppUser user = userService.findById(userId);
        return ratingRepository.findByUser(user).stream()
                .map(r -> new RatingResponse(
                        r.getId().getMostSignificantBits() & Long.MAX_VALUE,
                        r.getMovie().getMovieId(),
                        r.getAvaliacao().name().toLowerCase()
                ))
                .toList();
    }

    @Transactional
    public void deleteRating(UUID userId, Integer movieId) {
        AppUser user = userService.findById(userId);
        Movie movie = movieService.findEntityByMovieId(movieId);
        ratingRepository.findByUserAndMovie(user, movie)
                .ifPresent(ratingRepository::delete);
    }
}
