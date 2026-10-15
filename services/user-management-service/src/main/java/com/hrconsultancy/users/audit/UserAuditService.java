package com.hrconsultancy.users.audit;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrconsultancy.users.dto.UserAuditResponse;

@Service
public class UserAuditService {

    private static final Logger logger = LogManager.getLogger(UserAuditService.class);

    private final UserAuditRepository repository;

    public UserAuditService(UserAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserAuditResponse record(
            String targetUserId,
            String action,
            String outcome,
            Map<String, Object> details) {

        Actor actor = currentActor();

        String requestId = ThreadContext.get("requestId");

        UserAuditRecord record = new UserAuditRecord(
                UUID.randomUUID(),
                targetUserId,
                action,
                outcome,
                actor.id(),
                actor.username(),
                requestId == null || requestId.isBlank() ? null : requestId,
                safeDetails(details),
                Instant.now());

        // Flush now so persistence errors are raised during this call.
        UserAuditRecord saved = repository.saveAndFlush(record);

        logger.info(
                "User audit recorded targetUserId={} action={} outcome={}",
                targetUserId,
                action,
                outcome);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserAuditResponse> getHistory(String targetUserId) {
        return repository
                .findByTargetUserIdOrderByOccurredAtDesc(targetUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Actor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String subject = jwtAuth.getToken().getSubject();
            String username = jwtAuth.getToken()
                    .getClaimAsString("preferred_username");

            return new Actor(subject, username);
        }

        if (authentication != null && authentication.isAuthenticated()) {
            return new Actor(null, authentication.getName());
        }

        return new Actor("SYSTEM", "SYSTEM");
    }

    private Map<String, Object> safeDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> sanitized = new LinkedHashMap<>();

        details.forEach((key, value) -> {
            if (key == null) {
                return;
            }

            sanitized.put(key, isSensitiveKey(key)
                    ? "[REDACTED]"
                    : sanitizeValue(value));
        });

        return Collections.unmodifiableMap(sanitized);
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);

        return normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("authorization")
                || normalized.contains("credential")
                || normalized.contains("cookie")
                || normalized.contains("email")
                || normalized.contains("phone")
                || normalized.contains("mobile")
                || normalized.contains("aadhaar")
                || normalized.contains("aadhar")
                || normalized.contains("pan")
                || normalized.contains("dateofbirth")
                || normalized.equals("dob");
    }

    private Object sanitizeValue(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();

            map.forEach((key, nestedValue) -> {
                if (key instanceof String stringKey) {
                    sanitized.put(stringKey, isSensitiveKey(stringKey)
                            ? "[REDACTED]"
                            : sanitizeValue(nestedValue));
                }
            });

            return Collections.unmodifiableMap(sanitized);
        }

        if (value instanceof Collection<?> collection) {
            List<Object> sanitized = new ArrayList<>(collection.size());

            for (Object item : collection) {
                sanitized.add(sanitizeValue(item));
            }

            return Collections.unmodifiableList(sanitized);
        }

        // Avoid serializing arbitrary objects into audit records.
        return "[OMITTED]";
    }

    private UserAuditResponse toResponse(UserAuditRecord record) {
        return new UserAuditResponse(
                record.getId(),
                record.getTargetUserId(),
                record.getAction(),
                record.getOutcome(),
                record.getActorId(),
                record.getActorUsername(),
                record.getRequestId(),
                record.getDetails(),
                record.getOccurredAt());
    }

    private record Actor(String id, String username) {
    }
}