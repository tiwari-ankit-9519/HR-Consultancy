
package com.hrconsultancy.users.profile;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakUserId(String keycloakUserId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByOrganizationId(UUID organizationId);

    Optional<User> findByStatus(String status);

    boolean existsByKeycloakUserId(String keycloakUserId);

    void deleteByKeycloakUserId(String keycloakUserId);
}
