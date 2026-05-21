package com.semse.mobile_server.controller;

import com.semse.mobile_server.dto.ApiResponse;
import com.semse.mobile_server.dto.UserResponse;
import com.semse.mobile_server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.semse.mobile_server.dto.RoleUpdateRequest;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers()));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<String>> updateRole(
            @PathVariable String userId,
            @RequestBody RoleUpdateRequest request) {
        userService.updateRole(userId, request.getRole());
        return ResponseEntity.ok(ApiResponse.ok("권한 변경 완료"));
    }
}