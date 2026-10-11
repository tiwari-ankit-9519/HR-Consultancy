package com.hrconsultancy.organization.service;

import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hrconsultancy.organization.dto.CreateOrganizationRequestDto;
import com.hrconsultancy.organization.dto.OrganizationResponse;
import com.hrconsultancy.organization.entity.Organization;
import com.hrconsultancy.organization.repository.OrganizationRepository;

@Service
public class OrganizationService {
    private static final Logger logger = LogManager.getLogger(OrganizationService.class);
    private final OrganizationRepository organizationRepository;

    public OrganizationService(
            OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequestDto request) {
        logger.info("Organization creation started");
        Organization organization = new Organization(request.getName(), request.getLegalName(), request.getGstin());
        Organization savedOrganization = organizationRepository.saveAndFlush(organization);
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
}
