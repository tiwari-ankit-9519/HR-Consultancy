package com.hrconsultancy.users.dto;

import java.util.Set;

public record UserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Set<String> roles) {
}