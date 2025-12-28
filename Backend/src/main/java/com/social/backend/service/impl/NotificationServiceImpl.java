package com.social.backend.service.impl;

import com.social.backend.model.entity.NotificationEntity;
import com.social.backend.repository.NotificationRepository;
import com.social.backend.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public String createNotification(NotificationEntity notification) {
        String id = UUID.randomUUID().toString();
        notification.setId(id);
        try {
            notificationRepository.save(id, notification).get();
            return id;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error creating notification", e);
        }
    }

    @Override
    public List<NotificationEntity> getNotificationsForUser(String userId, int limit) {
        try {
            // A more advanced query would filter by userId
            return notificationRepository.findAll(limit).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching notifications", e);
        }
    }

    @Override
    public void markAsRead(String notificationId) {
        // Implementation would update the 'isRead' field
        NotificationEntity notification;
        try {
            notification = notificationRepository.findById(notificationId).get();
            if (notification != null) {
                notification.setRead(true);
                notificationRepository.save(notificationId, notification).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error marking notification as read", e);
        }
    }
}
