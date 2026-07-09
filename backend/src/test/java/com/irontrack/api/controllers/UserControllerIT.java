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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobertura de integração via HTTP real (10_ESTRATEGIA_DE_TESTES.md §C) do
 * fluxo de perfil/exclusão de conta de 07_ROADMAP_BACKEND.md §C.1:
 * troca de senha, solicitação e cancelamento de exclusão.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

    @Test
    void deveTrocarSenhaESolicitarECancelarExclusaoDeConta() throws Exception {
        String email = "perfil.it@email.com";

        registrar(email, "SenhaSegura123!");
        String accessToken = logar(email, "SenhaSegura123!");

        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"SenhaErrada!\",\"newPassword\":\"NovaSenha456!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CURRENT_PASSWORD"));

        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"SenhaSegura123!\",\"newPassword\":\"NovaSenha456!\"}"))
                .andExpect(status().isNoContent());

        String novoAccessToken = logar(email, "NovaSenha456!");

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + novoAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NovaSenha456!\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.deletionScheduledFor").exists());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"NovaSenha456!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_PENDING_DELETION"));

        mockMvc.perform(post("/api/v1/auth/cancel-deletion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"SenhaErrada!\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/cancel-deletion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"NovaSenha456!\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/cancel-deletion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"NovaSenha456!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("NO_PENDING_DELETION"));

        logar(email, "NovaSenha456!");
    }

    @Test
    void updateProfileDeveExigirNovaVerificacaoQuandoEmailMuda() throws Exception {
        String email = "trocaremail.it@email.com";
        registrar(email, "SenhaSegura123!");
        String accessToken = logar(email, "SenhaSegura123!");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"novoemail.it@email.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("novoemail.it@email.com"))
                .andExpect(jsonPath("$.emailVerifiedAt").doesNotExist());
    }

    private void registrar(String email, String password) throws Exception {
        // ADR-018: registro já nasce com email_verified_at preenchido, sem
        // passo de verificação intermediário.
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Usuário Teste\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
    }

    private String logar(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tokens = objectMapper.readTree(result.getResponse().getContentAsString());
        return tokens.get("accessToken").asText();
    }
}
