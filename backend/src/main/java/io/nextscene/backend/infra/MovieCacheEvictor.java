package io.nextscene.backend.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Invalida os caches do catálogo depois que o conteúdo dos filmes muda.
 * <p>
 * Existia um buraco aqui: os três caches eram populados e nunca invalidados. O
 * job de enriquecimento gravava título em português, pôster, sinopse e trailer,
 * e o app continuava servindo a versão antiga até o TTL de 10 minutos vencer.
 *
 * <h2>Por que não usa {@code @CacheEvict}</h2>
 * A primeira versão era um método anotado com
 * {@code @Caching(evict = {@CacheEvict(allEntries = true), ...})}. O bean estava
 * sob proxy e o {@code CacheInterceptor} de fato rodava — verificado — e ainda
 * assim a entrada continuava sendo servida depois. Sem exceção, sem log: o
 * {@link CacheConfig#errorHandler()} engole falhas de Redis de propósito, e o
 * caminho anotado não deixava rastro nenhum de que não tinha feito nada.
 * <p>
 * Chamar {@link org.springframework.cache.Cache#clear()} direto funciona, é
 * óbvio de ler e não depende de proxy, de auto-invocação nem da resolução de
 * cache por anotação — três armadilhas silenciosas que já custaram caro neste
 * arquivo. O ganho da anotação aqui era estético.
 * <p>
 * Limpa tudo em vez de invalidar por chave: as chaves de {@code movies}
 * combinam gênero, ordenação, página e tamanho, e um único filme enriquecido
 * pode aparecer em dezenas delas. Descobrir quais custaria mais que reconstruir
 * o cache na próxima leitura.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieCacheEvictor {

    public static final String MOVIES = "movies";
    public static final String MOVIE_BY_ID = "movieById";
    public static final String FEATURED_MOVIE = "featuredMovie";

    /** Precisa bater com o prefixo configurado em {@link CacheConfig}. */
    static final String KEY_PREFIX = "nextscene:cache:";

    private static final List<String> CATALOG_CACHES =
            List.of(MOVIES, MOVIE_BY_ID, FEATURED_MOVIE);

    private final StringRedisTemplate redis;

    /**
     * Apaga as chaves pelo padrão, em vez de chamar {@code Cache#clear()}.
     * <p>
     * O caminho do Spring Cache não invalidava nada aqui — nem via
     * {@code @CacheEvict}, nem chamando {@code clear()} direto no
     * {@code Cache} devolvido pelo {@code CacheManager}. Sem exceção e sem
     * log: a entrada seguia sendo servida depois, de forma reproduzível.
     * Apagar a chave é uma linha de Redis, é verificável no teste de
     * integração e não depende de proxy, de resolução por anotação nem do
     * comportamento de {@code clear()} com prefixo customizado.
     */
    public void evictCatalog() {
        long removed = 0;
        for (String name : CATALOG_CACHES) {
            Set<String> keys = redis.keys(KEY_PREFIX + name + "::*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redis.delete(keys);
                removed += deleted == null ? 0 : deleted;
            }
        }
        log.debug("Caches do catálogo invalidados ({} chaves).", removed);
    }

    /**
     * O Redis é um container à parte e sobrevive ao restart do backend: depois
     * de um deploy, entradas gravadas pela versão anterior continuavam sendo
     * servidas por até 10 minutos. Aconteceu de verdade ao aplicar a V12 — o
     * destaque seguiu mostrando um filme que a versão nova já considerava
     * inexibível, com a migration correta o tempo todo.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void evictOnStartup() {
        evictCatalog();
        log.info("Caches do catálogo invalidados na subida — evita servir dados da versão anterior.");
    }
}
