package com.hrconsultancy.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.hrconsultancy.organization.audit.OrganizationAuditService;
import com.hrconsultancy.organization.dto.CreateOrganizationRequestDto;
import com.hrconsultancy.organization.dto.OrganizationResponse;
import com.hrconsultancy.organization.dto.UpdateOrganizationRequestDto;
import com.hrconsultancy.organization.entity.Organization;
import com.hrconsultancy.organization.repository.OrganizationRepository;
import com.hrconsultancy.organization.service.OrganizationService;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationAuditService organizationAuditService;

    @InjectMocks
    private OrganizationService organizationService;

    private UUID organizationId;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();

        organization = new Organization(
                "ABC Consultancy",
                "ABC Consultancy Private Limited",
                "27ABCDE1234F1Z5");

        ReflectionTestUtils.setField(
                organization, "id", organizationId);
    }

    @Test
    void createOrganizationShouldSaveOrganizationAndRecordAudit() {
        CreateOrganizationRequestDto request = new CreateOrganizationRequestDto(
                "ABC Consultancy",
                "ABC Consultancy Private Limited",
                "27ABCDE1234F1Z5");

        Map<String, Object> newValues = Map.of(
                "name", "ABC Consultancy",
                "status", "ACTIVE");

        when(organizationRepository.saveAndFlush(
                any(Organization.class)))
                .thenAnswer(invocation -> {
                    Organization saved = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            saved, "id", organizationId);

                    return saved;
                });

        when(organizationAuditService.toSnapshot(
                any(Organization.class)))
                .thenReturn(newValues);

        OrganizationResponse response = organizationService.createOrganization(request);

        assertNotNull(response);
        assertEquals(organizationId, response.getId());
        assertEquals("ABC Consultancy", response.getName());

        verify(organizationRepository)
                .saveAndFlush(any(Organization.class));

        verify(organizationAuditService)
                .record(organizationId, "CREATE", null, newValues);
    }

    @Test
    void getAllOrganizationsShouldReturnAllOrganizations() {
        when(organizationRepository.findAll())
                .thenReturn(List.of(organization));

        List<OrganizationResponse> responses = organizationService.getAllOrganizations();

        assertEquals(1, responses.size());
        assertEquals(
                "ABC Consultancy",
                responses.get(0).getName());

        verify(organizationRepository).findAll();
    }

    @Test
    void getOrganizationByIdShouldReturnOrganization() {
        when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(organization));

        OrganizationResponse response = organizationService.getOrganizationById(organizationId);

        assertNotNull(response);
        assertEquals(organizationId, response.getId());
        assertEquals("ABC Consultancy", response.getName());

        verify(organizationRepository).findById(organizationId);
    }

    @Test
    void getOrganizationByIdShouldThrowWhenNotFound() {
        when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> organizationService.getOrganizationById(
                        organizationId));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void updateOrganizationShouldRecordOldAndNewValues() {
        UpdateOrganizationRequestDto request = new UpdateOrganizationRequestDto(
                "ABC Consultancy India",
                "ABC Consultancy India Private Limited",
                "27XYZAB1234C1Z8");

        Map<String, Object> oldValues = Map.of(
                "name", "ABC Consultancy");

        Map<String, Object> newValues = Map.of(
                "name", "ABC Consultancy India");

        when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(organization));

        when(organizationRepository.saveAndFlush(
                any(Organization.class)))
                .thenReturn(organization);

        AtomicInteger snapshotCall = new AtomicInteger(0);

        when(organizationAuditService.toSnapshot(any(Organization.class)))
                .thenAnswer(invocation -> snapshotCall.getAndIncrement() == 0
                        ? oldValues
                        : newValues);

        OrganizationResponse response = organizationService.updateOrganization(
                organizationId, request);

        assertEquals(
                "ABC Consultancy India",
                response.getName());

        assertEquals(
                "ABC Consultancy India Private Limited",
                organization.getLegalName());

        assertEquals(
                "27XYZAB1234C1Z8",
                organization.getGstin());

        verify(organizationAuditService).record(
                organizationId,
                "UPDATE",
                oldValues,
                newValues);
    }

    @Test
    void deactivateOrganizationShouldRecordAudit() {
        Map<String, Object> oldValues = Map.of(
                "name", "ABC Consultancy",
                "status", "ACTIVE");

        Map<String, Object> newValues = Map.of(
                "name", "ABC Consultancy",
                "status", "INACTIVE");

        when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(organization));

        when(organizationRepository.saveAndFlush(
                any(Organization.class)))
                .thenReturn(organization);

        AtomicInteger snapshotCall = new AtomicInteger(0);

        when(organizationAuditService.toSnapshot(any(Organization.class)))
                .thenAnswer(invocation -> snapshotCall.getAndIncrement() == 0
                        ? oldValues
                        : newValues);

        organizationService.deactivateOrganizationById(organizationId);

        assertEquals("INACTIVE", organization.getStatus());

        verify(organizationAuditService).record(
                organizationId,
                "DEACTIVATE",
                oldValues,
                newValues);
    }

    @Test
    void deactivateOrganizationShouldNotAuditTwice() {
        organization.setStatus("INACTIVE");

        when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(organization));

        organizationService.deactivateOrganizationById(organizationId);

        verify(organizationRepository, never())
                .saveAndFlush(any(Organization.class));

        verify(organizationAuditService, never())
                .record(any(), any(), any(), any());
    }

    @Test
    void deactivateOrganizationShouldThrowWhenNotFound() {
        when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> organizationService.deactivateOrganizationById(
                        organizationId));

        assertEquals(404, exception.getStatusCode().value());

        verify(organizationAuditService, never())
                .record(any(), any(), any(), any());
    }
}
