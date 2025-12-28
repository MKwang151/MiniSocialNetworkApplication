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
public class UserEntity {
    private String uid;
    private String name;
    private String email;
    private String avatarUrl;
    private String bio;
    private String fcmToken;
    private boolean online;
    private Timestamp lastActive;
    private Timestamp createdAt;
    @Builder.Default
    private String role = "USER";
    @Builder.Default
    private String status = "ACTIVE";
}
