package com.social.backend.model.entity;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity {
    private String id;
    private String conversationId;
    private long sequenceId;
    private String senderId;
    private String senderName;
    private String senderAvatarUrl;
    @Builder.Default
    private String type = "TEXT";
    private String content;
    @Builder.Default
    private List<String> mediaUrls = Collections.emptyList();
    private String fileName;
    private Long fileSize;
    private Integer duration;
    private String replyToMessageId;
    @Builder.Default
    private Map<String, List<String>> reactions = Collections.emptyMap();
    @Builder.Default
    private String status = "SENT";
    @Builder.Default
    private List<String> deliveredTo = Collections.emptyList();
    @Builder.Default
    private List<String> seenBy = Collections.emptyList();
    private boolean isRevoked;
    private Timestamp timestamp;
}
