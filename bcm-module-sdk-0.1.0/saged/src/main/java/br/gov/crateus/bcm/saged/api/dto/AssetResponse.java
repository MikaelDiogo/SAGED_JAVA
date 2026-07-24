package br.gov.crateus.bcm.saged.api.dto;

import br.gov.crateus.bcm.saged.infrastructure.entity.AssetEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AssetResponse {

    private UUID id;
    private String assetTag;
    private String description;
    private String lifecycleStatus;
    private OffsetDateTime createdAt;
    private String createdBy;

    public static AssetResponse from(AssetEntity e) {
        var r = new AssetResponse();
        r.id = e.getId();
        r.assetTag = e.getAssetTag();
        r.description = e.getDescription();
        r.lifecycleStatus = e.getLifecycleStatus();
        r.createdAt = e.getCreatedAt();
        r.createdBy = e.getCreatedBy();
        return r;
    }

    public UUID getId() { return id; }
    public String getAssetTag() { return assetTag; }
    public String getDescription() { return description; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
