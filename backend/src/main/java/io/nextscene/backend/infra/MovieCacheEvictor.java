package io.nextscene.backend.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Invalida os caches do catálogo depois que o conteúdo dos filmes muda.
 * <p>
 * Existia um buraco aqui: os três caches eram populados e nunca invalidados. O
 * job de enriquecimento gravava título em português, pôster, sinopse e trailer,
 * e o app continuava servindo a versão antiga até o TTL de 10 minutos vencer.
 * <p>
 * <b>Componente separado de propósito.</b> {@code @CacheEvict} funciona por
 * proxy: se o job anotasse um método próprio e o chamasse de dentro da própria
 * classe, a chamada não passaria pelo proxy e a anotação seria silenciosamente
 * ignorada — o pior tipo de bug, porque parece resolvido no código.
 * <p>
 * {@code allEntries = true} em vez de invalidar por chave: as chaves de
 * {@code movies} combinam gênero, ordenação, página e tamanho, e um único filme
 * enriquecido pode aparecer em dezenas delas. Descobrir quais custaria mais que
 * simplesmente reconstruir o cache na próxima leitura.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieCacheEvictor {

    /** Constantes, não literais: os mesmos nomes são usados na anotação e no
     *  listener de startup, e a anotação exige valor de tempo de compilação. */
    public static final String MOVIES = "movies";
    public static final String MOVIE_BY_ID = "movieById";
    public static final String FEATURED_MOVIE = "featuredMovie";

    private static final List<String> CATALOG_CACHES =
            List.of(MOVIES, MOVIE_BY_ID, FEATURED_MOVIE);

    private final CacheManager cacheManager;

    @Caching(evict = {
            @CacheEvict(value = MOVIES, allEntries = true),
            @CacheEvict(value = MOVIE_BY_ID, allEntries = true),
            @CacheEvict(value = FEATURED_MOVIE, allEntries = true)
    })
    public void evictCatalog() {
        log.debug("Caches do catálogo invalidados.");
    }

    /**
     * Invalida o cache na subida da aplicação.
     * <p>
     * O Redis é um container à parte e sobrevive ao restart do backend: depois
     * de um deploy, entradas gravadas pela versão anterior seguiam sendo
     * servidas por até 10 minutos. Foi exatamente o que aconteceu ao aplicar a
     * V12 — o destaque continuou mostrando um filme que a versão nova já
     * considerava inexibível, e a migration estava certa o tempo todo.
     * <p>
     * Limpa pelo {@link CacheManager} em vez de chamar {@link #evictCatalog()}:
     * auto-invocação não passa pelo proxy do Spring, então a anotação do outro
     * método seria ignorada em silêncio — a mesma armadilha descrita acima, e
     * a razão de este método não ser apenas um encaminhamento.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void evictOnStartup() {
        CATALOG_CACHES.forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        log.info("Caches do catálogo invalidados na subida — evita servir dados da versão anterior.");
    }
}
