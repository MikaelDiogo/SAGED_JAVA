package br.gov.crateus.bcm.saged.infrastructure.entity;

import br.gov.crateus.bcm.sdk.persistence.SdkAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "saged", name = "user_specialties")
public class UserSpecialtyEntity extends SdkAuditableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private SpecialtyEntity specialty;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public SpecialtyEntity getSpecialty() { return specialty; }
    public void setSpecialty(SpecialtyEntity specialty) { this.specialty = specialty; }
}
