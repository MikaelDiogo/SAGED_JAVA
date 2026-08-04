package br.gov.crateus.bcm.saged.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class PreRegisterTelegramRequest {

    @NotBlank
    @Size(max = 64)
    private String phoneNumber;

    @NotBlank
    @Size(max = 255)
    private String displayName;

    private UUID departmentId;

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
}
