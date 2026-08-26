package io.nextscene.backend.controller;

import io.nextscene.backend.dto.GenrePreferenceRequest;
import io.nextscene.backend.dto.UserResponse;
import io.nextscene.backend.dto.UserStatsResponse;
import io.nextscene.backend.dto.UserUpdateRequest;
import io.nextscene.backend.infra.AuthenticatedUser;
import io.nextscene.backend.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Perfil", description = "Dados, estatísticas e preferências do usuário autenticado")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(userService.getProfile(user.id()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(user.id(), request));
    }

    @GetMapping("/me/stats")
    public ResponseEntity<UserStatsResponse> getStats(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(userService.getStats(user.id()));
    }

    @PutMapping("/me/genres")
    public ResponseEntity<Map<String, String>> updateGenres(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody GenrePreferenceRequest request
    ) {
        userService.updateGenres(user.id(), request);
        return ResponseEntity.ok(Map.of("message", "Gêneros atualizados com sucesso."));
    }
}
