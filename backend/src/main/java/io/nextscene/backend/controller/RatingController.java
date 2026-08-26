package io.nextscene.backend.controller;

import io.nextscene.backend.dto.RatingRequest;
import io.nextscene.backend.dto.RatingResponse;
import io.nextscene.backend.infra.AuthenticatedUser;
import io.nextscene.backend.service.RatingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
@Tag(name = "Avaliações", description = "Like, dislike e já-assisti sobre filmes do catálogo")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingResponse> createRating(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody RatingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.createRating(user.id(), request));
    }

    /** Grava várias avaliações de uma vez — usado ao final do onboarding. */
    @PostMapping("/batch")
    public ResponseEntity<List<RatingResponse>> createRatings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody List<RatingRequest> requests
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.createRatings(user.id(), requests));
    }

    @GetMapping("/me")
    public ResponseEntity<List<RatingResponse>> getMyRatings(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ratingService.getMyRatings(user.id()));
    }

    /**
     * Avaliação de um único filme. Sem avaliação registrada é 404 — o mesmo
     * status de filme inexistente, e a tela de detalhes trata os dois casos
     * como "sem avaliação".
     * <p>
     * Precisa ser declarado depois de {@code /me}: caso contrário "me" seria
     * capturado por {@code {movieId}} e falharia na conversão para inteiro.
     */
    @GetMapping("/{movieId}")
    public ResponseEntity<RatingResponse> getMyRatingFor(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Integer movieId
    ) {
        return ratingService.getMyRatingFor(user.id(), movieId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> deleteRating(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Integer movieId
    ) {
        ratingService.deleteRating(user.id(), movieId);
        return ResponseEntity.ok(Map.of("message", "Avaliação removida."));
    }
}
