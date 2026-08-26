package io.nextscene.backend.infra;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados do contrato exposto em {@code /v3/api-docs} e {@code /swagger-ui.html}.
 * <p>
 * {@code @SecurityRequirement} no nível global marca todo endpoint como
 * protegido por padrão; os públicos (auth, catálogo de leitura) continuam
 * acessíveis normalmente — a anotação só afeta o botão "Authorize" do Swagger
 * UI, não a autorização real, que é o {@link SecurityConfig}.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "NextScene API",
                version = "v1",
                description = "Backend do NextScene: autenticação, catálogo, avaliações, "
                        + "watchlist e recomendações. O motor de ML é um serviço interno, "
                        + "sem contrato público."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
@Configuration
public class OpenApiConfig {
}
