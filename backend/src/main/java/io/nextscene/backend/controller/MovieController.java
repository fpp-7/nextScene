package io.nextscene.backend.controller;

import io.nextscene.backend.dto.MovieResponse;
import io.nextscene.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getMovies(
            @RequestParam(required = false) String genre
    ) {
        return ResponseEntity.ok(movieService.getMovies(genre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Integer id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(@RequestParam String q) {
        return ResponseEntity.ok(movieService.searchMovies(q));
    }

    @GetMapping("/featured")
    public ResponseEntity<MovieResponse> getFeaturedMovie() {
        return ResponseEntity.ok(movieService.getFeaturedMovie());
    }
}
