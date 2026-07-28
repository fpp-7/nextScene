package io.nextscene.backend.infra;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 401 quando não há credencial válida na requisição.
 * <p>
 * Sem um entry point explícito, o Spring Security devolve <b>403</b> para
 * requisição sem autenticação numa API stateless. A diferença não é acadêmica:
 * o aplicativo trata 401 como "sessão expirou, volte ao login" e 403 como "você
 * não tem permissão para isto". Como o token expira em 30 minutos e o backend
 * respondia 403, o interceptor do app nunca disparava — o usuário seguia
 * "logado", via "Request failed with status code 403" em todas as telas e suas
 * avaliações falhavam sem aviso.
 * <p>
 * 403 continua reservado para o caso legítimo: autenticado, porém sem permissão.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"error\":\"Sessão expirada ou credenciais ausentes.\"}");
    }
}
