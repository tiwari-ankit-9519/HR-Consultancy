package com.hrconsultancy.organization.controller;

import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrconsultancy.organization.dto.CreateOrganizationRequestDto;
import com.hrconsultancy.organization.dto.OrganizationResponse;
import com.hrconsultancy.organization.service.OrganizationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {
    private static final Logger logger = LogManager.getLogger(OrganizationController.class);
    private final OrganizationService organizationService;

    public OrganizationController(
            OrganizationService organizationService) {
        this.organizationService = organizationService;
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

}
