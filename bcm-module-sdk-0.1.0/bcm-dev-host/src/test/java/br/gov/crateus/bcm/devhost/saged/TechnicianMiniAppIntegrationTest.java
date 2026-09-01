package br.gov.crateus.bcm.devhost.saged;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.crateus.bcm.saged.application.DemandService;
import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.SpecialtyEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramTechnicianEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramTechnicianStatus;
import br.gov.crateus.bcm.saged.infrastructure.repository.DemandRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.SpecialtyRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramTechnicianRepository;
import br.gov.crateus.bcm.saged.infrastructure.telegram.TelegramInitDataValidator;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

class TechnicianMiniAppIntegrationTest extends SagedIntegrationTestBase {

    static final String FAKE_TG_USER = "999888777";
    static final String FAKE_INIT   = "fake_init_data";

    @MockBean
    TelegramInitDataValidator initDataValidator;

    @Autowired TelegramTechnicianRepository technicianRepository;
    @Autowired DemandService demandService;
    @Autowired DemandRepository demandRepository;
    @Autowired SpecialtyRepository specialtyRepository;

    @BeforeEach
    void seedSpecialty() {
        given(initDataValidator.extractUserId(FAKE_INIT)).willReturn(FAKE_TG_USER);

        if (specialtyRepository.findAll().isEmpty()) {
            SpecialtyEntity sp = new SpecialtyEntity();
            sp.setCode("HW");
            sp.setName("Hardware");
            specialtyRepository.save(sp);
        }
    }

    @Test
    void listDemands_linkedActiveTechnician_returnsAssignedDemands() throws Exception {
        createLinkedTechnician(FAKE_TG_USER, TEST_USER_ID);
        DemandEntity demand = createAndAssign(TEST_USER_ID);

        mockMvc.perform(post("/api/v1/saged/webhooks/miniapp/tech/demands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("initData", FAKE_INIT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].protocol").value(demand.getProtocol()))
                .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    @Test
    void listDemands_unlinkedUser_returns403() throws Exception {
        given(initDataValidator.extractUserId(FAKE_INIT)).willReturn("unknown_user");

        mockMvc.perform(post("/api/v1/saged/webhooks/miniapp/tech/demands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("initData", FAKE_INIT))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listDemands_inactiveTechnician_returns403() throws Exception {
        TelegramTechnicianEntity tech = createLinkedTechnician(FAKE_TG_USER, TEST_USER_ID);
        tech.setStatus(TelegramTechnicianStatus.INACTIVE);
        technicianRepository.save(tech);

        mockMvc.perform(post("/api/v1/saged/webhooks/miniapp/tech/demands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("initData", FAKE_INIT))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listDemands_invalidInitData_returns401() throws Exception {
        given(initDataValidator.extractUserId("bad"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid"));

        mockMvc.perform(post("/api/v1/saged/webhooks/miniapp/tech/demands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("initData", "bad"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changeStatus_assignedDemand_toInProgress_returns200() throws Exception {
        createLinkedTechnician(FAKE_TG_USER, TEST_USER_ID);
        DemandEntity demand = createAndAssign(TEST_USER_ID);

        mockMvc.perform(post("/api/v1/saged/webhooks/miniapp/tech/demands/{id}/status", demand.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "initData", FAKE_INIT,
                                "newStatus", "IN_PROGRESS"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatus_demandNotAssignedToThisTech_returns403() throws Exception {
        createLinkedTechnician(FAKE_TG_USER, TEST_USER_ID);
        DemandEntity demand = createAndAssign(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/saged/webhooks/miniapp/tech/demands/{id}/status", demand.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "initData", FAKE_INIT,
                                "newStatus", "IN_PROGRESS"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeStatus_interrupted_missingJustification_returns400() throws Exception {
        createLinkedTechnician(FAKE_TG_USER, TEST_USER_ID);
        DemandEntity demand = createAndAssign(TEST_USER_ID);
        demandService.changeStatus(demand.getId(), DemandStatus.IN_PROGRESS, null, "test");

        mockMvc.perform(post("/api/v1/saged/webhooks/miniapp/tech/demands/{id}/status", demand.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "initData", FAKE_INIT,
                                "newStatus", "INTERRUPTED"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_done_withTechnicalNote_returns200() throws Exception {
        createLinkedTechnician(FAKE_TG_USER, TEST_USER_ID);
        DemandEntity demand = createAndAssign(TEST_USER_ID);
        demandService.changeStatus(demand.getId(), DemandStatus.IN_PROGRESS, null, "test");

        mockMvc.perform(post("/api/v1/saged/webhooks/miniapp/tech/demands/{id}/status", demand.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "initData", FAKE_INIT,
                                "newStatus", "DONE",
                                "justification", "Replaced power supply unit"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void changeStatus_invalidStatusValue_returns400() throws Exception {
        createLinkedTechnician(FAKE_TG_USER, TEST_USER_ID);
        DemandEntity demand = createAndAssign(TEST_USER_ID);

        mockMvc.perform(post("/api/v1/saged/webhooks/miniapp/tech/demands/{id}/status", demand.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "initData", FAKE_INIT,
                                "newStatus", "BOGUS_STATUS"
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TelegramTechnicianEntity createLinkedTechnician(String tgUserId, UUID keycloakId) {
        TelegramTechnicianEntity tech = new TelegramTechnicianEntity();
        tech.setTelegramUserId(tgUserId);
        tech.setKeycloakUserId(keycloakId);
        tech.setDisplayName("Test Tech");
        tech.setStatus(TelegramTechnicianStatus.ACTIVE);
        tech.setCreatedBy("test");
        tech.setUpdatedBy("test");
        return technicianRepository.save(tech);
    }

    private DemandEntity createAndAssign(UUID assigneeId) {
        DemandEntity demand = demandService.create(
                "Printer not working at all",
                "The printer makes a noise and stops immediately after power on",
                "HW", null, UUID.randomUUID(), TEST_DEPT_ID, "test");
        demand.setAssigneeUserId(assigneeId);
        return demandRepository.save(demand);
    }
}
