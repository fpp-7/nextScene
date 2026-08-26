package io.nextscene.backend.infra;

import io.nextscene.backend.dto.MovieResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Cache do catálogo, compartilhado entre instâncias via Redis.
 * <p>
 * Antes era Caffeine, em memória por processo: cada nó do backend tinha seu
 * próprio cache, e a atualização de um filme só expirava no nó que a
 * processou. Com Redis, todos os nós leem e invalidam o mesmo estado.
 * <p>
 * Cada cache tem seu serializer amarrado ao tipo real que guarda —
 * {@code MovieResponse} ou {@code List<MovieResponse>} — em vez do
 * serializer genérico com tipagem polimórfica do Jackson
 * ({@code RedisSerializer.json()}), que quebrava na leitura: o valor escrito
 * sem metadado de tipo (`[{...}]`) não batia com o que o desserializador
 * genérico esperava ao ler de volta.
 * <p>
 * <b>Degradação:</b> se o Redis cair, o cache não deve derrubar a API — o
 * {@link CacheErrorHandler} abaixo loga e deixa a chamada seguir direto para o
 * banco, como se o cache tivesse dado miss. Mais lento, não indisponível.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final String KEY_PREFIX = MovieCacheEvictor.KEY_PREFIX;
    private static final Duration TTL = Duration.ofMinutes(10);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = JsonMapper.builder().build();

        var movieByIdConfig = cacheConfig(
                new JacksonJsonRedisSerializer<>(objectMapper, MovieResponse.class));
        var movieListJavaType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, MovieResponse.class);
        var movieListConfig = cacheConfig(
                new JacksonJsonRedisSerializer<List<MovieResponse>>(objectMapper, movieListJavaType));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(movieByIdConfig)
                .withCacheConfiguration("movies", movieListConfig)
                .withCacheConfiguration("movieById", movieByIdConfig)
                .withCacheConfiguration("featuredMovie", movieByIdConfig)
                .build();
    }

    private RedisCacheConfiguration cacheConfig(JacksonJsonRedisSerializer<?> valueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(TTL)
                // prefixCacheNameWith é a API própria para prefixo fixo; o
                // computePrefixWith com lambda que existia aqui produzia as
                // mesmas chaves. Trocado por ser mais direto — não foi o que
                // resolveu a invalidação (ver MovieCacheEvictor).
                .prefixCacheNameWith(KEY_PREFIX)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer));
    }

    /**
     * Sem isto, uma falha de rede no Redis (conexão recusada, timeout) subiria
     * como exceção não tratada e derrubaria qualquer endpoint anotado com
     * {@code @Cacheable} — o catálogo ficaria fora do ar por causa do cache que
     * deveria só acelerá-lo.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis indisponível ao ler o cache '{}': {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis indisponível ao gravar no cache '{}': {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis indisponível ao invalidar o cache '{}': {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis indisponível ao limpar o cache '{}': {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
