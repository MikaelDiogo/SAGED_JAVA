package br.gov.crateus.bcm.devhost.saged;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.crateus.bcm.saged.application.TelegramBotService;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramTechnicianStatus;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramTechnicianRepository;
import br.gov.crateus.bcm.saged.infrastructure.telegram.TelegramSender;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

class TelegramTechnicianLinkIntegrationTest extends SagedIntegrationTestBase {

    @MockBean
    @SuppressWarnings("unused")
    private TelegramSender telegramSender;

    @Autowired
    private TelegramBotService botService;

    @Autowired
    private TelegramTechnicianRepository technicianRepository;

    @Test
    void linkTelegram_validCode_creates204AndPersistsLink() throws Exception {
        String telegramUserId = "111111111";
        String code = botService.generateLinkCode(telegramUserId);

        mockMvc.perform(post("/api/v1/saged/telegram/technicians/link")
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isNoContent());

        var saved = technicianRepository.findByTelegramUserId(telegramUserId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getKeycloakUserId()).isEqualTo(TEST_USER_ID);
        assertThat(saved.get().getStatus()).isEqualTo(TelegramTechnicianStatus.ACTIVE);
    }

    @Test
    void linkTelegram_invalidCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/saged/telegram/technicians/link")
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "000000"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkTelegram_codeConsumedOnce_secondUseFails() throws Exception {
        String code = botService.generateLinkCode("222222222");

        mockMvc.perform(post("/api/v1/saged/telegram/technicians/link")
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isNoContent());

        // second use of the same code with a different user must fail — code was consumed
        UUID differentUser = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/saged/telegram/technicians/link")
                        .with(jwt()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SAGED_TECNICO"))
                                .jwt(t -> t.subject(differentUser.toString())
                                           .claim("org_unit_id", TEST_DEPT_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkTelegram_alreadyLinkedKeycloak_returns409() throws Exception {
        String code1 = botService.generateLinkCode("333333333");
        mockMvc.perform(post("/api/v1/saged/telegram/technicians/link")
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code1))))
                .andExpect(status().isNoContent());

        String code2 = botService.generateLinkCode("444444444");
        mockMvc.perform(post("/api/v1/saged/telegram/technicians/link")
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code2))))
                .andExpect(status().isConflict());
    }

    @Test
    void linkTelegram_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/saged/telegram/technicians/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "123456"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateTechnician_adminGeral_returns204() throws Exception {
        String code = botService.generateLinkCode("555555555");
        mockMvc.perform(post("/api/v1/saged/telegram/technicians/link")
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isNoContent());

        UUID techId = technicianRepository.findByTelegramUserId("555555555").get().getId();

        mockMvc.perform(patch("/api/v1/saged/telegram/technicians/{id}/deactivate", techId)
                        .with(adminJwt()))
                .andExpect(status().isNoContent());

        assertThat(technicianRepository.findById(techId).get().getStatus())
                .isEqualTo(TelegramTechnicianStatus.INACTIVE);
    }

    @Test
    void deactivateTechnician_tecnico_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/saged/telegram/technicians/{id}/deactivate", UUID.randomUUID())
                        .with(tecnicoJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateTechnician_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/saged/telegram/technicians/{id}/deactivate", UUID.randomUUID())
                        .with(adminJwt()))
                .andExpect(status().isNotFound());
    }
}
