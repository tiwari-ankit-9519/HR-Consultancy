package com.hrconsultancy.users.dto;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
                @NotBlank @Size(min = 3, max = 100) @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may contain letters, numbers, dots, underscores and hyphens only") String username,

                @NotBlank @Email @Size(max = 254) String email,

                @NotBlank @Size(max = 100) String firstName,

                @NotBlank @Size(max = 100) String lastName,

                @NotEmpty Set<@NotBlank @Pattern(regexp = "^(HR_MANAGER|RECRUITER)$", message = "Only HR_MANAGER and RECRUITER can be assigned through this endpoint") String> roles,

                @Size(max = 30) String phoneNumber,

                @Size(max = 100) String jobTitle,

                @Size(max = 100) String department,

                UUID organizationId,

                @Size(max = 100) String employeeCode) {

        public CreateUserRequest(
                        String username,
                        String email,
                        String firstName,
                        String lastName,
                        Set<String> roles) {
                this(
                                username,
                                email,
                                firstName,
                                lastName,
                                roles,
                                null,
                                null,
                                null,
                                null,
                                null);
        }
}
