package br.gov.crateus.bcm.saged.api.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateNoteRequest {

    @NotBlank
    private String note;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
