package br.gov.crateus.bcm.devhost.saged;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.crateus.bcm.saged.infrastructure.entity.SpecialtyEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.SpecialtyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class DemandIntegrationTest extends SagedIntegrationTestBase {

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @BeforeEach
    void setUpSpecialty() {
        SpecialtyEntity specialty = new SpecialtyEntity();
        specialty.setCode("HW");
        specialty.setName("Hardware");
        specialtyRepository.save(specialty);
    }

    // --- create ---

    @Test
    void createDemand_returns201WithCorrectProtocolFormat() throws Exception {
        mockMvc.perform(post("/api/v1/saged/demands")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Computer won't start",
                                "description", "The computer does not power on",
                                "specialtyCode", "HW",
                                "departmentId", TEST_DEPT_ID
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocol").value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{4}-HW-\\d{5}")))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.specialtyCode").value("HW"));
    }

    @Test
    void createDemand_unknownSpecialty_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/saged/demands")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Issue", "description", "Desc",
                                "specialtyCode", "UNKNOWN", "departmentId", TEST_DEPT_ID
                        ))))
                .andExpect(status().isBadRequest());
    }

    // --- list / filter / pagination ---

    @Test
    void listDemands_adminGeral_seesAll() throws Exception {
        createDemandInDept(TEST_DEPT_ID);
        createDemandInDept(UUID.randomUUID());

        mockMvc.perform(get("/api/v1/saged/demands").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listDemands_adminSetor_onlySeesOwnDepartment() throws Exception {
        createDemandInDept(TEST_DEPT_ID);
        createDemandInDept(UUID.randomUUID());

        mockMvc.perform(get("/api/v1/saged/demands").with(adminSetorJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listDemands_tecnico_onlySeesOwnSpecialties() throws Exception {
        createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(get("/api/v1/saged/demands").with(tecnicoJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listDemands_filterByStatus_returnsOnlyMatching() throws Exception {
        createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(get("/api/v1/saged/demands?status=TODO").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/saged/demands?status=DONE").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listDemands_pagination_respectsPageSize() throws Exception {
        createDemandInDept(TEST_DEPT_ID);
        createDemandInDept(TEST_DEPT_ID);
        createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(get("/api/v1/saged/demands?page=0&size=2").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true));

        mockMvc.perform(get("/api/v1/saged/demands?page=1&size=2").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    // --- get by id ---

    @Test
    void getDemandById_returnsOk() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(get("/api/v1/saged/demands/{id}", id).with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getDemandById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/saged/demands/{id}", UUID.randomUUID()).with(adminJwt()))
                .andExpect(status().isNotFound());
    }

    // --- status ---

    @Test
    void changeStatus_TODO_to_IN_PROGRESS_returns200() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(patch("/api/v1/saged/demands/{id}/status", id)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatus_invalidTransition_TODO_to_DONE_returns422() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(patch("/api/v1/saged/demands/{id}/status", id)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void changeStatus_interruptedWithoutJustification_returns400() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);
        advanceToInProgress(id);

        mockMvc.perform(patch("/api/v1/saged/demands/{id}/status", id)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INTERRUPTED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_interruptedWithJustification_returns200() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);
        advanceToInProgress(id);

        mockMvc.perform(patch("/api/v1/saged/demands/{id}/status", id)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INTERRUPTED","justification":"Hardware irreparável"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERRUPTED"));
    }

    @Test
    void changeStatus_tecnico_returns403() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(patch("/api/v1/saged/demands/{id}/status", id)
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeStatus_nonexistentDemand_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/saged/demands/{id}/status", UUID.randomUUID())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isNotFound());
    }

    // --- assign ---

    @Test
    void assignDemand_setsAssignee() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);
        UUID assignee = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/saged/demands/{id}/assignee", id)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("assigneeUserId", assignee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeUserId").value(assignee.toString()));
    }

    @Test
    void assignDemand_tecnico_returns403() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(patch("/api/v1/saged/demands/{id}/assignee", id)
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("assigneeUserId", UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignDemand_nonexistentDemand_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/saged/demands/{id}/assignee", UUID.randomUUID())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("assigneeUserId", UUID.randomUUID()))))
                .andExpect(status().isNotFound());
    }

    // --- note ---

    @Test
    void updateNote_setsNote() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(patch("/api/v1/saged/demands/{id}/note", id)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"Cabo de alimentação trocado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTechnicalNote").value("Cabo de alimentação trocado"));
    }

    @Test
    void updateNote_nonexistentDemand_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/saged/demands/{id}/note", UUID.randomUUID())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"alguma coisa"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateNote_unauthenticated_returns403() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(patch("/api/v1/saged/demands/{id}/note", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"sem auth"}
                                """))
                .andExpect(status().isForbidden());
    }

    // --- history ---

    @Test
    void getDemandHistory_returnsCreatedEntry() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);

        mockMvc.perform(get("/api/v1/saged/demands/{id}/history", id).with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[0].demandId").value(id));
    }

    @Test
    void getDemandHistory_afterStatusChange_hasMultipleEntries() throws Exception {
        String id = createDemandInDept(TEST_DEPT_ID);
        advanceToInProgress(id);

        mockMvc.perform(get("/api/v1/saged/demands/{id}/history", id).with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[1].action").value("IN_PROGRESS"));
    }

    // --- helpers ---

    private String createDemandInDept(UUID deptId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/saged/demands")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Test demand",
                                "description", "Test description",
                                "specialtyCode", "HW",
                                "departmentId", deptId
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        assertThat(node.get("id")).isNotNull();
        return node.get("id").asText();
    }

    private void advanceToInProgress(String demandId) throws Exception {
        mockMvc.perform(patch("/api/v1/saged/demands/{id}/status", demandId)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk());
    }
}
