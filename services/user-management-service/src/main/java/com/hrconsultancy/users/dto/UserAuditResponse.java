package com.hrconsultancy.users.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UserAuditResponse(
        UUID id,
        String targetUserId,
        String action,
        String outcome,
        String actorId,
        String actorUsername,
        String requestId,
        Map<String, Object> details,
        Instant occurredAt) {
}