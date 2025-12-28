package com.social.backend.controller;

import com.social.backend.model.dto.response.ApiResponse;
import com.social.backend.model.entity.UserEntity;
import com.social.backend.security.SecurityUtils;
import com.social.backend.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserEntity> getCurrentUser() {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        UserEntity user = userService.getUserById(uid);
        if (user == null) {
            return ApiResponse.error(404, "User not found");
        }
        return ApiResponse.success(user);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserEntity> getUserById(@PathVariable String id) {
        UserEntity user = userService.getUserById(id);
        if (user == null) {
            return ApiResponse.error(404, "User not found");
        }
        return ApiResponse.success(user);
    }

    @PatchMapping("/me")
    public ApiResponse<Void> updateProfile(@RequestBody Map<String, Object> updates) {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        userService.updateProfile(uid, updates);
        return ApiResponse.success(null, "Profile updated");
    }
}
