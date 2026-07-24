package br.gov.crateus.bcm.saged.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateAssetRequest {

    @NotBlank
    @Size(max = 128)
    private String assetTag;

    @Size(max = 512)
    private String description;

    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
