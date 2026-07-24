package br.gov.crateus.bcm.saged.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateSpecialtyRequest {

    @NotBlank
    @Size(max = 32)
    private String code;

    @NotBlank
    @Size(max = 128)
    private String name;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
