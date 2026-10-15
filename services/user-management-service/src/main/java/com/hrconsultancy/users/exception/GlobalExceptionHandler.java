package com.hrconsultancy.users.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hrconsultancy.users.service.KeycloakAdminException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(
                        MethodArgumentNotValidException ex) {

                String details = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": "
                                                + error.getDefaultMessage())
                                .distinct()
                                .collect(Collectors.joining("; "));

                return errorResponse(
                                HttpStatus.BAD_REQUEST,
                                "VALIDATION_ERROR",
                                details);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<Map<String, Object>> handleConstraintViolation(
                        ConstraintViolationException ex) {

                return errorResponse(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_PARAMETER",
                                "One or more request parameters are invalid.");
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<Map<String, Object>> handleUnreadableRequest() {
                return errorResponse(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_REQUEST",
                                "The request body is missing or invalid.");
        }

        @ExceptionHandler(KeycloakAdminException.class)
        public ResponseEntity<Map<String, Object>> handleKeycloakError(
                        KeycloakAdminException ex) {

                int upstreamStatus = ex.getStatusCode();
                String operation = ex.getOperation();

                if ("USER_LOOKUP".equals(operation) && upstreamStatus == 404) {
                        return errorResponse(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "The requested user was not found.");
                }

                if ("USER_CREATE".equals(operation) && upstreamStatus == 409) {
                        return errorResponse(
                                        HttpStatus.CONFLICT,
                                        "USER_CONFLICT",
                                        "An account with conflicting details may already exist.");
                }

                logger.error(
                                "Identity provider operation failed; operation={}; upstreamStatus={}",
                                operation,
                                upstreamStatus);

                return errorResponse(
                                HttpStatus.BAD_GATEWAY,
                                "IDENTITY_PROVIDER_ERROR",
                                "The identity provider could not complete the request.");
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleUnexpectedError(
                        Exception ex) {

                logger.error(
                                "Unexpected user-management service error; exceptionType={}",
                                ex.getClass().getSimpleName(),
                                ex);

                return errorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "INTERNAL_ERROR",
                                "An unexpected error occurred.");
        }

        private ResponseEntity<Map<String, Object>> errorResponse(
                        HttpStatus status,
                        String code,
                        String message) {

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("status", status.value());
                body.put("code", code);
                body.put("message", message);

                return ResponseEntity.status(status).body(body);
        }
}