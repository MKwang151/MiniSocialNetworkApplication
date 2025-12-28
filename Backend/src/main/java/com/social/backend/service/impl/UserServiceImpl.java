package com.social.backend.service.impl;

import com.social.backend.model.entity.UserEntity;
import com.social.backend.repository.UserRepository;
import com.social.backend.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserEntity getUserById(String id) {
        try {
            return userRepository.findById(id).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching user", e);
        }
    }

    @Override
    public void updateProfile(String uid, Map<String, Object> updates) {
        UserEntity user = getUserById(uid);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        // Simplified update logic
        try {
            userRepository.save(uid, user).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error updating user", e);
        }
    }
}
