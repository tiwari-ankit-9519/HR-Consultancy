package com.hrconsultancy.users.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
                String id,
                UUID profileId,
                String username,
                String email,
                String firstName,
                String lastName,
                boolean enabled,
                Set<String> roles,

                String phoneNumber,
                String jobTitle,
                String department,
                UUID organizationId,
                String employeeCode,
                String status,
                Instant createdAt,
                Instant updatedAt,
                Instant deactivatedAt) {
        public UserResponse {
                roles = roles == null ? Set.of() : Set.copyOf(roles);
        }
}
