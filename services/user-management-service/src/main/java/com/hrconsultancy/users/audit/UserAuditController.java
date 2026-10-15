package com.hrconsultancy.users.audit;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrconsultancy.users.dto.UserAuditResponse;

@RestController
@RequestMapping("/api/v1/users/{userId}/audit-logs")
public class UserAuditController {

    private final UserAuditService userAuditService;

    public UserAuditController(UserAuditService userAuditService) {
        this.userAuditService = userAuditService;
    }

    @GetMapping
    public ResponseEntity<List<UserAuditResponse>> getAuditHistory(
            @PathVariable String userId) {

        return ResponseEntity.ok(
                userAuditService.getHistory(userId));
    }
}