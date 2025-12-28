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
public class ConversationEntity {
    private String id;
    @Builder.Default
    private String type = "DIRECT";
    private String name;
    private String avatarUrl;
    @Builder.Default
    private List<String> participantIds = Collections.emptyList();
    @Builder.Default
    private List<String> adminIds = Collections.emptyList();
    private LastMessageEntity lastMessage;
    private int unreadCount;
    private boolean isPinned;
    private boolean isMuted;
    @Builder.Default
    private List<String> pinnedMessageIds = Collections.emptyList();
    private String creatorId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
