package io.nextscene.backend.controller;

import io.nextscene.backend.dto.GenrePreferenceRequest;
import io.nextscene.backend.dto.UserResponse;
import io.nextscene.backend.dto.UserStatsResponse;
import io.nextscene.backend.dto.UserUpdateRequest;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal AppUser user,
            @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(user.getId(), request));
    }

    @GetMapping("/me/stats")
    public ResponseEntity<UserStatsResponse> getStats(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(userService.getStats(user.getId()));
    }

    @PutMapping("/me/genres")
    public ResponseEntity<Map<String, String>> updateGenres(
            @AuthenticationPrincipal AppUser user,
            @RequestBody GenrePreferenceRequest request
    ) {
        userService.updateGenres(user.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Gêneros atualizados com sucesso."));
    }
}
