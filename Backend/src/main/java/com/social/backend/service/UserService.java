package com.social.backend.service;

import com.social.backend.model.entity.UserEntity;

import java.util.Map;

public interface UserService {
    UserEntity getUserById(String id);
    void updateProfile(String uid, Map<String, Object> updates);
}
