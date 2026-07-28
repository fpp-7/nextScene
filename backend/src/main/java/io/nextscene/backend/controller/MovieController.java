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

    /**
     * Catálogo paginado. Antes devolvia sempre os mesmos 30 primeiros registros
     * sem forma de avançar — o resto do catálogo era inalcançável pelo app.
     */
    /**
     * Catálogo paginado.
     *
     * @param sort {@code popular} (mais avaliados), {@code recent} (mais novos)
     *             ou {@code rating} (melhor nota, padrão)
     */
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getMovies(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(movieService.getMovies(genre, sort, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Integer id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(movieService.searchMovies(q, page, size));
    }

    @GetMapping("/featured")
    public ResponseEntity<MovieResponse> getFeaturedMovie() {
        return ResponseEntity.ok(movieService.getFeaturedMovie());
    }
}
