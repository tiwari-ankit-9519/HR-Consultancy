package com.hrconsultancy.organization.service;

import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hrconsultancy.organization.audit.OrganizationAuditService;
import com.hrconsultancy.organization.dto.CreateOrganizationRequestDto;
import com.hrconsultancy.organization.dto.OrganizationResponse;
import com.hrconsultancy.organization.dto.UpdateOrganizationRequestDto;
import com.hrconsultancy.organization.entity.Organization;
import com.hrconsultancy.organization.repository.OrganizationRepository;

@Service
public class OrganizationService {
    private static final Logger logger = LogManager.getLogger(OrganizationService.class);
    private final OrganizationRepository organizationRepository;
    private final OrganizationAuditService organizationAuditService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationAuditService organizationAuditService) {
        this.organizationRepository = organizationRepository;
        this.organizationAuditService = organizationAuditService;
    }

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequestDto request) {
        logger.info("Organization creation started");
        Organization organization = new Organization(request.getName(), request.getLegalName(), request.getGstin());
        Organization savedOrganization = organizationRepository.saveAndFlush(organization);
        Map<String, Object> newValues = organizationAuditService.toSnapshot(savedOrganization);
        organizationAuditService.record(savedOrganization.getId(), "CREATE", null, newValues);
        logger.info("Organization created successfully organizationId={}", savedOrganization.getId());
        return OrganizationResponse.fromEntity(savedOrganization);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAllOrganizations() {
        logger.info("Fetching all organizations");
        return organizationRepository.findAll().stream().map(OrganizationResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(UUID id) {
        logger.info("Fetching organization with organizationId={}", id);
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Organization not found with organizationId={}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Organization not found");
                });

        return OrganizationResponse.fromEntity(organization);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID id, UpdateOrganizationRequestDto request) {
        logger.info("Organizationn update started for organizationId={}", id);
        Organization organization = organizationRepository.findById(id).orElseThrow(() -> {
            logger.warn("Organization not found organizationId={}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
        });
        Map<String, Object> oldValues = organizationAuditService.toSnapshot(organization);
        organization.setName(request.getName());
        organization.setLegalName(request.getLegalname());
        organization.setGstin(request.getGstin());
        Organization updatedOrganization = organizationRepository.saveAndFlush(organization);
        Map<String, Object> newValues = organizationAuditService.toSnapshot(updatedOrganization);
        organizationAuditService.record(updatedOrganization.getId(), "UPDATE", oldValues, newValues);
        logger.info("Organization updated successfully for organizationId={}", id);
        return OrganizationResponse.fromEntity(updatedOrganization);
    }

    @Transactional
    public void deactivateOrganizationById(UUID id) {
        logger.info("Organization deactivation started for organizationId={}", id);
        Organization organization = organizationRepository.findById(id).orElseThrow(() -> {
            logger.warn("Organization not found with organizationId={}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
        });
        if ("INACTIVE".equals(organization.getStatus())) {
            logger.info("Organization is already inactive organizationId={}", id);
            return;
        }

        Map<String, Object> oldValues = organizationAuditService.toSnapshot(organization);
        organization.setStatus("INACTIVE");
        Organization updateOrganization = organizationRepository.saveAndFlush(organization);
        Map<String, Object> newValues = organizationAuditService.toSnapshot(updateOrganization);
        organizationAuditService.record(updateOrganization.getId(), "DEACTIVATE", oldValues, newValues);
        logger.info("Organization deactivated successfully for organizationId={}", id);
    }
}
