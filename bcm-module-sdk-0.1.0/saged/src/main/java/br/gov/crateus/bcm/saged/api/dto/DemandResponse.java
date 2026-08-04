package br.gov.crateus.bcm.saged.api.dto;

import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DemandResponse {

    private UUID id;
    private String protocol;
    private String title;
    private String description;
    private DemandStatus status;
    private UUID requesterUserId;
    private UUID departmentId;
    private UUID specialtyId;
    private String specialtyCode;
    private String specialtyName;
    private String assetTag;
    private String currentTechnicalNote;
    private UUID assigneeUserId;
    private String assigneeName;
    private String equipmentName;
    private String equipmentModel;
    private Boolean isRented;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
    private long version;

    public static DemandResponse from(DemandEntity d) {
        var r = new DemandResponse();
        r.id = d.getId();
        r.protocol = d.getProtocol();
        r.title = d.getTitle();
        r.description = d.getDescription();
        r.status = d.getStatus();
        r.requesterUserId = d.getRequesterUserId();
        r.departmentId = d.getDepartmentId();
        if (d.getSpecialty() != null) {
            r.specialtyId = d.getSpecialty().getId();
            r.specialtyCode = d.getSpecialty().getCode();
            r.specialtyName = d.getSpecialty().getName();
        }
        r.assetTag = d.getAssetTag();
        r.currentTechnicalNote = d.getCurrentTechnicalNote();
        r.assigneeUserId = d.getAssigneeUserId();
        r.assigneeName = d.getAssigneeName();
        r.equipmentName = d.getEquipmentName();
        r.equipmentModel = d.getEquipmentModel();
        r.isRented = d.getIsRented();
        r.createdAt = d.getCreatedAt();
        r.updatedAt = d.getUpdatedAt();
        r.createdBy = d.getCreatedBy();
        r.version = d.getVersion();
        return r;
    }

    public UUID getId() { return id; }
    public String getProtocol() { return protocol; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public DemandStatus getStatus() { return status; }
    public UUID getRequesterUserId() { return requesterUserId; }
    public UUID getDepartmentId() { return departmentId; }
    public UUID getSpecialtyId() { return specialtyId; }
    public String getSpecialtyCode() { return specialtyCode; }
    public String getSpecialtyName() { return specialtyName; }
    public String getAssetTag() { return assetTag; }
    public String getCurrentTechnicalNote() { return currentTechnicalNote; }
    public UUID getAssigneeUserId() { return assigneeUserId; }
    public String getAssigneeName() { return assigneeName; }
    public String getEquipmentName() { return equipmentName; }
    public String getEquipmentModel() { return equipmentModel; }
    public Boolean getIsRented() { return isRented; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public long getVersion() { return version; }
}
