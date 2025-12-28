package com.social.backend.model.entity;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEntity {
    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String text;
    @Builder.Default
    private List<String> mediaUrls = Collections.emptyList();
    private int likeCount;
    private int commentCount;
    private Timestamp createdAt;
    private String groupId;
    @Builder.Default
    private String approvalStatus = "APPROVED";
    private boolean isPinned;
    private boolean isHidden;
}
