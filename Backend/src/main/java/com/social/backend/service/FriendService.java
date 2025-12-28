package com.social.backend.service;

import com.social.backend.model.entity.FriendEntity;

import java.util.List;

public interface FriendService {
    void sendFriendRequest(String senderId, String receiverId);
    void acceptFriendRequest(String userId, String friendId);
    void removeFriend(String userId, String friendId);
    List<FriendEntity> getFriends(String userId);
    List<FriendEntity> getFriendRequests(String userId);
}
