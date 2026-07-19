package io.nextscene.backend.service;

import io.nextscene.backend.dto.*;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.enums.Avaliacao;
import io.nextscene.backend.repository.AppUserRepository;
import io.nextscene.backend.repository.RatingRepository;
import io.nextscene.backend.repository.WatchListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final WatchListRepository watchListRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUser findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }

    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(findById(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UserUpdateRequest request) {
        AppUser user = findById(userId);

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }
        if (request.email() != null && !request.email().isBlank()) {
            if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email já está em uso.");
            }
            user.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public UserStatsResponse getStats(UUID userId) {
        AppUser user = findById(userId);
        long rated = ratingRepository.countByUser(user);
        long watched = ratingRepository.countByUserAndAvaliacao(user, Avaliacao.SEEN);
        long favorites = ratingRepository.countByUserAndAvaliacao(user, Avaliacao.LIKE);
        return new UserStatsResponse(rated, watched, favorites);
    }

    @Transactional
    public void updateGenres(UUID userId, GenrePreferenceRequest request) {
        AppUser user = findById(userId);
        List<String> genres = new ArrayList<>();
        if (request.liked() != null) genres.addAll(request.liked());
        user.setGenresPreference(genres);
        userRepository.save(user);
    }
}
