package com.hrconsultancy.users.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuditRepository
        extends JpaRepository<UserAuditRecord, UUID> {

    List<UserAuditRecord> findByTargetUserIdOrderByOccurredAtDesc(String targetUserId);
}