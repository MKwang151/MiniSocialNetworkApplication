package com.social.backend.service.impl;

import com.social.backend.model.entity.CommentEntity;
import com.social.backend.model.entity.PostEntity;
import com.social.backend.repository.CommentRepository;
import com.social.backend.repository.PostRepository;
import com.social.backend.service.PostService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public PostServiceImpl(PostRepository postRepository, CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    public String createPost(PostEntity post) {
        String id = UUID.randomUUID().toString();
        post.setId(id);
        try {
            postRepository.save(id, post).get();
            return id;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error creating post", e);
        }
    }

    @Override
    public PostEntity getPostById(String id) {
        try {
            return postRepository.findById(id).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching post", e);
        }
    }

    @Override
    public List<PostEntity> getPosts(int limit) {
        try {
            return postRepository.findAll(limit).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching posts", e);
        }
    }

    @Override
    public String addComment(CommentEntity comment) {
        String id = UUID.randomUUID().toString();
        comment.setId(id);
        try {
            commentRepository.save(id, comment).get();
            return id;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error adding comment", e);
        }
    }
}
