package br.gov.crateus.bcm.saged.api.dto;

import br.gov.crateus.bcm.saged.domain.DemandStatus;
import jakarta.validation.constraints.NotNull;

public class ChangeStatusRequest {

    @NotNull
    private DemandStatus status;

    private String justification;

    public DemandStatus getStatus() { return status; }
    public void setStatus(DemandStatus status) { this.status = status; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
}
