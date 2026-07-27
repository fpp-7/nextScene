package io.nextscene.backend.service;

import io.nextscene.backend.dto.RatingRequest;
import io.nextscene.backend.dto.RatingResponse;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.Movie;
import io.nextscene.backend.model.Rating;
import io.nextscene.backend.model.enums.Avaliacao;
import io.nextscene.backend.repository.AppUserRepository;
import io.nextscene.backend.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserService userService;
    private final MovieService movieService;
    private final AppUserRepository userRepository;

    @Transactional
    public RatingResponse createRating(UUID userId, RatingRequest request) {
        AppUser user = userService.findById(userId);
        RatingResponse response = upsert(user, request);
        userRepository.save(user);
        return response;
    }

    /**
     * Grava várias avaliações de uma vez — usado pelo onboarding.
     * <p>
     * Antes o onboarding enviava as avaliações apenas para
     * {@code /recommendations/cold-start}, que só devolve sugestões e não
     * persiste nada. O resultado é que nenhuma avaliação inicial era gravada e
     * o histórico do usuário nascia vazio.
     */
    @Transactional
    public List<RatingResponse> createRatings(UUID userId, List<RatingRequest> requests) {
        AppUser user = userService.findById(userId);

        List<RatingResponse> responses = new ArrayList<>();
        for (RatingRequest request : requests) {
            responses.add(upsert(user, request));
        }

        userRepository.save(user);
        return responses;
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getMyRatings(UUID userId) {
        AppUser user = userService.findById(userId);
        return ratingRepository.findByUser(user).stream()
                .map(r -> new RatingResponse(
                        r.getId().toString(),
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

    /**
     * Cria ou atualiza a avaliação. O contador de interações só sobe quando a
     * avaliação é nova — reavaliar o mesmo filme não deve inflar o histórico.
     */
    private RatingResponse upsert(AppUser user, RatingRequest request) {
        Movie movie = movieService.findEntityByMovieId(request.movieId());
        Avaliacao avaliacao = parseAvaliacao(request.type());

        Rating rating = ratingRepository.findByUserAndMovie(user, movie).orElse(null);
        if (rating == null) {
            rating = new Rating();
            rating.setUser(user);
            rating.setMovie(movie);
            user.setInteractionCount(user.getInteractionCount() + 1);
        }

        rating.setAvaliacao(avaliacao);
        rating.setScore((float) avaliacao.toRatingScale());
        rating = ratingRepository.save(rating);

        return new RatingResponse(
                rating.getId().toString(),
                movie.getMovieId(),
                avaliacao.name().toLowerCase()
        );
    }

    private Avaliacao parseAvaliacao(String type) {
        try {
            return Avaliacao.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Tipo de avaliação inválido: '" + type + "'. Use like, dislike ou seen.");
        }
    }
}
