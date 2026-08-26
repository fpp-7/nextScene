package io.nextscene.backend.controller;

import io.nextscene.backend.dto.AuthRequest;
import io.nextscene.backend.dto.AuthResponse;
import io.nextscene.backend.dto.RefreshRequest;
import io.nextscene.backend.dto.RegisterRequest;
import io.nextscene.backend.infra.AuthenticatedUser;
import io.nextscene.backend.infra.LoginRateLimiter;
import io.nextscene.backend.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Cadastro, login, renovação e encerramento de sessão")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;

    /** Cadastros por IP na mesma janela do rate limit de login. */
    @Value("${app.security.register.max-attempts:50}")
    private int maxRegistrations;

    /**
     * Cadastro, limitado por IP com o mesmo contador do login.
     * <p>
     * Sem isso, criar contas em massa era gratuito — e cada conta nova é uma
     * linha no banco que alimenta o re-treino do motor. O limite é o mesmo do
     * login: quem cria mais de 10 contas em 15 minutos do mesmo IP não é um
     * usuário — mas o teto é bem mais alto que o do login e configurável, porque
     * atrás de CGNAT ou da saída de uma empresa muita gente legítima divide o
     * mesmo IP, e barrar um cadastro real é pior que o abuso que se quer conter.
     * <p>
     * A chave é prefixada para não compartilhar o contador com o login: senão
     * um cadastro consumiria tentativas de quem só está tentando entrar.
     */
    @PostMapping("/register")
    @SecurityRequirements
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!rateLimiter.tryConsume("register:" + clientKey(httpRequest), maxRegistrations)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas de cadastro. Tente novamente mais tarde.");
        }

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @SecurityRequirements
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

    /**
     * Troca um refresh token válido por um novo par de tokens.
     * <p>
     * Público de propósito: quem chama aqui é justamente quem está com o access
     * token vencido e não conseguiria passar pelo filtro de autenticação.
     */
    @PostMapping("/refresh")
    @SecurityRequirements
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * Encerra a sessão. Diferente da versão anterior, tem efeito no servidor:
     * os refresh tokens do usuário são revogados, então o aparelho não consegue
     * mais renovar o acesso.
     * <p>
     * O access token em si continua válido até vencer — é a natureza do JWT.
     * Com 30 minutos de validade, a janela é aceitável.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user != null) {
            authService.logout(user.id());
        }
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
