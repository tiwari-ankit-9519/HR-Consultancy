
package com.hrconsultancy.organization.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import tools.jackson.databind.ObjectMapper;
import com.hrconsultancy.organization.audit.OrganizationAuditResponse;
import com.hrconsultancy.organization.audit.OrganizationAuditService;
import com.hrconsultancy.organization.dto.CreateOrganizationRequestDto;
import com.hrconsultancy.organization.dto.OrganizationResponse;
import com.hrconsultancy.organization.dto.UpdateOrganizationRequestDto;
import com.hrconsultancy.organization.service.OrganizationService;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OrganizationAuditService organizationAuditService;

    @InjectMocks
    private OrganizationController organizationController;

    private UUID organizationId;
    private OrganizationResponse organizationResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(organizationController)
                .setValidator(validator)
                .build();

        organizationId = UUID.randomUUID();

        organizationResponse = new OrganizationResponse(
                organizationId,
                "ABC Consultancy",
                "ABC Consultancy Private Limited",
                "27XYZAB1234C1Z8",
                "ACTIVE",
                null,
                null);
    }

    @Test
    void createOrganizationShouldReturnCreated() throws Exception {
        CreateOrganizationRequestDto request = new CreateOrganizationRequestDto(
                "ABC Consultancy",
                "ABC Consultancy Private Limited",
                "27XYZAB1234C1Z8");

        when(organizationService.createOrganization(
                any(CreateOrganizationRequestDto.class)))
                .thenReturn(organizationResponse);

        mockMvc.perform(post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(
                        organizationId.toString()))
                .andExpect(jsonPath("$.name").value(
                        "ABC Consultancy"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(organizationService).createOrganization(
                any(CreateOrganizationRequestDto.class));
    }

    @Test
    void createOrganizationShouldRejectBlankName() throws Exception {
        CreateOrganizationRequestDto request = new CreateOrganizationRequestDto(
                " ",
                "ABC Consultancy Private Limited",
                "27XYZAB1234C1Z8");

        mockMvc.perform(post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(organizationService);
    }

    @Test
    void getAllOrganizationsShouldReturnList() throws Exception {
        when(organizationService.getAllOrganizations())
                .thenReturn(List.of(organizationResponse));

        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(
                        "ABC Consultancy"));

        verify(organizationService).getAllOrganizations();
    }

    @Test
    void getOrganizationByIdShouldReturnOrganization()
            throws Exception {

        when(organizationService.getOrganizationById(organizationId))
                .thenReturn(organizationResponse);

        mockMvc.perform(get(
                "/api/v1/organizations/{id}", organizationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(
                        organizationId.toString()))
                .andExpect(jsonPath("$.name").value(
                        "ABC Consultancy"));

        verify(organizationService)
                .getOrganizationById(organizationId);
    }

    @Test
    void getOrganizationByIdShouldRejectMalformedUuid()
            throws Exception {

        mockMvc.perform(get(
                "/api/v1/organizations/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(organizationService);
    }

    @Test
    void updateOrganizationShouldReturnUpdatedOrganization()
            throws Exception {

        UpdateOrganizationRequestDto request = new UpdateOrganizationRequestDto(
                "ABC Consultancy India",
                "ABC Consultancy India Private Limited",
                "27XYZAB1234C1Z8");

        OrganizationResponse updatedResponse = new OrganizationResponse(
                organizationId,
                "ABC Consultancy India",
                "ABC Consultancy India Private Limited",
                "27XYZAB1234C1Z8",
                "ACTIVE",
                null,
                null);

        when(organizationService.updateOrganization(
                eq(organizationId),
                any(UpdateOrganizationRequestDto.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put(
                "/api/v1/organizations/{id}", organizationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(
                        "ABC Consultancy India"))
                .andExpect(jsonPath("$.legalName").value(
                        "ABC Consultancy India Private Limited"));

        verify(organizationService).updateOrganization(
                eq(organizationId),
                any(UpdateOrganizationRequestDto.class));
    }

    @Test
    void updateOrganizationShouldRejectBlankName()
            throws Exception {

        UpdateOrganizationRequestDto request = new UpdateOrganizationRequestDto(
                "",
                "ABC Consultancy Private Limited",
                "27XYZAB1234C1Z8");

        mockMvc.perform(put(
                "/api/v1/organizations/{id}", organizationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(organizationService);
    }

    @Test
    void deactivateOrganizationShouldReturnNoContent()
            throws Exception {

        mockMvc.perform(delete(
                "/api/v1/organizations/{id}", organizationId))
                .andExpect(status().isNoContent());

        verify(organizationService)
                .deactivateOrganizationById(organizationId);
    }

    @Test
    void getAuditHistoryShouldReturnAuditEntries()
            throws Exception {

        OrganizationAuditResponse auditResponse = new OrganizationAuditResponse(
                UUID.randomUUID(),
                organizationId,
                "CREATE",
                null,
                Map.of("name", "ABC Consultancy"),
                null,
                "request-123",
                null);

        when(organizationService.getOrganizationById(organizationId))
                .thenReturn(organizationResponse);

        when(organizationAuditService.getAuditHistory(organizationId))
                .thenReturn(List.of(auditResponse));

        mockMvc.perform(get(
                "/api/v1/organizations/{id}/audit-logs",
                organizationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("CREATE"))
                .andExpect(jsonPath("$[0].requestId").value(
                        "request-123"));

        verify(organizationService)
                .getOrganizationById(organizationId);
        verify(organizationAuditService)
                .getAuditHistory(organizationId);
    }

    @Test
    void getAuditHistoryShouldRejectMalformedUuid()
            throws Exception {

        mockMvc.perform(get(
                "/api/v1/organizations/{id}/audit-logs",
                "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(organizationService);
        verifyNoInteractions(organizationAuditService);
    }
}
