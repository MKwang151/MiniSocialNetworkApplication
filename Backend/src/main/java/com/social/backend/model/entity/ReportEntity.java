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
public class ReportEntity {
    private String id;
    private String targetId;
    @Builder.Default
    private String targetType = "POST";
    private String reporterId;
    private String reporterName;
    private String authorId;
    private String groupId;
    private String reason;
    private String description;
    @Builder.Default
    private String status = "PENDING";
    private Timestamp createdAt;
}
