package io.nextscene.backend.controller;

import io.nextscene.backend.dto.RatingRequest;
import io.nextscene.backend.dto.RatingResponse;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingResponse> createRating(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody RatingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.createRating(user.getId(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<RatingResponse>> getMyRatings(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(ratingService.getMyRatings(user.getId()));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> deleteRating(
            @AuthenticationPrincipal AppUser user,
            @PathVariable Integer movieId
    ) {
        ratingService.deleteRating(user.getId(), movieId);
        return ResponseEntity.ok(Map.of("message", "Avaliação removida."));
    }
}
