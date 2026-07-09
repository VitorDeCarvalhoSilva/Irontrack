package com.irontrack.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 403 Forbidden com payload de erro padronizado para usuário autenticado sem
 * permissão sobre o recurso (01_ARQUITETURA_E_PADROES.md §4.1).
 */
class RestAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    private final RestAccessDeniedHandler handler =
            new RestAccessDeniedHandler(new SecurityExceptionResponseWriter(objectMapper));

    @Test
    void deveResponder403ComPayloadDeErroPadronizado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/exercises/exe-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("negado"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"status\":403");
        assertThat(response.getContentAsString()).contains("/api/v1/exercises/exe-1");
    }
}
