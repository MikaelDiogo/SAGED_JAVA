package br.gov.crateus.bcm.devhost.saged;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SpecialtyIntegrationTest extends SagedIntegrationTestBase {

    @Test
    void listSpecialties_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/saged/specialties").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createSpecialty_adminGeral_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/saged/specialties")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"HW","name":"Hardware"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("HW"))
                .andExpect(jsonPath("$.name").value("Hardware"));
    }

    @Test
    void createSpecialty_lowercodeIsNormalized() throws Exception {
        mockMvc.perform(post("/api/v1/saged/specialties")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"sw","name":"Software"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SW"));
    }

    @Test
    void createSpecialty_tecnico_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/saged/specialties")
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"HW","name":"Hardware"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSpecialty_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/saged/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"HW","name":"Hardware"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSpecialty_duplicateCode_returns409() throws Exception {
        String body = """
                {"code":"HW","name":"Hardware"}
                """;

        mockMvc.perform(post("/api/v1/saged/specialties")
                        .with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/saged/specialties")
                        .with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}
