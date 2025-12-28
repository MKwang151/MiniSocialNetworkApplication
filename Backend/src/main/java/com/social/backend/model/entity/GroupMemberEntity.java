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
public class GroupMemberEntity {
    private String id;
    private String groupId;
    private String memberId;
    private String memberName;
    private String memberAvatarUrl;
    @Builder.Default
    private String role = "MEMBER"; // CREATOR, ADMIN, MEMBER
    private Timestamp joinedAt;
}
