
package com.hrconsultancy.users.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.hrconsultancy.users.audit.UserAuditService;
import com.hrconsultancy.users.dto.CreateUserRequest;
import com.hrconsultancy.users.dto.UserResponse;
import com.hrconsultancy.users.profile.User;
import com.hrconsultancy.users.profile.UserRepository;

class UserManagementServiceTest {

        private final KeycloakAdminClient keycloakAdminClient = mock(KeycloakAdminClient.class);

        private final UserAuditService userAuditService = mock(UserAuditService.class);

        private final UserRepository userRepository = mock(UserRepository.class);

        private final UserManagementService userManagementService = new UserManagementService(
                        keycloakAdminClient,
                        userAuditService,
                        userRepository);

        private CreateUserRequest request() {
                return new CreateUserRequest(
                                "test.recruiter",
                                "recruiter@example.com",
                                "Test",
                                "Recruiter",
                                Set.of("RECRUITER"));
        }

        @Test
        void createsUserAndAssignsRolesSuccessfully() {
                when(keycloakAdminClient.createUser(request()))
                                .thenReturn("kc-user-1");

                when(keycloakAdminClient.getUser("kc-user-1"))
                                .thenReturn(Map.of(
                                                "id", "kc-user-1",
                                                "username", "test.recruiter",
                                                "email", "recruiter@example.com",
                                                "firstName", "Test",
                                                "lastName", "Recruiter",
                                                "enabled", true));

                when(keycloakAdminClient.getUserRoles("kc-user-1"))
                                .thenReturn(Set.of("RECRUITER"));

                when(userRepository.saveAndFlush(any(User.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                UserResponse response = userManagementService.createUser(request());

                assertEquals("kc-user-1", response.id());
                assertEquals("test.recruiter", response.username());
                assertEquals("recruiter@example.com", response.email());
                assertEquals("Test", response.firstName());
                assertEquals("Recruiter", response.lastName());
                assertEquals(Set.of("RECRUITER"), response.roles());
                assertEquals("ACTIVE", response.status());

                verify(keycloakAdminClient)
                                .assignRoles("kc-user-1", Set.of("RECRUITER"));

                verify(userRepository).saveAndFlush(any(User.class));

                verify(userAuditService).record(
                                eq("kc-user-1"),
                                eq("ROLE_ASSIGNMENT"),
                                eq("SUCCESS"),
                                anyMap());

                verify(userAuditService).record(
                                eq("kc-user-1"),
                                eq("CREATE"),
                                eq("SUCCESS"),
                                anyMap());
        }

        @Test
        void rollsBackCreatedUserWhenProvisioningFails() {
                when(keycloakAdminClient.createUser(request()))
                                .thenReturn("kc-user-2");

                doThrow(new KeycloakAdminException(
                                "Simulated lookup failure",
                                502,
                                "USER_LOOKUP")).when(keycloakAdminClient).getUser("kc-user-2");

                assertThrows(
                                KeycloakAdminException.class,
                                () -> userManagementService.createUser(request()));

                // The partially provisioned Keycloak user must be deleted.
                verify(keycloakAdminClient).deleteUser("kc-user-2");

                // Profile cleanup is attempted during compensation.
                verify(userRepository)
                                .deleteByKeycloakUserId("kc-user-2");

                // Failure and successful compensation must be audited.
                verify(userAuditService).record(
                                eq("kc-user-2"),
                                eq("PROVISIONING_FAILURE"),
                                eq("FAILURE"),
                                anyMap());

                verify(userAuditService).record(
                                eq("kc-user-2"),
                                eq("PROVISIONING_ROLLBACK"),
                                eq("SUCCESS"),
                                anyMap());
        }
}
