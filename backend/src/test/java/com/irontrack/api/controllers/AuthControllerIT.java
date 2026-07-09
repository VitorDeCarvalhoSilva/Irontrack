package com.irontrack.api.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irontrack.api.services.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobertura de integração ponta a ponta via HTTP real (10_ESTRATEGIA_DE_TESTES.md
 * §C: MockMvc + SQLite real, sem H2) do fluxo crítico de
 * 07_ROADMAP_BACKEND.md §C.1: registro (já verificado, ADR-018) → login
 * imediato → endpoint autenticado → refresh (rotação) → logout.
 * {@link EmailService} é mockado (interface, sem limitação de
 * ByteBuddy/JDK26) para confirmar que o e-mail de verificação não é mais
 * disparado no registro (13_ADR_LOG.md ADR-018).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

    @Test
    void deveExecutarFluxoCompletoDeRegistroLoginImediatoRefreshELogout() throws Exception {
        String email = "gabriel.it@email.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Gabriel Teste\",\"email\":\"" + email + "\",\"password\":\"SenhaSegura123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerifiedAt").exists());

        // ADR-018: verificação de e-mail desativada — nenhum e-mail de verificação é disparado.
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());

        // Login funciona imediatamente após o registro, sem passo de verificação intermediário.
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"SenhaSegura123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tokens = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = tokens.get("accessToken").asText();
        String refreshToken = tokens.get("refreshToken").asText();

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rotated = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        assertThat(rotated.get("refreshToken").asText()).isNotEqualTo(refreshToken);

        // Rotação obrigatória: o refresh token antigo não pode mais ser usado.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotated.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar422AoRegistrarEmailJaCadastrado() throws Exception {
        String email = "duplicado.it@email.com";
        String body = "{\"name\":\"Duplicado\",\"email\":\"" + email + "\",\"password\":\"SenhaSegura123!\"}";

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void deveRetornar400ParaSenhaFraca() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fraco\",\"email\":\"fraco.it@email.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }
}
