package io.nextscene.backend.infra;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * Cliente HTTP usado para falar com o TMDB.
     * <p>
     * Usa pool de conexões: o SimpleClientHttpRequestFactory anterior abria e
     * fechava uma conexão TCP (e um handshake TLS) por chamada, o que pesava no
     * job de enriquecimento, que faz dezenas de requisições em sequência.
     */
    @Bean
    public RestTemplate restTemplate() {
        // No Spring 7 os timeouts ficam no cliente HTTP, não no request factory.
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(20)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(2))
                        .setSocketTimeout(Timeout.ofSeconds(5))
                        .build())
                .build();

        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setResponseTimeout(Timeout.ofSeconds(5))
                        .setConnectionRequestTimeout(Timeout.ofSeconds(2))
                        .build())
                .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}
