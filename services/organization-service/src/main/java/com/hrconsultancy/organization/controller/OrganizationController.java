package com.hrconsultancy.organization.controller;

import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrconsultancy.organization.dto.CreateOrganizationRequestDto;
import com.hrconsultancy.organization.dto.OrganizationResponse;
import com.hrconsultancy.organization.dto.UpdateOrganizationRequestDto;
import com.hrconsultancy.organization.service.OrganizationService;
import com.hrconsultancy.organization.audit.OrganizationAuditResponse;
import com.hrconsultancy.organization.audit.OrganizationAuditService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {
    private static final Logger logger = LogManager.getLogger(OrganizationController.class);
    private final OrganizationService organizationService;
    private final OrganizationAuditService organizationAuditService;

    public OrganizationController(
            OrganizationService organizationService,
            OrganizationAuditService organizationAuditService) {

        this.organizationService = organizationService;
        this.organizationAuditService = organizationAuditService;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequestDto request) {
        logger.info("Received create organization request");
        OrganizationResponse response = organizationService.createOrganization(request);
        logger.info("Create organization request handled successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations() {
        logger.info("Received request to get all organizations");
        List<OrganizationResponse> organzations = organizationService.getAllOrganizations();
        logger.info("Retrieved organizations count={}", organzations.size());
        return ResponseEntity.ok(organzations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(@PathVariable UUID id) {
        logger.info("Received get organization request for organizationId={}", id);
        OrganizationResponse organization = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(organization);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequestDto request) {
        logger.info("Received update organization request for organizationId={}", id);
        OrganizationResponse response = organizationService.updateOrganization(id, request);
        logger.info("Update organization request handled successfully for organizationId={}", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateOrganization(
            @PathVariable UUID id) {
        logger.info("Received organization deactivation request for organizationId={}", id);
        organizationService.deactivateOrganizationById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<List<OrganizationAuditResponse>> getAuditHistory(
            @PathVariable UUID id) {
        logger.info("Received organization audit history request for organizationId={}", id);
        organizationService.getOrganizationById(id);
        List<OrganizationAuditResponse> auditHistory = organizationAuditService.getAuditHistory(id);
        return ResponseEntity.ok(auditHistory);
    }
}
