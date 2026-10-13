package com.hrconsultancy.organization.audit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;


import com.hrconsultancy.organization.entity.Organization;

@Service
public class OrganizationAuditService {
    private static final Logger logger = LogManager.getLogger(OrganizationAuditService.class);
    private final OrganizationAuditRepository organizationAuditRepository;

    public OrganizationAuditService(
            OrganizationAuditRepository organizationAuditRepository) {
        this.organizationAuditRepository = organizationAuditRepository;
    }

    public Map<String, Object> toSnapshot(Organization organization) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("id", organization.getId());
        snapshot.put("name", organization.getName());
        snapshot.put("legalName", organization.getLegalName());
        snapshot.put("gstin", organization.getGstin());
        snapshot.put("status", organization.getStatus());
        snapshot.put("createdAt", organization.getCreatedAt());
        snapshot.put("updatedAt", organization.getUpdatedAt());

        return snapshot;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID organizationId, String action, Map<String, Object> oldValues,
            Map<String, Object> newValues) {
        String requestId = ThreadContext.get("requestId");
        OrganizationAudit audit = new OrganizationAudit(organizationId, action, oldValues, newValues, getCurrentUsername(), requestId);
        organizationAuditRepository.save(audit);
        logger.info("Organization audit recorded organizationId={} action={}", organizationId, action);
    }

    @Transactional(readOnly = true)
    public List<OrganizationAuditResponse> getAuditHistory(UUID organizationId) {
        logger.info("Fetching organization audit history for organizationId={}", organizationId);
        return organizationAuditRepository.findByOrganizationIdOrderByChangedAtDesc(organizationId).stream()
                .map(OrganizationAuditResponse::fromEntity).collect(Collectors.toList());
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String username = jwtAuthenticationToken.getToken().getClaimAsString("preferred_username");
            if(username != null && !username.isBlank()) {
                return username;
            }

            return jwtAuthenticationToken.getToken().getSubject();
        }

        return  null;
    }
}
