package io.nextscene.backend.controller;

import io.nextscene.backend.dto.WatchlistItemResponse;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.service.WatchListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchListService watchListService;

    @GetMapping
    public ResponseEntity<List<WatchlistItemResponse>> getWatchlist(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(watchListService.getWatchlist(user.getId()));
    }

    @PostMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> addToWatchlist(
            @AuthenticationPrincipal AppUser user,
            @PathVariable Integer movieId
    ) {
        watchListService.addToWatchlist(user.getId(), movieId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Adicionado à watchlist."));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Map<String, String>> removeFromWatchlist(
            @AuthenticationPrincipal AppUser user,
            @PathVariable Integer movieId
    ) {
        watchListService.removeFromWatchlist(user.getId(), movieId);
        return ResponseEntity.ok(Map.of("message", "Removido da watchlist."));
    }
}
