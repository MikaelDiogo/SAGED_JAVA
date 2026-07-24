package br.gov.crateus.bcm.saged.infrastructure.entity;

import br.gov.crateus.bcm.sdk.persistence.SdkAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(schema = "saged", name = "assets")
public class AssetEntity extends SdkAuditableEntity {

    @Column(name = "asset_tag", length = 128, nullable = false, unique = true)
    private String assetTag;

    @Column(name = "description", length = 512)
    private String description;

    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
