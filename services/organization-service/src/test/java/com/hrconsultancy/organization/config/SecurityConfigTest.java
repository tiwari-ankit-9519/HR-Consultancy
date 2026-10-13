
package com.hrconsultancy.organization.config;

import com.hrconsultancy.organization.audit.OrganizationAuditService;
import com.hrconsultancy.organization.controller.OrganizationController;
import com.hrconsultancy.organization.service.OrganizationService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.time.Instant;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity 
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private OrganizationAuditService organizationAuditService;

    private JwtAuthenticationToken jwtWithRole(String role) {

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .claim("preferred_username", "test.user")
                .claim("iss",System.getProperty("test.jwt.issuer","http://localhost:8080/realms/hr-platform"))
                .claim("aud", List.of("hr-platform-api"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @Test
    void unauthenticatedRequestShouldReturn401() throws Exception {

        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hrAdminShouldBeAllowedToReadOrganizations() throws Exception {

        mockMvc.perform(get("/api/v1/organizations")
                        .with(authentication(jwtWithRole("HR_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void recruiterShouldBeForbiddenFromCreatingOrganizations()
            throws Exception {

        mockMvc.perform(post("/api/v1/organizations")
                        .with(authentication(jwtWithRole("RECRUITER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test Organization",
                                  "legalName": "Test Organization Pvt Ltd"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrManagerShouldBeForbiddenFromDeactivatingOrganizations()
            throws Exception {

        mockMvc.perform(delete(
                        "/api/v1/organizations/11111111-1111-1111-1111-111111111111")
                        .with(authentication(jwtWithRole("HR_MANAGER"))))
                .andExpect(status().isForbidden());
    }
}
