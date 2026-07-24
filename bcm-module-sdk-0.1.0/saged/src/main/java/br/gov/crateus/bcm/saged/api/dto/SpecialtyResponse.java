package br.gov.crateus.bcm.saged.api.dto;

import br.gov.crateus.bcm.saged.infrastructure.entity.SpecialtyEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public class SpecialtyResponse {

    private UUID id;
    private String code;
    private String name;
    private OffsetDateTime createdAt;

    public static SpecialtyResponse from(SpecialtyEntity e) {
        var r = new SpecialtyResponse();
        r.id = e.getId();
        r.code = e.getCode();
        r.name = e.getName();
        r.createdAt = e.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
