package com.social.backend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {
    private String id;
    private String userId;
    @Builder.Default
    private String type = "GROUP_INVITATION";
    private String title;
    private String message;
    @Builder.Default
    private Map<String, String> data = Collections.emptyMap();
    private boolean isRead;
    private long createdAt;
}
