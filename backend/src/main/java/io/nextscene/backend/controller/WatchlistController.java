package io.nextscene.backend.controller;

import io.nextscene.backend.dto.WatchlistItemResponse;
import io.nextscene.backend.infra.AuthenticatedUser;
import io.nextscene.backend.service.WatchListService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
@Tag(name = "Watchlist", description = "Filmes salvos para assistir depois")
public class WatchlistController {

    private final WatchListService watchListService;

    @GetMapping
    public ResponseEntity<List<WatchlistItemResponse>> getWatchlist(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(watchListService.getWatchlist(user.id()));
    }

    @PostMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> addToWatchlist(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Integer movieId
    ) {
        watchListService.addToWatchlist(user.id(), movieId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Adicionado à watchlist."));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> removeFromWatchlist(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Integer movieId
    ) {
        watchListService.removeFromWatchlist(user.id(), movieId);
        return ResponseEntity.ok(Map.of("message", "Removido da watchlist."));
    }
}
