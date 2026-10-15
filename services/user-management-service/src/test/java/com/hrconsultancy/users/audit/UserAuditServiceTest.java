package com.hrconsultancy.users.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class UserAuditServiceTest {

    @Mock
    private UserAuditRepository repository;

    @InjectMocks
    private UserAuditService auditService;

    @BeforeEach
    void setUp() {
        ThreadContext.put("requestId", "audit-test-request-123");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("actor-123")
                .claim("preferred_username", "hr.admin")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void tearDown() {
        ThreadContext.clearMap();
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordShouldPersistActorRequestIdAndEventDetails() {
        when(repository.saveAndFlush(any(UserAuditRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = auditService.record(
                "keycloak-user-456",
                "CREATE",
                "SUCCESS",
                Map.of("username", "new.employee", "assignedRoles",
                        java.util.List.of("RECRUITER")));

        ArgumentCaptor<UserAuditRecord> captor = ArgumentCaptor.forClass(UserAuditRecord.class);

        verify(repository).saveAndFlush(captor.capture());

        UserAuditRecord saved = captor.getValue();

        assertNotNull(saved.getId());
        assertEquals("keycloak-user-456", saved.getTargetUserId());
        assertEquals("CREATE", saved.getAction());
        assertEquals("SUCCESS", saved.getOutcome());
        assertEquals("actor-123", saved.getActorId());
        assertEquals("hr.admin", saved.getActorUsername());
        assertEquals("audit-test-request-123", saved.getRequestId());
        assertEquals("new.employee", saved.getDetails().get("username"));

        assertNotNull(response.occurredAt());
        assertEquals(saved.getId(), response.id());
    }
}