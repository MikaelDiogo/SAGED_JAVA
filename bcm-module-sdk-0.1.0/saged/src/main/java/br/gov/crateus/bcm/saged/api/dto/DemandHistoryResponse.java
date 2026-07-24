package br.gov.crateus.bcm.saged.api.dto;

import br.gov.crateus.bcm.saged.infrastructure.DemandHistoryEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DemandHistoryResponse {

    private UUID id;
    private UUID demandId;
    private String action;
    private String justification;
    private OffsetDateTime createdAt;
    private String createdBy;

    public static DemandHistoryResponse from(DemandHistoryEntity h) {
        var r = new DemandHistoryResponse();
        r.id = h.getId();
        r.demandId = h.getDemand().getId();
        r.action = h.getAction();
        r.justification = h.getJustification();
        r.createdAt = h.getCreatedAt();
        r.createdBy = h.getCreatedBy();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getDemandId() { return demandId; }
    public String getAction() { return action; }
    public String getJustification() { return justification; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
