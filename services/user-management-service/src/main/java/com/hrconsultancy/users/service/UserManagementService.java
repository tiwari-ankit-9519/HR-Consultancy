package com.hrconsultancy.users.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.hrconsultancy.users.audit.UserAuditService;
import com.hrconsultancy.users.dto.CreateUserRequest;
import com.hrconsultancy.users.dto.UserResponse;
import com.hrconsultancy.users.profile.User;
import com.hrconsultancy.users.profile.UserRepository;

@Service
public class UserManagementService {

    private static final Logger logger = LogManager.getLogger(UserManagementService.class);

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserAuditService userAuditService;
    private final UserRepository userRepository;

    public UserManagementService(
            KeycloakAdminClient keycloakAdminClient,
            UserAuditService userAuditService,
            UserRepository userRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userAuditService = userAuditService;
        this.userRepository = userRepository;
    }

    public UserResponse createUser(CreateUserRequest request) {
        final String userId;

        try {
            userId = keycloakAdminClient.createUser(request);
        } catch (RuntimeException ex) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("username", request.username());
            details.put("reason", "KEYCLOAK_USER_CREATION_FAILED");

            auditSafely(
                    null,
                    "PROVISIONING_FAILURE",
                    "FAILURE",
                    details,
                    ex);

            throw ex;
        }

