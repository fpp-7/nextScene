package io.nextscene.backend.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

/**
 * Invalida os caches do catálogo depois que o conteúdo dos filmes muda.
 * <p>
 * Existia um buraco aqui: os três caches ({@code movies}, {@code movieById},
 * {@code featuredMovie}) eram populados e nunca invalidados. O job de
 * enriquecimento gravava título em português, pôster, sinopse e trailer, e o
 * app continuava servindo a versão antiga até o TTL de 10 minutos vencer — o
 * que fazia um filme recém-enriquecido aparecer sem pôster mesmo depois de o
 * banco já ter um.
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
public class MovieCacheEvictor {

    @Caching(evict = {
            @CacheEvict(value = "movies", allEntries = true),
            @CacheEvict(value = "movieById", allEntries = true),
            @CacheEvict(value = "featuredMovie", allEntries = true)
    })
    public void evictCatalog() {
        log.debug("Caches do catálogo invalidados.");
    }
}
