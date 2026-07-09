package com.irontrack.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irontrack.api.exceptions.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Escreve o payload de erro padronizado (03_CONTRATOS_API.md §1.4) para
 * falhas de autenticação/autorização detectadas na cadeia de filtros do
 * Spring Security — antes de qualquer controller ser alcançado, portanto
 * fora do alcance de {@code GlobalExceptionHandler} (@RestControllerAdvice
 * só intercepta exceções lançadas durante a execução de um controller).
 * Usado por {@link RestAuthenticationEntryPoint} (401) e
 * {@link RestAccessDeniedHandler} (403).
 */
@Component
public class SecurityExceptionResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityExceptionResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String message) throws IOException {
        ErrorResponse body = new ErrorResponse(
                status.value(), status.getReasonPhrase(), message, request.getRequestURI(), null);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
