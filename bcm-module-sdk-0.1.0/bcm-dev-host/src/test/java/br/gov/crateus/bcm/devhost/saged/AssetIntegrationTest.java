package br.gov.crateus.bcm.devhost.saged;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AssetIntegrationTest extends SagedIntegrationTestBase {

    @Test
    void createAsset_adminGeral_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/saged/assets")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetTag":"PC-001","description":"Desktop HP EliteDesk"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetTag").value("PC-001"))
                .andExpect(jsonPath("$.lifecycleStatus").value("ACTIVE"));
    }

    @Test
    void createAsset_tecnico_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/saged/assets")
                        .with(tecnicoJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetTag":"PC-001","description":"Desktop HP"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAsset_duplicateTag_returns409() throws Exception {
        String body = """
                {"assetTag":"PC-002","description":"Notebook Lenovo"}
                """;

        mockMvc.perform(post("/api/v1/saged/assets")
                        .with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/saged/assets")
                        .with(adminJwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void listAssets_returnsPaged() throws Exception {
        createAsset("PC-010");
        createAsset("PC-011");

        mockMvc.perform(get("/api/v1/saged/assets").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAssetById_returnsOk() throws Exception {
        String id = createAsset("PC-020");

        mockMvc.perform(get("/api/v1/saged/assets/{id}", id).with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetTag").value("PC-020"));
    }

    @Test
    void deleteAsset_softDelete_setsInactive() throws Exception {
        String id = createAsset("PC-030");

        mockMvc.perform(delete("/api/v1/saged/assets/{id}", id).with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycleStatus").value("INACTIVE"));
    }

    @Test
    void deleteAsset_tecnico_returns403() throws Exception {
        String id = createAsset("PC-040");

        mockMvc.perform(delete("/api/v1/saged/assets/{id}", id).with(tecnicoJwt()))
                .andExpect(status().isForbidden());
    }

    // --- helper ---

    private String createAsset(String tag) throws Exception {
        String response = mockMvc.perform(post("/api/v1/saged/assets")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"assetTag":"%s","description":"Equipamento de teste"}
                                """, tag)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asText();
    }
}
