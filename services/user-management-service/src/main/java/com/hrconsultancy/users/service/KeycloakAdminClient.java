
package com.hrconsultancy.users.service;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.hrconsultancy.users.config.KeycloakProperties;
import com.hrconsultancy.users.dto.CreateUserRequest;

@Component
public class KeycloakAdminClient {

    private static final Logger logger = LogManager.getLogger(KeycloakAdminClient.class);

    private final RestClient restClient;
    private final KeycloakProperties properties;

    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public KeycloakAdminClient(
            RestClient.Builder restClientBuilder,
            KeycloakProperties properties) {

        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    private synchronized String getAccessToken() {
        if (cachedAccessToken != null
                && Instant.now().isBefore(tokenExpiresAt.minusSeconds(30))) {
            return cachedAccessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token",
                            properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response == null
                    || !(response.get("access_token") instanceof String token)
                    || token.isBlank()) {

                logger.error("Keycloak token endpoint returned no usable access token");

                throw new KeycloakAdminException(
                        "Keycloak did not return an access token.",
                        502,
                        "TOKEN_ACQUISITION");
            }

            Object expiresIn = response.get("expires_in");
            long expiresSeconds = expiresIn instanceof Number number
                    ? number.longValue()
                    : 60L;

            if (expiresSeconds <= 0) {
                throw new KeycloakAdminException(
                        "Keycloak returned an invalid token lifetime.",
                        502,
                        "TOKEN_ACQUISITION");
            }

            cachedAccessToken = token;
            tokenExpiresAt = Instant.now().plusSeconds(expiresSeconds);

            logger.debug("Keycloak service access token acquired");

            return cachedAccessToken;

        } catch (RestClientResponseException ex) {
            logger.error(
                    "Keycloak service authentication failed; status={}",
                    ex.getStatusCode().value());

            throw new KeycloakAdminException(
                    "Could not authenticate the service with Keycloak.",
                    502,
                    "TOKEN_ACQUISITION");

        } catch (RestClientException ex) {
            logger.error(
                    "Keycloak token endpoint communication failed; exceptionType={}",
                    ex.getClass().getSimpleName());

            throw new KeycloakAdminException(
                    "Could not communicate with Keycloak.",
                    502,
                    "TOKEN_ACQUISITION");
        }
    }

    public String createUser(CreateUserRequest request) {
        Map<String, Object> representation = new LinkedHashMap<>();
        representation.put("username", request.username());
        representation.put("email", request.email());
        representation.put("firstName", request.firstName());
        representation.put("lastName", request.lastName());
        representation.put("enabled", true);
        representation.put("emailVerified", false);

        try {
            URI location = restClient.post()
                    .uri("/admin/realms/{realm}/users", properties.realm())
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(representation)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();

            if (location == null || location.getPath() == null) {
                logger.error("Keycloak user creation returned no Location header");

                throw new KeycloakAdminException(
                        "Keycloak did not return the created user location.",
                        502,
                        "USER_CREATE");
            }

            String path = location.getPath();
            String userId = path.substring(path.lastIndexOf('/') + 1);

            if (userId.isBlank()) {
                throw new KeycloakAdminException(
                        "Keycloak returned an invalid user location.",
                        502,
                        "USER_CREATE");
            }

            logger.info("Keycloak user created; userId={}", userId);
            return userId;

        } catch (RestClientResponseException ex) {
            logger.error(
                    "Keycloak user creation failed; status={}",
                    ex.getStatusCode().value());

            throw translate(ex, "Could not create the Keycloak user.",
                    "USER_CREATE");

        } catch (RestClientException ex) {
            logger.error(
                    "Keycloak user creation communication failed; exceptionType={}",
                    ex.getClass().getSimpleName());

            throw new KeycloakAdminException(
                    "Could not communicate with Keycloak.",
                    502,
                    "USER_CREATE");
        }
    }

