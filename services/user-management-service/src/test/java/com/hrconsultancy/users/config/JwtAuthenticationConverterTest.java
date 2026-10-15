package com.hrconsultancy.users.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter = new SecurityConfig().jwtAuthenticationConverter();

    @Test
    void convertsRealmRolesAndPreservesScopes() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("test-user-id")
                .claim("scope", "read write")
                .claim("realm_access", Map.of(
                        "roles", List.of("HR_ADMIN", "RECRUITER")))
                .build();

        Set<String> authorities = converter.convert(jwt)
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertTrue(authorities.contains("ROLE_HR_ADMIN"));
        assertTrue(authorities.contains("ROLE_RECRUITER"));
        assertTrue(authorities.contains("SCOPE_read"));
        assertTrue(authorities.contains("SCOPE_write"));
    }
}