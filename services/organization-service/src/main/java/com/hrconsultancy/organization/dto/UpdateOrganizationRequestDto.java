package com.hrconsultancy.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateOrganizationRequestDto {

    @NotBlank(message = "Organization name is required")
    @Size(max = 200, message = "Organization name must not exceed 200 characters")
    private String name;

    @Size(max = 200, message = "Legal name must not exceed 200 characters")
    private String legalName;

    @Size(max = 15, message = "GSTIN must not exceed 15 characters")
    private String gstin;

    public UpdateOrganizationRequestDto() {
    }

    public UpdateOrganizationRequestDto(
            String name,
            String legalName,
            String gstin) {
        this.name = name;
        this.legalName = legalName;
        this.gstin = gstin;
    }

    public String getName() {
        return name;
    }

    public String getLegalname() {
        return legalName;
    }

    public String getGstin() {
        return gstin;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLegalname(String legalName) {
        this.legalName = legalName;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }
}
