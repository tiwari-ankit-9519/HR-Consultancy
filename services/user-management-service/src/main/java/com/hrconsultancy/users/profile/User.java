package com.hrconsultancy.users.profile;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profile", schema = "user_management")
public class User {

    @Id
    private UUID id;

    @Column(name = "keycloak_user_id", nullable = false, unique = true, length = 255)
    private String keycloakUserId;

    @Column(nullable = false, length = 255)
    private String username;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(length = 100)
    private String department;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "employee_code", length = 100)
    private String employeeCode;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected User() {
        // Required by JPA.
    }

    public User(
            String keycloakUserId,
            String username,
            String firstName,
            String lastName,
            String email) {

        this.id = UUID.randomUUID();
        this.keycloakUserId = requireText(keycloakUserId, "keycloakUserId");
        updateIdentity(username, firstName, lastName, email);
    }

    public void updateIdentity(
            String username,
            String firstName,
            String lastName,
            String email) {

        this.username = requireText(username, "username");
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.email = requireText(email, "email");
    }

    public void updateProfile(
            String phoneNumber,
            String jobTitle,
            String department,
            UUID organizationId,
            String employeeCode) {

        this.phoneNumber = phoneNumber;
        this.jobTitle = jobTitle;
        this.department = department;
        this.organizationId = organizationId;
        this.employeeCode = employeeCode;
    }

    public void deactivate() {
        this.status = "INACTIVE";
        this.deactivatedAt = Instant.now();
    }

    public void activate() {
        this.status = "ACTIVE";
        this.deactivatedAt = null;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value;
    }

    public UUID getId() {
        return id;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }
}