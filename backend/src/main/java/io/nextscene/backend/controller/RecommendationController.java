package io.nextscene.backend.controller;

import io.nextscene.backend.dto.ColdStartRequest;
import io.nextscene.backend.dto.RecommendationResponse;
import io.nextscene.backend.infra.AuthenticatedUser;
import io.nextscene.backend.service.RecommendationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recomendações", description = "Sugestões personalizadas, via motor de ML")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<RecommendationResponse> getRecommendations(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(recommendationService.getRecommendations(user.id()));
    }

    @PostMapping("/cold-start")
    public ResponseEntity<RecommendationResponse> getColdStartRecommendations(
            @RequestBody ColdStartRequest request
    ) {
        return ResponseEntity.ok(recommendationService.getColdStartRecommendations(request));
    }
}
