package com.social.backend.service.impl;

import com.social.backend.model.entity.GroupEntity;
import com.social.backend.repository.GroupRepository;
import com.social.backend.service.GroupService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;

    public GroupServiceImpl(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Override
    public String createGroup(GroupEntity group) {
        String id = UUID.randomUUID().toString();
        group.setId(id);
        try {
            groupRepository.save(id, group).get();
            return id;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error creating group", e);
        }
    }

    @Override
    public GroupEntity getGroupById(String id) {
        try {
            return groupRepository.findById(id).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching group", e);
        }
    }

    @Override
    public List<GroupEntity> getAllGroups(int limit) {
        try {
            return groupRepository.findAll(limit).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching groups", e);
        }
    }

    @Override
    public void deleteGroup(String id) {
        try {
            groupRepository.delete(id).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting group", e);
        }
    }
}
