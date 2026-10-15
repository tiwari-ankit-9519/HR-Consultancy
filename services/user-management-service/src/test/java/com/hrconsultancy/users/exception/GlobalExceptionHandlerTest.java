package com.hrconsultancy.users.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hrconsultancy.users.service.KeycloakAdminException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void missingUserReturnsNotFound() {
        ResponseEntity<Map<String, Object>> response = handler.handleKeycloakError(
                new KeycloakAdminException(
                        "User not found", 404, "USER_LOOKUP"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("USER_NOT_FOUND", response.getBody().get("code"));
    }

    @Test
    void duplicateUserReturnsConflict() {
        ResponseEntity<Map<String, Object>> response = handler.handleKeycloakError(
                new KeycloakAdminException(
                        "User already exists", 409, "USER_CREATE"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("USER_CONFLICT", response.getBody().get("code"));
    }

    @Test
    void forbiddenUpstreamOperationReturnsSafeGatewayError() {
        ResponseEntity<Map<String, Object>> response = handler.handleKeycloakError(
                new KeycloakAdminException(
                        "Forbidden", 403, "ROLE_LOOKUP"));

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                response.getStatusCode());

        assertEquals(
                "IDENTITY_PROVIDER_ERROR",
                response.getBody().get("code"));

        assertEquals(
                "The identity provider could not complete the request.",
                response.getBody().get("message"));
    }

    @Test
    void malformedRequestReturnsBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.handleUnreadableRequest();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST", response.getBody().get("code"));
    }

    @Test
    void unexpectedErrorReturnsGenericInternalError() {
        ResponseEntity<Map<String, Object>> response = handler.handleUnexpectedError(
                new IllegalStateException("Internal implementation detail"));

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());

        assertEquals("INTERNAL_ERROR", response.getBody().get("code"));

        assertNotNull(response.getBody());
        assertEquals(
                "An unexpected error occurred.",
                response.getBody().get("message"));
    }
}