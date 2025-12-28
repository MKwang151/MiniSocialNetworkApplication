package com.social.backend.service.impl;

import com.social.backend.model.entity.FriendEntity;
import com.social.backend.service.FriendService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class FriendServiceImpl implements FriendService {

    @Override
    public void sendFriendRequest(String senderId, String receiverId) {
        // Implementation: Add senderId to receiver's 'receivedRequests' subcollection
        // Add receiverId to sender's 'sentRequests' subcollection
    }

    @Override
    public void acceptFriendRequest(String userId, String friendId) {
        // Implementation: Move from requests to friends for both users
    }

    @Override
    public void removeFriend(String userId, String friendId) {
        // Implementation: Remove from friends collection for both users
    }

    @Override
    public List<FriendEntity> getFriends(String userId) {
        // Implementation: Query 'friends' subcollection of user
        return Collections.emptyList();
    }

    @Override
    public List<FriendEntity> getFriendRequests(String userId) {
        // Implementation: Query 'receivedRequests' subcollection of user
        return Collections.emptyList();
    }
}