    public Map<String, Object> getUser(String userId) {
        try {
            Map<String, Object> user = restClient.get()
                    .uri("/admin/realms/{realm}/users/{id}",
                            properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (user == null) {
                throw new KeycloakAdminException(
                        "Keycloak returned no user representation.",
                        502,
                        "USER_LOOKUP");
            }

            return user;

        } catch (RestClientResponseException ex) {
            logger.warn(
                    "Keycloak user lookup failed; status={}",
                    ex.getStatusCode().value());

            throw translate(ex, "Could not retrieve the Keycloak user.",
                    "USER_LOOKUP");

        } catch (RestClientException ex) {
            throw new KeycloakAdminException(
                    "Could not communicate with Keycloak.",
                    502,
                    "USER_LOOKUP");
        }
    }

    public List<Map<String, Object>> listUsers(int first, int max) {
        try {
            List<Map<String, Object>> users = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/{realm}/users")
                            .queryParam("first", first)
                            .queryParam("max", max)
                            .build(properties.realm()))
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return users == null ? List.of() : users;

        } catch (RestClientResponseException ex) {
            logger.error(
                    "Keycloak user listing failed; status={}",
                    ex.getStatusCode().value());

            throw translate(ex, "Could not list Keycloak users.",
                    "USER_LIST");

        } catch (RestClientException ex) {
            throw new KeycloakAdminException(
                    "Could not communicate with Keycloak.",
                    502,
                    "USER_LIST");
        }
    }

    public Set<String> getUserRoles(String userId) {
        try {
            List<Map<String, Object>> roles = restClient.get()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm",
                            properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            Set<String> names = new LinkedHashSet<>();

            if (roles != null) {
                for (Map<String, Object> role : roles) {
                    Object name = role.get("name");

                    if (name instanceof String roleName) {
                        names.add(roleName);
                    }
                }
            }

            return names;

        } catch (RestClientResponseException ex) {
            throw translate(ex, "Could not retrieve user roles.",
                    "ROLE_LOOKUP");

        } catch (RestClientException ex) {
            throw new KeycloakAdminException(
                    "Could not communicate with Keycloak.",
                    502,
                    "ROLE_LOOKUP");
        }
    }

    public void assignRoles(String userId, Set<String> roleNames) {
        List<Map<String, Object>> representations = new ArrayList<>();

        for (String roleName : roleNames) {
            try {
                Map<String, Object> role = restClient.get()
                        .uri("/admin/realms/{realm}/roles/{roleName}",
                                properties.realm(), roleName)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {
                        });

                if (role == null || !(role.get("id") instanceof String)) {
                    throw new KeycloakAdminException(
                            "A requested Keycloak role was not found.",
                            502,
                            "ROLE_LOOKUP");
                }

                representations.add(role);

            } catch (RestClientResponseException ex) {
                throw translate(ex, "Could not resolve a requested Keycloak role.",
                        "ROLE_LOOKUP");

            } catch (RestClientException ex) {
                throw new KeycloakAdminException(
                        "Could not communicate with Keycloak.",
                        502,
                        "ROLE_LOOKUP");
            }
        }

        if (representations.isEmpty()) {
            return;
        }

        try {
            restClient.post()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm",
                            properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(representations)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("Keycloak realm roles assigned; userId={}", userId);

        } catch (RestClientResponseException ex) {
            logger.error(
                    "Keycloak role assignment failed; status={}",
                    ex.getStatusCode().value());

            throw translate(ex, "Could not assign Keycloak roles.",
                    "ROLE_ASSIGNMENT");

        } catch (RestClientException ex) {
            throw new KeycloakAdminException(
                    "Could not communicate with Keycloak.",
                    502,
                    "ROLE_ASSIGNMENT");
        }
    }

    public void deleteUser(String userId) {
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}",
                            properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .retrieve()
                    .toBodilessEntity();

            logger.info("Keycloak user deleted; userId={}", userId);

        } catch (RestClientResponseException ex) {
            logger.error(
                    "Keycloak user deletion failed; status={}",
                    ex.getStatusCode().value());

            throw translate(ex, "Could not delete the Keycloak user.",
                    "USER_DELETE");

        } catch (RestClientException ex) {
            throw new KeycloakAdminException(
                    "Could not communicate with Keycloak.",
                    502,
                    "USER_DELETE");
        }
    }

    private String bearerToken() {
        return "Bearer " + getAccessToken();
    }

    private KeycloakAdminException translate(
            RestClientResponseException ex,
            String message,
            String operation) {

        return new KeycloakAdminException(
                message,
                ex.getStatusCode().value(),
                operation);
    }
}