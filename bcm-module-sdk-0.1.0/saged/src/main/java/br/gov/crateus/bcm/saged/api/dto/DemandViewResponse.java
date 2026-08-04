package br.gov.crateus.bcm.saged.api.dto;

import br.gov.crateus.bcm.saged.infrastructure.entity.DemandViewEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DemandViewResponse {

    private UUID viewerUserId;
    private String viewerName;
    private OffsetDateTime viewedAt;

    public static DemandViewResponse from(DemandViewEntity e) {
        var r = new DemandViewResponse();
        r.viewerUserId = e.getViewerUserId();
        r.viewerName = e.getViewerName();
        r.viewedAt = e.getViewedAt();
        return r;
    }

    public UUID getViewerUserId() { return viewerUserId; }
    public String getViewerName() { return viewerName; }
    public OffsetDateTime getViewedAt() { return viewedAt; }
}
