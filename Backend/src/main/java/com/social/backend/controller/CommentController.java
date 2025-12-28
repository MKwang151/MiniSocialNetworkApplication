package com.social.backend.controller;

import com.social.backend.model.dto.response.ApiResponse;
import com.social.backend.model.entity.CommentEntity;
import com.social.backend.security.SecurityUtils;
import com.social.backend.service.PostService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comments")
@SecurityRequirement(name = "Bearer Authentication")
public class CommentController {

    private final PostService postService;

    public CommentController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ApiResponse<String> addComment(@RequestBody CommentEntity comment) {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        comment.setAuthorId(uid);
        String id = postService.addComment(comment);
        return ApiResponse.success(id, "Comment added");
    }
}
