package com.social.backend.controller;

import com.social.backend.model.dto.response.ApiResponse;
import com.social.backend.model.entity.FriendEntity;
import com.social.backend.security.SecurityUtils;
import com.social.backend.service.FriendService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@SecurityRequirement(name = "Bearer Authentication")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public ApiResponse<List<FriendEntity>> getFriends() {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        return ApiResponse.success(friendService.getFriends(uid));
    }

    @GetMapping("/requests")
    public ApiResponse<List<FriendEntity>> getFriendRequests() {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        return ApiResponse.success(friendService.getFriendRequests(uid));
    }

    @PostMapping("/request/{friendId}")
    public ApiResponse<Void> sendFriendRequest(@PathVariable String friendId) {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        friendService.sendFriendRequest(uid, friendId);
        return ApiResponse.success(null, "Friend request sent");
    }

    @PostMapping("/accept/{friendId}")
    public ApiResponse<Void> acceptFriendRequest(@PathVariable String friendId) {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        friendService.acceptFriendRequest(uid, friendId);
        return ApiResponse.success(null, "Friend request accepted");
    }

    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> removeFriend(@PathVariable String friendId) {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        friendService.removeFriend(uid, friendId);
        return ApiResponse.success(null, "Friend removed");
    }
}
