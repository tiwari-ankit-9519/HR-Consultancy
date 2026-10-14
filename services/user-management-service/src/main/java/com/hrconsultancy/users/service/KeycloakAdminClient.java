package com.hrconsultancy.users.service;

import java.time.Instant;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.hrconsultancy.users.config.KeycloakProperties;

@Component
public class KeycloakAdminClient {
    private static final Logger logger = LogManager.getLogger(KeycloakAdminClient.class);
    private final RestClient restClient;
    private final KeycloakProperties properties;

    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    private KeycloakAdminClient(
            RestClient.Builder restClientBuilder,
            KeycloakProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    private synchronized String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(30))) {
            return cachedAccessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            if (response == null || !(response.get("access_token") instanceof String token) || token.isBlank()) {
                logger.error("Keycloak did not return an access token.", 502, "TOKEN_ACQUISITION");
            }

            Object expiresIn = response.get("expires_in");
            long expiresSeconds = expiresIn
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
}