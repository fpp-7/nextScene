package io.nextscene.backend.service;

import io.nextscene.backend.dto.*;
import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.model.enums.Avaliacao;
import io.nextscene.backend.repository.AppUserRepository;
import io.nextscene.backend.repository.RatingRepository;
import io.nextscene.backend.repository.WatchListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final WatchListRepository watchListRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

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
        boolean emailChanged = false;
        if (request.email() != null && !request.email().isBlank()) {
            emailChanged = !user.getEmail().equals(request.email());
            if (emailChanged && userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email já está em uso.");
            }
            user.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        user = userRepository.save(user);

        // O access token carrega o e-mail nas claims, e o JwtAuthFilter monta o
        // principal a partir delas sem consultar o banco. Trocar o e-mail sem
        // encerrar a sessão deixaria o principal mentindo até o token vencer.
        // Revogar os refresh tokens força um login novo, com claims corretas —
        // e é o mesmo comportamento que uma troca de senha merece.
        if (emailChanged) {
            log.info("E-mail do usuário {} alterado — revogando as sessões ativas.", userId);
            refreshTokenService.revokeAll(userId);
        }

        return UserResponse.from(user);
    }

    public UserStatsResponse getStats(UUID userId) {
        AppUser user = findById(userId);
        long rated = ratingRepository.countByUser(user);
        long watched = ratingRepository.countByUserAndAvaliacao(user, Avaliacao.SEEN);
        long favorites = ratingRepository.countByUserAndAvaliacao(user, Avaliacao.LIKE);
        return new UserStatsResponse(rated, watched, favorites);
    }

    /**
     * Grava as duas listas de gênero do onboarding.
     * <p>
     * {@code disliked} era descartado em silêncio: o app sempre enviou o campo e
     * o backend só lia {@code liked}. Quem excluía Terror continuava recebendo
     * Terror, sem nenhum sinal de que o pedido tinha sido ignorado. Agora vale —
     * ver o veto aplicado em {@code RecommendationService.buildResponse}.
     * <p>
     * As duas listas são substituídas por inteiro, não mescladas: a tela envia
     * sempre o estado completo, e mesclar impediria de desfazer uma exclusão.
     */
    @Transactional
    public void updateGenres(UUID userId, GenrePreferenceRequest request) {
        AppUser user = findById(userId);

        List<String> liked = new ArrayList<>();
        if (request.liked() != null) liked.addAll(request.liked());
        user.setGenresPreference(liked);

        List<String> disliked = new ArrayList<>();
        if (request.disliked() != null) disliked.addAll(request.disliked());
        user.setGenresExcluded(disliked);

        userRepository.save(user);
    }
}
