package br.gov.crateus.bcm.saged.api.dto;

import br.gov.crateus.bcm.saged.infrastructure.entity.OrgUnitEntity;
import java.util.UUID;

public class OrgUnitResponse {

    private UUID id;
    private String code;
    private String name;

    public static OrgUnitResponse from(OrgUnitEntity e) {
        var r = new OrgUnitResponse();
        r.id = e.getId();
        r.code = e.getCode();
        r.name = e.getName();
        return r;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