        try {
            Set<String> requestedRoles = Set.copyOf(request.roles());
            keycloakAdminClient.assignRoles(userId, requestedRoles);
            Map<String, Object> keycloakUser = keycloakAdminClient.getUser(userId);
            Set<String> actualRoles = keycloakAdminClient.getUserRoles(userId);
            User profile = new User(
                    userId,
                    requiredString(keycloakUser, "username"),
                    requiredString(keycloakUser, "firstName"),
                    requiredString(keycloakUser, "lastName"),
                    requiredString(keycloakUser, "email"));

            profile.updateProfile(
                    request.phoneNumber(),
                    request.jobTitle(),
                    request.department(),
                    request.organizationId(),
                    request.employeeCode());

            synchronizeEnabledStatus(
                    profile,
                    Boolean.TRUE.equals(keycloakUser.get("enabled")));

            User savedProfile = userRepository.saveAndFlush(profile);

            Map<String, Object> roleDetails = new LinkedHashMap<>();
            roleDetails.put(
                    "assignedRoles",
                    actualRoles.stream().sorted().toList());

            userAuditService.record(
                    userId,
                    "ROLE_ASSIGNMENT",
                    "SUCCESS",
                    roleDetails);

            Map<String, Object> creationDetails = new LinkedHashMap<>();
            creationDetails.put("username", savedProfile.getUsername());
            creationDetails.put(
                    "assignedRoles",
                    actualRoles.stream().sorted().toList());
            creationDetails.put(
                    "profileId",
                    savedProfile.getId().toString());

            userAuditService.record(
                    userId,
                    "CREATE",
                    "SUCCESS",
                    creationDetails);

            logger.info(
                    "Internal user provisioning completed; userId={}; profileId={}",
                    userId,
                    savedProfile.getId());

            return toResponse(keycloakUser, actualRoles, savedProfile);

        } catch (RuntimeException ex) {
            logger.error(
                    "Internal user provisioning failed; userId={}; attempting rollback",
                    userId);

            Map<String, Object> failureDetails = new LinkedHashMap<>();
            failureDetails.put("reason", "PROVISIONING_FAILED");
            failureDetails.put(
                    "exceptionType",
                    ex.getClass().getSimpleName());

            auditSafely(
                    userId,
                    "PROVISIONING_FAILURE",
                    "FAILURE",
                    failureDetails,
                    ex);

            // Compensate for a profile that may already have been persisted.
            try {
                userRepository.deleteByKeycloakUserId(userId);
                userRepository.flush();
            } catch (RuntimeException profileRollbackException) {
                logger.error(
                        "Could not remove user profile during rollback; userId={}; exceptionType={}",
                        userId,
                        profileRollbackException.getClass().getSimpleName());
                ex.addSuppressed(profileRollbackException);
            }

            // Attempt identity rollback independently of profile rollback.
            try {
                keycloakAdminClient.deleteUser(userId);

                userAuditService.record(
                        userId,
                        "PROVISIONING_ROLLBACK",
                        "SUCCESS",
                        Map.of("reason", "PROVISIONING_COMPENSATION"));

                logger.info(
                        "User provisioning rollback completed; userId={}",
                        userId);
            } catch (RuntimeException rollbackException) {
                logger.error(
                        "Keycloak rollback failed; userId={}; exceptionType={}",
                        userId,
                        rollbackException.getClass().getSimpleName());

                ex.addSuppressed(rollbackException);

                Map<String, Object> rollbackFailure = Map.of(
                        "reason", "ROLLBACK_FAILED",
                        "exceptionType",
                        rollbackException.getClass().getSimpleName());

                auditSafely(
                        userId,
                        "PROVISIONING_ROLLBACK",
                        "FAILURE",
                        rollbackFailure,
                        ex);
            }

            throw ex;
        }
    }

    public UserResponse getUser(String userId) {
        Map<String, Object> keycloakUser = keycloakAdminClient.getUser(userId);

        Set<String> roles = keycloakAdminClient.getUserRoles(userId);

        User profile = findOrCreateProfile(keycloakUser);

        return toResponse(keycloakUser, roles, profile);
    }

    public List<UserResponse> listUsers(int first, int max) {
        return keycloakAdminClient.listUsers(first, max)
                .stream()
                .map(keycloakUser -> {
                    Object id = keycloakUser.get("id");

                    if (!(id instanceof String userId)
                            || userId.isBlank()) {
                        throw new IllegalStateException(
                                "Keycloak returned a user without an ID");
                    }

                    Set<String> roles = keycloakAdminClient.getUserRoles(userId);

                    User profile = findOrCreateProfile(keycloakUser);

                    return toResponse(keycloakUser, roles, profile);
                })
                .toList();
    }

    private User findOrCreateProfile(
            Map<String, Object> keycloakUser) {
        String userId = requiredString(keycloakUser, "id");

        User profile = userRepository
                .findByKeycloakUserId(userId)
                .orElseGet(() -> new User(
                        userId,
                        requiredString(keycloakUser, "username"),
                        requiredString(keycloakUser, "firstName"),
                        requiredString(keycloakUser, "lastName"),
                        requiredString(keycloakUser, "email")));

        profile.updateIdentity(
                requiredString(keycloakUser, "username"),
                requiredString(keycloakUser, "firstName"),
                requiredString(keycloakUser, "lastName"),
                requiredString(keycloakUser, "email"));

        synchronizeEnabledStatus(
                profile,
                Boolean.TRUE.equals(keycloakUser.get("enabled")));

        return userRepository.saveAndFlush(profile);
    }

    private void synchronizeEnabledStatus(
            User profile,
            boolean enabled) {
        if (enabled && "INACTIVE".equals(profile.getStatus())) {
            profile.activate();
        } else if (!enabled && "ACTIVE".equals(profile.getStatus())) {
            profile.deactivate();
        }
    }

    private UserResponse toResponse(
            Map<String, Object> keycloakUser,
            Set<String> roles,
            User profile) {
        return new UserResponse(
                requiredString(keycloakUser, "id"),
                profile.getId(),
                profile.getUsername(),
                profile.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                Boolean.TRUE.equals(keycloakUser.get("enabled")),
                Set.copyOf(roles),
                profile.getPhoneNumber(),
                profile.getJobTitle(),
                profile.getDepartment(),
                profile.getOrganizationId(),
                profile.getEmployeeCode(),
                profile.getStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getDeactivatedAt());
    }

    private String requiredString(
            Map<String, Object> source,
            String key) {
        Object value = source.get(key);

        if (!(value instanceof String string) || string.isBlank()) {
            throw new IllegalStateException(
                    "Required identity field is missing: " + key);
        }

        return string;
    }

    private void auditSafely(
            String userId,
            String action,
            String outcome,
            Map<String, Object> details,
            RuntimeException originalException) {
        try {
            userAuditService.record(
                    userId,
                    action,
                    outcome,
                    details);
        } catch (RuntimeException auditException) {
            logger.error(
                    "Could not persist user audit event; action={}; outcome={}",
                    action,
                    outcome);

            originalException.addSuppressed(auditException);
        }
    }
}
