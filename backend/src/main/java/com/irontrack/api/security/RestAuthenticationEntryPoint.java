package com.irontrack.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 Unauthorized para requisições sem token, com token inválido ou
 * expirado (01_ARQUITETURA_E_PADROES.md §4.1) — substitui o
 * {@code Http403ForbiddenEntryPoint} padrão do Spring Security (que
 * responderia 403 sem corpo, quebrando o contrato de erro documentado).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityExceptionResponseWriter responseWriter;

    public RestAuthenticationEntryPoint(SecurityExceptionResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        responseWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                "Token ausente, inválido ou expirado.");
    }
}
