package br.gov.crateus.bcm.devhost.saged;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

class UserSpecialtyIntegrationTest extends SagedIntegrationTestBase {

    private static final UUID USER_A = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID USER_B = UUID.fromString("00000000-0000-0000-0000-000000000011");

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private UUID specialtyId;

    @BeforeEach
    void setUpSpecialty() {
        SpecialtyEntity s = new SpecialtyEntity();
        s.setCode("HW");
        s.setName("Hardware");
        specialtyId = specialtyRepository.save(s).getId();
    }

    @Test
    void listUserSpecialties_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/saged/user-specialties").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void assignUserToSpecialty_adminGeral_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/saged/user-specialties")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", USER_A, "specialtyId", specialtyId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(USER_A.toString()))
                .andExpect(jsonPath("$.specialtyCode").value("HW"));
    }

    @Test
    void assignUserToSpecialty_tecnico_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/saged/user-specialties")
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", USER_A, "specialtyId", specialtyId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignUserToSpecialty_duplicate_returns409() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "userId", USER_A, "specialtyId", specialtyId));

        mockMvc.perform(post("/api/v1/saged/user-specialties")
                        .with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/saged/user-specialties")
                        .with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void assignUserToSpecialty_unknownSpecialty_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/saged/user-specialties")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", USER_A, "specialtyId", UUID.randomUUID()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listByUserId_returnsOnlyThatUser() throws Exception {
        assign(USER_A);
        assign(USER_B);

        mockMvc.perform(get("/api/v1/saged/user-specialties?userId=" + USER_A).with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(USER_A.toString()));
    }

    @Test
    void listBySpecialtyId_returnsAllUsersForSpecialty() throws Exception {
        assign(USER_A);
        assign(USER_B);

        mockMvc.perform(get("/api/v1/saged/user-specialties?specialtyId=" + specialtyId).with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void deleteAssignment_adminGeral_returns204() throws Exception {
        String id = assign(USER_A);

        mockMvc.perform(delete("/api/v1/saged/user-specialties/{id}", id).with(adminJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/saged/user-specialties?userId=" + USER_A).with(adminJwt()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deleteAssignment_tecnico_returns403() throws Exception {
        String id = assign(USER_A);

        mockMvc.perform(delete("/api/v1/saged/user-specialties/{id}", id).with(tecnicoJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAssignment_notFound_returns400() throws Exception {
        mockMvc.perform(delete("/api/v1/saged/user-specialties/{id}", UUID.randomUUID())
                        .with(adminJwt()))
                .andExpect(status().isBadRequest());
    }

    // --- helper ---

    private String assign(UUID userId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/saged/user-specialties")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId, "specialtyId", specialtyId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }
}
