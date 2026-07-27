package io.nextscene.backend.controller;

import io.nextscene.backend.dto.AuthRequest;
import io.nextscene.backend.dto.AuthResponse;
import io.nextscene.backend.dto.RegisterRequest;
import io.nextscene.backend.infra.LoginRateLimiter;
import io.nextscene.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientKey = clientKey(httpRequest);
        if (!rateLimiter.tryConsume(clientKey)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas de login. Tente novamente em alguns minutos.");
        }

        AuthResponse response = authService.login(request);
        rateLimiter.reset(clientKey);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        // JWT stateless — o cliente descarta o token.
        return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso."));
    }

    /**
     * Identifica o cliente para o rate limit. Considera X-Forwarded-For porque o
     * backend roda atrás de proxy/túnel em dev — sem isso todas as requisições
     * viriam do mesmo IP e um único usuário bloquearia todos os demais.
     */
    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
