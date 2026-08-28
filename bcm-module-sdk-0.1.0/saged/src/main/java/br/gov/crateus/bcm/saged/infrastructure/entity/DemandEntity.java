package br.gov.crateus.bcm.saged.infrastructure.entity;

import br.gov.crateus.bcm.saged.infrastructure.entity.SagedAuditableEntity;
import br.gov.crateus.bcm.saged.domain.DemandStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "saged", name = "demands")
public class DemandEntity extends SagedAuditableEntity {

    @Column(name = "protocol", length = 64, nullable = false, unique = true)
    private String protocol;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 64, nullable = false)
    private DemandStatus status;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private SpecialtyEntity specialty;

    @Column(name = "asset_tag", length = 128)
    private String assetTag;

    @Column(name = "current_technical_note", columnDefinition = "TEXT")
    private String currentTechnicalNote;

    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    @Column(name = "equipment_name", length = 255)
    private String equipmentName;

    @Column(name = "equipment_model", length = 128)
    private String equipmentModel;

    @Column(name = "is_rented")
    private Boolean isRented;

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public DemandStatus getStatus() { return status; }
    public void setStatus(DemandStatus status) { this.status = status; }

    public UUID getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(UUID requesterUserId) { this.requesterUserId = requesterUserId; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public SpecialtyEntity getSpecialty() { return specialty; }
    public void setSpecialty(SpecialtyEntity specialty) { this.specialty = specialty; }

    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public String getCurrentTechnicalNote() { return currentTechnicalNote; }
    public void setCurrentTechnicalNote(String currentTechnicalNote) { this.currentTechnicalNote = currentTechnicalNote; }

    public UUID getAssigneeUserId() { return assigneeUserId; }
    public void setAssigneeUserId(UUID assigneeUserId) { this.assigneeUserId = assigneeUserId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getEquipmentModel() { return equipmentModel; }
    public void setEquipmentModel(String equipmentModel) { this.equipmentModel = equipmentModel; }

    public Boolean getIsRented() { return isRented; }
    public void setIsRented(Boolean isRented) { this.isRented = isRented; }
}
