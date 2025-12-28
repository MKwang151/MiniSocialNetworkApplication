package com.social.backend.service;

import com.social.backend.model.entity.GroupEntity;

import java.util.List;

public interface GroupService {
    String createGroup(GroupEntity group);
    GroupEntity getGroupById(String id);
    List<GroupEntity> getAllGroups(int limit);
    void deleteGroup(String id);
}
