package com.hrconsultancy.organization.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationAuditRepository extends JpaRepository<OrganizationAudit, UUID> {
    List<OrganizationAudit> findByOrganizationIdOrderByChangedAtDesc(UUID organizationId);
}
