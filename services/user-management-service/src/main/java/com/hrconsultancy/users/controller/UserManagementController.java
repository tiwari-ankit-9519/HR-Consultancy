package com.hrconsultancy.users.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrconsultancy.users.dto.CreateUserRequest;
import com.hrconsultancy.users.dto.UserResponse;
import com.hrconsultancy.users.service.UserManagementService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/v1/users")
public class UserManagementController {
    private final UserManagementService userManagementService;

    public UserManagementController(
            UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userManagementService.createUser(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userManagementService.getUser(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUser(
            @RequestParam(defaultValue = "0") @Min(0) int first,

            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int max) {
        return ResponseEntity.ok(userManagementService.listUsers(first, max));
    }

}