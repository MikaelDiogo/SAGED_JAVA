package br.gov.crateus.bcm.saged.api.dto;

import br.gov.crateus.bcm.saged.infrastructure.entity.UserSpecialtyEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public class UserSpecialtyResponse {

    private UUID id;
    private UUID userId;
    private UUID specialtyId;
    private String specialtyCode;
    private String specialtyName;
    private OffsetDateTime createdAt;
    private String createdBy;

    public static UserSpecialtyResponse from(UserSpecialtyEntity e) {
        var r = new UserSpecialtyResponse();
        r.id = e.getId();
        r.userId = e.getUserId();
        if (e.getSpecialty() != null) {
            r.specialtyId = e.getSpecialty().getId();
            r.specialtyCode = e.getSpecialty().getCode();
            r.specialtyName = e.getSpecialty().getName();
        }
        r.createdAt = e.getCreatedAt();
        r.createdBy = e.getCreatedBy();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getSpecialtyId() { return specialtyId; }
    public String getSpecialtyCode() { return specialtyCode; }
    public String getSpecialtyName() { return specialtyName; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
