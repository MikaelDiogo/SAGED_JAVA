package br.gov.crateus.bcm.devhost.saged;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.crateus.bcm.saged.infrastructure.entity.SpecialtyEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.SpecialtyRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

class DemandVisibilityMatrixTest extends SagedIntegrationTestBase {

    static final UUID OTHER_DEPT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @BeforeEach
    void setUpSpecialties() {
        SpecialtyEntity hw = new SpecialtyEntity();
        hw.setCode("HW");
        hw.setName("Hardware");
        specialtyRepository.save(hw);

        SpecialtyEntity net = new SpecialtyEntity();
        net.setCode("NET");
        net.setName("Redes");
        specialtyRepository.save(net);
    }

    // ── TECNICO_LIDER ────────────────────────────────────────────────────────

    @Test
    void tecnicoLider_cannotSeeDemandsFromOtherUnit() throws Exception {
        createDemand(OTHER_DEPT_ID, "HW");

        mockMvc.perform(get("/api/v1/saged/demands").with(lidJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void tecnicoLider_seesOnlyOwnUnitDemands() throws Exception {
        createDemand(TEST_DEPT_ID, "HW");
        createDemand(OTHER_DEPT_ID, "HW");

        mockMvc.perform(get("/api/v1/saged/demands").with(lidJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void tecnicoLider_withoutOrgUnitClaim_returns403() throws Exception {
        JwtRequestPostProcessor liderNoUnit = jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_SAGED_TECNICO_LIDER"))
                .jwt(t -> t.subject(TEST_USER_ID.toString()));

        mockMvc.perform(get("/api/v1/saged/demands").with(liderNoUnit))
                .andExpect(status().isForbidden());
    }

    // ── TECNICO ──────────────────────────────────────────────────────────────

    @Test
    void tecnico_onlySeesDemandsMatchingSpecialty() throws Exception {
        createDemand(TEST_DEPT_ID, "HW");
        createDemand(TEST_DEPT_ID, "NET");

        mockMvc.perform(get("/api/v1/saged/demands").with(tecnicoJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void tecnico_withEmptySpecialtyCodes_seesNothing() throws Exception {
        createDemand(TEST_DEPT_ID, "HW");

        JwtRequestPostProcessor tecnicoNoSpecialty = jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_SAGED_TECNICO"))
                .jwt(t -> t.subject(TEST_USER_ID.toString())
                           .claim("org_unit_id", TEST_DEPT_ID.toString())
                           .claim("specialty_codes", ""));

        mockMvc.perform(get("/api/v1/saged/demands").with(tecnicoNoSpecialty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void tecnico_cannotAccessReports_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/saged/demands/reports").with(tecnicoJwt()))
                .andExpect(status().isForbidden());
    }

    // ── ADMIN_SETOR ──────────────────────────────────────────────────────────

    @Test
    void adminSetor_cannotSeeDemandsFromOtherUnit() throws Exception {
        createDemand(OTHER_DEPT_ID, "HW");

        mockMvc.perform(get("/api/v1/saged/demands").with(adminSetorJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void adminSetor_withoutOrgUnitClaim_returns403() throws Exception {
        JwtRequestPostProcessor setorNoUnit = jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_SAGED_ADMIN_SETOR"))
                .jwt(t -> t.subject(TEST_USER_ID.toString()));

        mockMvc.perform(get("/api/v1/saged/demands").with(setorNoUnit))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSetor_cannotCreateDemandInOtherUnit_usesJwtDeptInstead() throws Exception {
        String response = mockMvc.perform(post("/api/v1/saged/demands")
                        .with(adminSetorJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Test demand title here",
                                "description", "Some description that is long enough",
                                "specialtyCode", "HW",
                                "departmentId", OTHER_DEPT_ID
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode node =
                objectMapper.readTree(response);
        org.assertj.core.api.Assertions.assertThat(node.get("departmentId").asText())
                .isEqualTo(TEST_DEPT_ID.toString());
    }

    // ── ADMIN_GERAL ──────────────────────────────────────────────────────────

    @Test
    void adminGeral_seesAllDemands() throws Exception {
        createDemand(TEST_DEPT_ID, "HW");
        createDemand(OTHER_DEPT_ID, "NET");

        mockMvc.perform(get("/api/v1/saged/demands").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void adminGeral_canAccessReports() throws Exception {
        mockMvc.perform(get("/api/v1/saged/demands/reports").with(adminJwt()))
                .andExpect(status().isOk());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void createDemand(UUID deptId, String specialtyCode) throws Exception {
        mockMvc.perform(post("/api/v1/saged/demands")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Test demand title here",
                                "description", "Some description that is long enough",
                                "specialtyCode", specialtyCode,
                                "departmentId", deptId
                        ))))
                .andExpect(status().isCreated());
    }
}
