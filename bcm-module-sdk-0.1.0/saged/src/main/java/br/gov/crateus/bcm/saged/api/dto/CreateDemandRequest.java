package br.gov.crateus.bcm.saged.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class CreateDemandRequest {

    @NotBlank
    @Size(min = 12, message = "Título deve ter no mínimo 12 caracteres")
    private String title;

    @NotBlank
    @Size(min = 20, message = "Descrição deve ter no mínimo 20 caracteres")
    private String description;

    @NotBlank
    private String specialtyCode;

    private String assetTag;

    private UUID requesterUserId;

    private UUID departmentId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSpecialtyCode() { return specialtyCode; }
    public void setSpecialtyCode(String specialtyCode) { this.specialtyCode = specialtyCode; }

    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public UUID getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(UUID requesterUserId) { this.requesterUserId = requesterUserId; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
}
