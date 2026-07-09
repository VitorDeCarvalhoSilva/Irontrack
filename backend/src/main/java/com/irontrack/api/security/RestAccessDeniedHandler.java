package com.irontrack.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 Forbidden para usuário autenticado sem permissão sobre o recurso
 * (01_ARQUITETURA_E_PADROES.md §4.1) — nenhuma regra de autorização por
 * papel/role existe ainda nesta sprint, mas o handler já fica pronto para
 * quando existir, com o mesmo payload de erro padronizado.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityExceptionResponseWriter responseWriter;

    public RestAccessDeniedHandler(SecurityExceptionResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        responseWriter.write(request, response, HttpStatus.FORBIDDEN,
                "Acesso negado ao recurso solicitado.");
    }
}
