package com.social.backend.model.entity;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEntity {
    private String id;
    private String postId;
    private String authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String text;
    private Timestamp createdAt;
    private String replyToId;
    private boolean isHidden;
}
