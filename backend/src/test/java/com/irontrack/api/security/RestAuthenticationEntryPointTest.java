package com.irontrack.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Substitui o {@code Http403ForbiddenEntryPoint} padrão do Spring Security —
 * confirma que requisições sem autenticação válida respondem 401 com o
 * payload de erro padronizado (01_ARQUITETURA_E_PADROES.md §4.1). Usa
 * {@link SecurityExceptionResponseWriter} real (não mock — classe concreta,
 * mesma limitação de ByteBuddy/JDK26 já documentada em {@code AuthServiceTest}).
 */
class RestAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    private final RestAuthenticationEntryPoint entryPoint =
            new RestAuthenticationEntryPoint(new SecurityExceptionResponseWriter(objectMapper));

    @Test
    void deveResponder401ComPayloadDeErroPadronizado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("credenciais inválidas"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"status\":401");
        assertThat(response.getContentAsString()).contains("/api/v1/users/me");
    }
}
