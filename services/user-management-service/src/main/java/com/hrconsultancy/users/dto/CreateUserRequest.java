package com.hrconsultancy.users.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username contains unsupported characters") String username,
        @NotBlank @Email @Size(max = 254) String email,

        @NotBlank @Size(max = 100) String firstName,

        @NotBlank @Size(max = 100) String lastName,

        @NotEmpty Set<@NotBlank @Pattern(regexp = "^(HR_MANAGER|RECRUITER)$") String> roles) {
}