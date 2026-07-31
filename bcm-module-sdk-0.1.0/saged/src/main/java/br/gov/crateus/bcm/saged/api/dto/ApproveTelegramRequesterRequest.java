package br.gov.crateus.bcm.saged.api.dto;

import java.util.UUID;

public class ApproveTelegramRequesterRequest {

    private UUID departmentId;

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
}
