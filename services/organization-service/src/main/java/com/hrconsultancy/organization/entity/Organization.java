package com.hrconsultancy.organization.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations", schema = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(length = 15)
    private String gstin;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Organization() {
    }

    // Constructor for creating an organization
    public Organization(String name, String legalName, String gstin) {
        this.name = name;
        this.legalName = legalName;
        this.gstin = gstin;
        this.status = "ACTIVE";
    }

    // Getters

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

    // Setters

    public void setName(String name) {
        this.name = name;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
