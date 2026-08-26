package io.nextscene.backend.service;

import io.nextscene.backend.dto.*;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email já cadastrado.");
        }

        var user = new AppUser();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        return sessionFor(user);
    }

    public AuthResponse login(AuthRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return sessionFor(user);
    }

    /**
     * Renova o access token a partir de um refresh token válido.
     * <p>
     * O refresh é rotacionado: o apresentado é consumido e um novo é devolvido.
     */
    @Transactional(noRollbackFor = RefreshTokenService.InvalidRefreshTokenException.class)
    public AuthResponse refresh(String refreshToken) {
        var rotated = refreshTokenService.rotate(refreshToken);
        AppUser user = rotated.user();

        return new AuthResponse(
                jwtService.generateToken(user.getId().toString(), user.getEmail()),
                rotated.refreshToken(),
                UserResponse.from(user));
    }

    /** Encerra a sessão de verdade: o refresh token deixa de valer no servidor. */
    @Transactional
    public void logout(UUID userId) {
        refreshTokenService.revokeAll(userId);
    }

    private AuthResponse sessionFor(AppUser user) {
        return new AuthResponse(
                jwtService.generateToken(user.getId().toString(), user.getEmail()),
                refreshTokenService.issue(user),
                UserResponse.from(user));
    }

    /** Mensagem propositalmente genérica: não revela se o e-mail existe. */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Credenciais inválidas.");
        }
    }
}
