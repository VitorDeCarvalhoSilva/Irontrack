package com.irontrack.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que respostas de erro escritas fora do {@code GlobalExceptionHandler}
 * (falhas de autenticação/autorização na cadeia de filtros do Spring
 * Security, antes de qualquer controller ser alcançado) ainda produzem o
 * payload de erro padronizado exato de 03_CONTRATOS_API.md §1.4 — sem isto,
 * o caso mais comum de falha da API (token ausente/inválido) escaparia do
 * formato de erro documentado.
 */
class SecurityExceptionResponseWriterTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    private final SecurityExceptionResponseWriter writer = new SecurityExceptionResponseWriter(objectMapper);

    @Test
    void deveEscreverPayloadDeErroPadronizadoNaResposta() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(request, response, HttpStatus.UNAUTHORIZED, "Token ausente, inválido ou expirado.");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(body.get("status")).isEqualTo(401);
        assertThat(body.get("error")).isEqualTo("Unauthorized");
        assertThat(body.get("message")).isEqualTo("Token ausente, inválido ou expirado.");
        assertThat(body.get("path")).isEqualTo("/api/v1/users/me");
        assertThat(body.get("timestamp")).isNotNull();
        assertThat(body).doesNotContainKey("errorCode");
    }
}
