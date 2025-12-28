package com.social.backend.controller;

import com.social.backend.model.dto.response.ApiResponse;
import com.social.backend.model.entity.GroupEntity;
import com.social.backend.security.SecurityUtils;
import com.social.backend.service.GroupService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@SecurityRequirement(name = "Bearer Authentication")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ApiResponse<String> createGroup(@RequestBody GroupEntity group) {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        group.setOwnerId(uid);
        String id = groupService.createGroup(group);
        return ApiResponse.success(id, "Group created");
    }

    @GetMapping
    public ApiResponse<List<GroupEntity>> getGroups(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(groupService.getAllGroups(limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<GroupEntity> getGroupById(@PathVariable String id) {
        GroupEntity group = groupService.getGroupById(id);
        if (group == null) {
            return ApiResponse.error(404, "Group not found");
        }
        return ApiResponse.success(group);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable String id) {
        groupService.deleteGroup(id);
        return ApiResponse.success(null, "Group deleted");
    }
}
