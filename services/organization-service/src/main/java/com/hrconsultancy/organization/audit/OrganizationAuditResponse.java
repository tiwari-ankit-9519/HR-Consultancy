package com.hrconsultancy.organization.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class OrganizationAuditResponse {
    private UUID id;
    private UUID organizationId;
    private String action;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private String changedBy;
    private String requestId;
    private OffsetDateTime changedAt;

    public OrganizationAuditResponse(
            UUID id,
            UUID organizationId,
            String action,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            String changedBy,
            String requestId,
            OffsetDateTime changedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.action = action;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.changedBy = changedBy;
        this.requestId = requestId;
        this.changedAt = changedAt;
    }

    public static OrganizationAuditResponse fromEntity(OrganizationAudit audit) {
        return new OrganizationAuditResponse(audit.getId(), audit.getOrganizationId(), audit.getAction(),
                audit.getOldValues(), audit.getNewValues(), audit.getChangedBy(), audit.getRequestId(),
                audit.getChangedAt());
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public Map<String, Object> getNewValues() {
        return newValues;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getRequestId() {
        return requestId;
    }

    public OffsetDateTime getChangedAt() {
        return changedAt;
    }
}
