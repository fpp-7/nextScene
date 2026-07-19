package io.nextscene.backend.controller;

import io.nextscene.backend.dto.ColdStartRequest;
import io.nextscene.backend.dto.RecommendationResponse;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<RecommendationResponse> getRecommendations(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(recommendationService.getRecommendations(user.getId()));
    }

    @PostMapping("/cold-start")
    public ResponseEntity<RecommendationResponse> getColdStartRecommendations(
            @RequestBody ColdStartRequest request
    ) {
        return ResponseEntity.ok(recommendationService.getColdStartRecommendations(request));
    }
}
