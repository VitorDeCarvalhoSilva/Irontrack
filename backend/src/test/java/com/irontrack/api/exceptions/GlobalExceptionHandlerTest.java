package com.irontrack.api.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica que o handler global mapeia cada exceção para o status HTTP e o
 * formato de payload exatos de 01_ARQUITETURA_E_PADROES.md §4.1 /
 * 03_CONTRATOS_API.md §1.4.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deveMapearResourceNotFoundExceptionPara404() {
        HttpServletRequest request = mockRequest("/api/v1/exercises/does-not-exist");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Exercício não encontrado."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Exercício não encontrado.");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/exercises/does-not-exist");
        assertThat(response.getBody().errorCode()).isNull();
    }

    @Test
    void deveMapearBusinessRuleExceptionPara422ComErrorCode() {
        HttpServletRequest request = mockRequest("/api/v1/sessions/start");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessRule(
                new BusinessRuleException("Já existe uma sessão em aberto.", "SESSION_ALREADY_IN_PROGRESS"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(422);
        assertThat(response.getBody().errorCode()).isEqualTo("SESSION_ALREADY_IN_PROGRESS");
    }

    @Test
    void deveMapearForbiddenExceptionPara403() {
        HttpServletRequest request = mockRequest("/api/v1/exercises/exe-1");

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(
                new ForbiddenException("Exercício não pertence ao usuário autenticado."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deveMapearUnauthorizedExceptionPara401() {
        HttpServletRequest request = mockRequest("/api/v1/users/me");

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(
                new UnauthorizedException("Token expirado."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveMapearUnauthorizedExceptionComErrorCode() {
        HttpServletRequest request = mockRequest("/api/v1/auth/refresh");

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(
                new UnauthorizedException("Refresh token inválido.", "INVALID_REFRESH_TOKEN"), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void deveMapearForbiddenExceptionComErrorCode() {
        HttpServletRequest request = mockRequest("/api/v1/auth/login");

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(
                new ForbiddenException("E-mail não verificado.", "EMAIL_NOT_VERIFIED"), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("EMAIL_NOT_VERIFIED");
    }

    @Test
    void deveMapearBadRequestExceptionPara400ComErrorCode() {
        HttpServletRequest request = mockRequest("/api/v1/auth/verify-email/abc");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                new BadRequestException("Token de verificação inválido ou expirado.", "INVALID_OR_EXPIRED_TOKEN"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_OR_EXPIRED_TOKEN");
    }

    @Test
    void deveMapearTooManyRequestsExceptionPara429ComErrorCode() {
        HttpServletRequest request = mockRequest("/api/v1/auth/login");

        ResponseEntity<ErrorResponse> response = handler.handleTooManyRequests(
                new TooManyRequestsException("Muitas tentativas de login.", "TOO_MANY_LOGIN_ATTEMPTS"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(429);
        assertThat(response.getBody().errorCode()).isEqualTo("TOO_MANY_LOGIN_ATTEMPTS");
    }

    @Test
    void deveMapearExcecaoGenericaPara500() {
        HttpServletRequest request = mockRequest("/api/v1/anything");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
                new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    private HttpServletRequest mockRequest(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}
