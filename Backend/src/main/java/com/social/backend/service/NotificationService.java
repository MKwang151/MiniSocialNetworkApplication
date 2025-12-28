package com.social.backend.service;

import com.social.backend.model.entity.NotificationEntity;

import java.util.List;

public interface NotificationService {
    String createNotification(NotificationEntity notification);
    List<NotificationEntity> getNotificationsForUser(String userId, int limit);
    void markAsRead(String notificationId);
}
