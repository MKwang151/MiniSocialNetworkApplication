package com.social.backend.controller;

import com.social.backend.model.dto.response.ApiResponse;
import com.social.backend.security.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Map<String, String>> checkAuth() {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        return ApiResponse.success(Collections.singletonMap("uid", uid), "Authenticated");
    }
}
