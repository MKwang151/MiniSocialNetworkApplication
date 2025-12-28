package com.social.backend.controller;

import com.social.backend.model.dto.response.ApiResponse;
import com.social.backend.model.entity.PostEntity;
import com.social.backend.security.SecurityUtils;
import com.social.backend.service.PostService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@SecurityRequirement(name = "Bearer Authentication")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ApiResponse<String> createPost(@RequestBody PostEntity post) {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        post.setAuthorId(uid);
        String id = postService.createPost(post);
        return ApiResponse.success(id, "Post created");
    }

    @GetMapping
    public ApiResponse<List<PostEntity>> getPosts(@RequestParam(defaultValue = "10") int limit) {
        List<PostEntity> posts = postService.getPosts(limit);
        return ApiResponse.success(posts);
    }

    @GetMapping("/{id}")
    public ApiResponse<PostEntity> getPostById(@PathVariable String id) {
        PostEntity post = postService.getPostById(id);
        if (post == null) {
            return ApiResponse.error(404, "Post not found");
        }
        return ApiResponse.success(post);
    }
}
