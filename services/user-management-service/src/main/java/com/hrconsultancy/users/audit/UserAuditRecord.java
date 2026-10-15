package com.hrconsultancy.users.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_audit", schema = "user_management")
public class UserAuditRecord {
    @Id
    private UUID id;

    @Column(name = "target_user_id", length = 255)
    private String targetUserId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 30)
    private String outcome;

    @Column(name = "actor_id", length = 255)
    private String actorId;

    @Column(name = "actor_username", length = 255)
    private String actorUsername;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected UserAuditRecord() {
    }

    public UserAuditRecord(
            UUID id,
            String targetUserId,
            String action,
            String outcome,
            String actorId,
            String actorUsername,
            String requestId,
            Map<String, Object> details, Instant occurredAt) {
        this.id = id;
        this.targetUserId = targetUserId;
        this.action = action;
        this.outcome = outcome;
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.requestId = requestId;
        this.details = details;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public String getAction() {
        return action;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getRequestId() {
        return requestId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

}
