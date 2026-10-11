package com.hrconsultancy.organization.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrconsultancy.organization.entity.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

}
