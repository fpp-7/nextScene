package io.nextscene.backend.infra;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Cache em memória para o catálogo, que muda raramente e era relido do banco a
 * cada scroll da tela de descoberta.
 * <p>
 * Caches são limitados por tamanho e TTL de propósito: um ConcurrentMap sem teto
 * cresceria com o catálogo inteiro e viraria vazamento de memória. Para mais de
 * uma instância do backend, isto deve migrar para Redis — cada nó tem hoje seu
 * próprio cache, e uma atualização de filme só expira no nó que a processou.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        var manager = new CaffeineCacheManager("movies", "movieById", "featuredMovie");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats());
        return manager;
    }
}
