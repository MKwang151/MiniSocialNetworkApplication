package com.social.backend.service;

import com.social.backend.model.entity.CommentEntity;
import com.social.backend.model.entity.PostEntity;

import java.util.List;

public interface PostService {
    String createPost(PostEntity post);
    PostEntity getPostById(String id);
    List<PostEntity> getPosts(int limit);
    String addComment(CommentEntity comment);
}
