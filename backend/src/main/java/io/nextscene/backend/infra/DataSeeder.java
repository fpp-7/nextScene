package io.nextscene.backend.infra;

import io.nextscene.backend.model.AppUser;
import io.nextscene.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@org.springframework.core.annotation.Order(2)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("teste@nextscene.com").isEmpty()) {
            var user = new AppUser();
            user.setName("Usuario Teste");
            user.setEmail("teste@nextscene.com");
            user.setPasswordHash(passwordEncoder.encode("123456"));
            user.setGenresPreference(List.of("Acao", "Ficcao Cientifica", "Drama", "Suspense"));
            user.setInteractionCount(0);
            userRepository.save(user);
            log.info("✅ Usuário de teste criado: teste@nextscene.com / 123456");
        } else {
            log.info("ℹ️ Usuário de teste já existe.");
        }
    }
}
