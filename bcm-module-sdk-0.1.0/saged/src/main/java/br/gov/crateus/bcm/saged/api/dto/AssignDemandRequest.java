package br.gov.crateus.bcm.saged.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AssignDemandRequest {

    @NotNull
    private UUID assigneeUserId;

    public UUID getAssigneeUserId() { return assigneeUserId; }
    public void setAssigneeUserId(UUID assigneeUserId) { this.assigneeUserId = assigneeUserId; }
}
