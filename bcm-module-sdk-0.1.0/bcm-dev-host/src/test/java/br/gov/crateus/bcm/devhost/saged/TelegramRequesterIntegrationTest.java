package br.gov.crateus.bcm.devhost.saged;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TelegramRequesterIntegrationTest extends SagedIntegrationTestBase {

    @Test
    void listRequesters_adminGeral_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/saged/telegram/requesters").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listRequesters_tecnico_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/saged/telegram/requesters").with(tecnicoJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRequester_adminGeral_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/saged/telegram/requesters")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "telegramChatId", "123456789",
                                "phoneNumber", "85999990001",
                                "displayName", "João Silva",
                                "departmentId", TEST_DEPT_ID
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telegramChatId").value("123456789"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createRequester_tecnico_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/saged/telegram/requesters")
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "telegramChatId", "999999999",
                                "phoneNumber", "85999990002",
                                "departmentId", TEST_DEPT_ID
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRequester_duplicateChatId_returns409() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "telegramChatId", "111111111",
                "phoneNumber", "85999990003",
                "departmentId", TEST_DEPT_ID
        ));

        mockMvc.perform(post("/api/v1/saged/telegram/requesters")
                        .with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/saged/telegram/requesters")
                        .with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void deactivateRequester_setsActiveFalse() throws Exception {
        String id = createRequester("222222222");

        mockMvc.perform(patch("/api/v1/saged/telegram/requesters/{id}/deactivate", id)
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void activateRequester_afterDeactivation_setsActiveTrue() throws Exception {
        String id = createRequester("333333333");

        mockMvc.perform(patch("/api/v1/saged/telegram/requesters/{id}/deactivate", id)
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/saged/telegram/requesters/{id}/activate", id)
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivateRequester_notFound_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/saged/telegram/requesters/{id}/deactivate", UUID.randomUUID())
                        .with(adminJwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateRequester_tecnico_returns403() throws Exception {
        String id = createRequester("444444444");

        mockMvc.perform(patch("/api/v1/saged/telegram/requesters/{id}/deactivate", id)
                        .with(tecnicoJwt()))
                .andExpect(status().isForbidden());
    }

    // --- helper ---

    private String createRequester(String chatId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/saged/telegram/requesters")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "telegramChatId", chatId,
                                "phoneNumber", "85999990000",
                                "departmentId", TEST_DEPT_ID
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asText();
    }
}
