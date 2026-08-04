package br.gov.crateus.bcm.saged.infrastructure.entity;

import br.gov.crateus.bcm.sdk.persistence.SdkAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "saged", name = "demand_views")
public class DemandViewEntity extends SdkAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demand_id", nullable = false)
    private DemandEntity demand;

    @Column(name = "viewer_user_id", nullable = false)
    private UUID viewerUserId;

    @Column(name = "viewer_name", length = 255)
    private String viewerName;

    @Column(name = "viewed_at", nullable = false)
    private OffsetDateTime viewedAt;

    public DemandEntity getDemand() { return demand; }
    public void setDemand(DemandEntity demand) { this.demand = demand; }

    public UUID getViewerUserId() { return viewerUserId; }
    public void setViewerUserId(UUID viewerUserId) { this.viewerUserId = viewerUserId; }

    public String getViewerName() { return viewerName; }
    public void setViewerName(String viewerName) { this.viewerName = viewerName; }

    public OffsetDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(OffsetDateTime viewedAt) { this.viewedAt = viewedAt; }
}
