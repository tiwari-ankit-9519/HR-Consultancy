package com.hrconsultancy.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateOrganizationRequestDto {
    @NotBlank(message = "Organization name is required")
    @Size(max = 200, message = "Organization name cannot exceed 200 characters")
    private String name;

    @Size(max = 200, message = "Legal name cannot exceed 200 characters")
    private String legalName;

    @Size(max = 15, message = "GSTIN cannot exceed 15 characters")
    private String gstin;

    public CreateOrganizationRequestDto() {
    }

    public CreateOrganizationRequestDto(String name, String legalName, String gstin) {
        this.name = name;
        this.legalName = legalName;
        this.gstin = gstin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }
}
