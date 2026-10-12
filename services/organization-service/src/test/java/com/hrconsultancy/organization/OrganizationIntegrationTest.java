
package com.hrconsultancy.organization;

import com.hrconsultancy.organization.audit.OrganizationAudit;
import com.hrconsultancy.organization.audit.OrganizationAuditRepository;
import com.hrconsultancy.organization.audit.OrganizationAuditResponse;
import com.hrconsultancy.organization.audit.OrganizationAuditService;
import com.hrconsultancy.organization.dto.CreateOrganizationRequestDto;
import com.hrconsultancy.organization.dto.OrganizationResponse;
import com.hrconsultancy.organization.dto.UpdateOrganizationRequestDto;
import com.hrconsultancy.organization.entity.Organization;
import com.hrconsultancy.organization.repository.OrganizationRepository;
import com.hrconsultancy.organization.service.OrganizationService;
import org.apache.logging.log4j.ThreadContext;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationIntegrationTest {

        @Autowired
        private OrganizationService organizationService;

        @Autowired
        private OrganizationRepository organizationRepository;

        @Autowired
        private OrganizationAuditRepository organizationAuditRepository;

        @MockitoSpyBean
        private OrganizationAuditRepository auditRepositorySpy;

        @Autowired
        private OrganizationAuditService organizationAuditService;

        @Test
        void createOrganizationShouldPersistOrganizationAndCreateAudit() {
                CreateOrganizationRequestDto request = new CreateOrganizationRequestDto(
                                "Integration Test Organization",
                                "Integration Test Legal Name",
                                null);

                OrganizationResponse response = organizationService.createOrganization(request);

                assertNotNull(response.getId());

                Organization savedOrganization = organizationRepository
                                .findById(response.getId())
                                .orElseThrow();

                assertEquals("Integration Test Organization", savedOrganization.getName());
                assertEquals("Integration Test Legal Name", savedOrganization.getLegalName());
                assertEquals("ACTIVE", savedOrganization.getStatus());

                List<OrganizationAudit> auditRecords = organizationAuditRepository
                                .findByOrganizationIdOrderByChangedAtDesc(response.getId());

                assertEquals(1, auditRecords.size());

                OrganizationAudit audit = auditRecords.get(0);

                assertNotNull(audit.getId(), "Audit ID must be generated");
                assertEquals(response.getId(), audit.getOrganizationId());
                assertEquals("CREATE", audit.getAction());
                assertNotNull(audit.getNewValues(), "New snapshot must be recorded");
                assertNotNull(audit.getChangedAt(), "Audit timestamp must be recorded");
        }

        @Test
        void createOrganizationShouldPersistRequestIdInAudit() {
                String requestId = "audit-test-request-123";
                String previousRequestId = ThreadContext.get("requestId");

                try {
                        ThreadContext.put("requestId", requestId);

                        CreateOrganizationRequestDto request = new CreateOrganizationRequestDto(
                                        "Request ID Test Organization",
                                        "Request ID Test Legal Name",
                                        null);

                        OrganizationResponse response = organizationService.createOrganization(request);

                        List<OrganizationAudit> auditRecords = organizationAuditRepository
                                        .findByOrganizationIdOrderByChangedAtDesc(
                                                        response.getId());

                        assertEquals(1, auditRecords.size());

                        OrganizationAudit audit = auditRecords.get(0);

                        assertEquals("CREATE", audit.getAction());
                        assertEquals(
                                        requestId,
                                        audit.getRequestId(),
                                        "Audit record should contain the request ID");
                } finally {
                        if (previousRequestId == null) {
                                ThreadContext.remove("requestId");
                        } else {
                                ThreadContext.put("requestId", previousRequestId);
                        }
                }
        }

        @Test
        void updateOrganizationShouldPersistChangesAndAuditOldAndNewValues() {
                CreateOrganizationRequestDto createRequest = new CreateOrganizationRequestDto(
                                "Original Organization",
                                "Original Legal Name",
                                null);

                OrganizationResponse created = organizationService.createOrganization(createRequest);

                UpdateOrganizationRequestDto updateRequest = new UpdateOrganizationRequestDto(
                                "Updated Organization",
                                "Updated Legal Name",
                                null);

                OrganizationResponse updated = organizationService.updateOrganization(created.getId(), updateRequest);

                assertEquals(created.getId(), updated.getId());
                assertEquals("Updated Organization", updated.getName());
                assertEquals("Updated Legal Name", updated.getLegalName());

                Organization persisted = organizationRepository
                                .findById(created.getId())
                                .orElseThrow();

                assertEquals("Updated Organization", persisted.getName());
                assertEquals("Updated Legal Name", persisted.getLegalName());

                List<OrganizationAudit> auditRecords = organizationAuditRepository
                                .findByOrganizationIdOrderByChangedAtDesc(created.getId());

                assertEquals(2, auditRecords.size());

                OrganizationAudit updateAudit = auditRecords.stream()
                                .filter(audit -> "UPDATE".equals(audit.getAction()))
                                .findFirst()
                                .orElseThrow();

                assertNotNull(updateAudit.getOldValues());
                assertNotNull(updateAudit.getNewValues());

                assertEquals(
                                "Original Organization",
                                updateAudit.getOldValues().get("name"));

                assertEquals(
                                "Updated Organization",
                                updateAudit.getNewValues().get("name"));
        }

        @Test
        void deactivateOrganizationShouldPersistInactiveStatusAndAudit() {
                CreateOrganizationRequestDto request = new CreateOrganizationRequestDto(
                                "Organization To Deactivate",
                                "Deactivation Test Legal Name",
                                null);

                OrganizationResponse created = organizationService.createOrganization(request);

                organizationService.deactivateOrganizationById(created.getId());

                Organization persisted = organizationRepository
                                .findById(created.getId())
                                .orElseThrow();

                assertEquals("INACTIVE", persisted.getStatus());

                List<OrganizationAudit> auditRecords = organizationAuditRepository
                                .findByOrganizationIdOrderByChangedAtDesc(created.getId());

                assertEquals(2, auditRecords.size());

                OrganizationAudit deactivateAudit = auditRecords.stream()
                                .filter(audit -> "DEACTIVATE".equals(audit.getAction()))
                                .findFirst()
                                .orElseThrow();

                assertEquals(
                                "ACTIVE",
                                deactivateAudit.getOldValues().get("status"));

                assertEquals(
                                "INACTIVE",
                                deactivateAudit.getNewValues().get("status"));
        }

        @Test
        void repeatedDeactivationShouldNotCreateDuplicateAudit() {
                CreateOrganizationRequestDto request = new CreateOrganizationRequestDto(
                                "Idempotent Deactivation Organization",
                                "Idempotent Test Legal Name",
                                null);

                OrganizationResponse created = organizationService.createOrganization(request);

                organizationService.deactivateOrganizationById(created.getId());
                organizationService.deactivateOrganizationById(created.getId());

                List<OrganizationAudit> auditRecords = organizationAuditRepository
                                .findByOrganizationIdOrderByChangedAtDesc(created.getId());

                long deactivationCount = auditRecords.stream()
                                .filter(audit -> "DEACTIVATE".equals(audit.getAction()))
                                .count();

                assertEquals(1, deactivationCount);
        }

        @Test
        void auditHistoryShouldReturnOrganizationChangesInDescendingTimestampOrder() {
                // Arrange: create an organization.
                CreateOrganizationRequestDto createRequest = new CreateOrganizationRequestDto(
                                "Audit History Organization",
                                "Audit History Legal Name",
                                null);

                OrganizationResponse created = organizationService.createOrganization(createRequest);

                // Act: update the organization.
                UpdateOrganizationRequestDto updateRequest = new UpdateOrganizationRequestDto(
                                "Audit History Organization Updated",
                                "Audit History Legal Name Updated",
                                null);

                organizationService.updateOrganization(created.getId(), updateRequest);

                // Retrieve audit history through the audit service.
                List<OrganizationAuditResponse> history = organizationAuditService.getAuditHistory(created.getId());

                // Assert: both CREATE and UPDATE audit records exist.
                assertEquals(2, history.size());

                OrganizationAuditResponse createAudit = history.stream()
                                .filter(audit -> "CREATE".equals(audit.getAction()))
                                .findFirst()
                                .orElseThrow();

                OrganizationAuditResponse updateAudit = history.stream()
                                .filter(audit -> "UPDATE".equals(audit.getAction()))
                                .findFirst()
                                .orElseThrow();

                assertEquals(created.getId(), createAudit.getOrganizationId());
                assertEquals(created.getId(), updateAudit.getOrganizationId());

                // Verify the CREATE snapshot.
                assertNotNull(createAudit.getNewValues());
                assertEquals(
                                "Audit History Organization",
                                createAudit.getNewValues().get("name"));

                // Verify the UPDATE audit's old and new snapshots.
                assertNotNull(updateAudit.getOldValues());
                assertNotNull(updateAudit.getNewValues());

                assertEquals(
                                "Audit History Organization",
                                updateAudit.getOldValues().get("name"));

                assertEquals(
                                "Audit History Organization Updated",
                                updateAudit.getNewValues().get("name"));

                // Verify that timestamps are in descending order.
                for (int i = 1; i < history.size(); i++) {
                        assertNotNull(history.get(i - 1).getChangedAt());
                        assertNotNull(history.get(i).getChangedAt());

                        assertTrue(
                                        !history.get(i).getChangedAt()
                                                        .isAfter(history.get(i - 1).getChangedAt()),
                                        "Audit history must be ordered newest first");
                }
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void createOrganizationShouldRollbackWhenAuditPersistenceFails() {
                long organizationsBefore = organizationRepository.count();

                CreateOrganizationRequestDto request = new CreateOrganizationRequestDto(
                                "Rollback Test Organization",
                                "Rollback Test Legal Name",
                                null);

                doThrow(new IllegalStateException(
                                "Simulated audit persistence failure"))
                                .when(auditRepositorySpy)
                                .save(any(OrganizationAudit.class));

                assertThrows(
                                RuntimeException.class,
                                () -> organizationService.createOrganization(request));

                assertEquals(
                                organizationsBefore,
                                organizationRepository.count(),
                                "Organization insert should roll back when audit persistence fails");
        }
}
