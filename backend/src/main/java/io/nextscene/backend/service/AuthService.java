package io.nextscene.backend.service;

import io.nextscene.backend.dto.*;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse login(AuthRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciais inválidas.");
        }

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }
}
