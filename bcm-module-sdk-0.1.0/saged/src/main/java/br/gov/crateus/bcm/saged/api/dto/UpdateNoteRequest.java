package br.gov.crateus.bcm.saged.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateNoteRequest {

    @NotBlank
    @Size(max = 3000, message = "Nota técnica deve ter no máximo 3000 caracteres")
    private String note;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
