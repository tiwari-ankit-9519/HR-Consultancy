package com.hrconsultancy.users.service;

public class KeycloakAdminException extends RuntimeException {

    private final int statusCode;
    private final String operation;

    public KeycloakAdminException(String message) {
        this(message, 502, "UNKNOWN");
    }

    public KeycloakAdminException(String message, int statusCode) {
        this(message, statusCode, "UNKNOWN");
    }

    public KeycloakAdminException(
            String message,
            int statusCode,
            String operation) {

        super(message);
        this.statusCode = statusCode;
        this.operation = operation;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getOperation() {
        return operation;
    }
}