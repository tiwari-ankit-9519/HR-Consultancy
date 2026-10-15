package com.hrconsultancy.users.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.keycloak")
public record KeycloakProperties(
                @NotBlank String baseUrl,
                @NotBlank String realm,
                @NotBlank String clientId,
                @NotBlank String clientSecret) {
}