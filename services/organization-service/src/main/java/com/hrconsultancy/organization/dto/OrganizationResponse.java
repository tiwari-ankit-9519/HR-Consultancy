package com.hrconsultancy.organization.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.hrconsultancy.organization.entity.Organization;

public class OrganizationResponse {
    private UUID id;
    private String name;
    private String legalName;
    private String gstin;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public OrganizationResponse() {
    }

    public OrganizationResponse(
            UUID id,
            String name,
            String legalName,
            String gstin,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.legalName = legalName;
        this.gstin = gstin;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrganizationResponse fromEntity(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getLegalName(),
                organization.getGstin(),
                organization.getStatus(),
                organization.getCreatedAt(),
                organization.getUpdatedAt());
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getGstin() {
        return gstin;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
