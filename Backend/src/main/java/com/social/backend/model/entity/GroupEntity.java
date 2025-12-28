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
public class GroupEntity {
    private String id;
    private String name;
    private String description;
    private String avatarUrl;
    private String coverUrl;
    private String ownerId;
    @Builder.Default
    private String privacy = "PUBLIC";
    @Builder.Default
    private String postingPermission = "EVERYONE";
    private boolean requirePostApproval;
    private long memberCount;
    private Timestamp createdAt;
    @Builder.Default
    private String status = "ACTIVE";
}
