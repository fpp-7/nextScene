package io.nextscene.backend.service;

import io.nextscene.backend.dto.MovieResponse;
import io.nextscene.backend.dto.WatchlistItemResponse;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.Movie;
import io.nextscene.backend.model.WatchList;
import io.nextscene.backend.repository.WatchListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchListService {

    private final WatchListRepository watchListRepository;
    private final UserService userService;
    private final MovieService movieService;

    public List<WatchlistItemResponse> getWatchlist(UUID userId) {
        AppUser user = userService.findById(userId);
        return watchListRepository.findByUser(user).stream()
                .map(wl -> new WatchlistItemResponse(
                        wl.getId().getMostSignificantBits() & Long.MAX_VALUE,
                        wl.getMovie().getMovieId(),
                        MovieResponse.from(wl.getMovie()),
                        wl.getAddedAt().toString()
                ))
                .toList();
    }

    @Transactional
    public void addToWatchlist(UUID userId, Integer movieId) {
        AppUser user = userService.findById(userId);
        Movie movie = movieService.findEntityByMovieId(movieId);

        if (watchListRepository.existsByUserAndMovie(user, movie)) {
            return; // Already in watchlist
        }

        var watchList = new WatchList();
        watchList.setUser(user);
        watchList.setMovie(movie);
        watchListRepository.save(watchList);
    }

    @Transactional
    public void removeFromWatchlist(UUID userId, Integer movieId) {
        AppUser user = userService.findById(userId);
        Movie movie = movieService.findEntityByMovieId(movieId);
        watchListRepository.deleteByUserAndMovie(user, movie);
    }
}
