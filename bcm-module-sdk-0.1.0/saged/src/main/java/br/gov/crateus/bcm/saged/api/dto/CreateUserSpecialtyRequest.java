package br.gov.crateus.bcm.saged.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateUserSpecialtyRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID specialtyId;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getSpecialtyId() { return specialtyId; }
    public void setSpecialtyId(UUID specialtyId) { this.specialtyId = specialtyId; }
}
