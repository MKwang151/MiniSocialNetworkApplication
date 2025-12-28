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
public class LastMessageEntity {
    private String text;
    @Builder.Default
    private String type = "TEXT";
    private String senderId;
    private String senderName;
    private long sequenceId;
    private Timestamp timestamp;
}
