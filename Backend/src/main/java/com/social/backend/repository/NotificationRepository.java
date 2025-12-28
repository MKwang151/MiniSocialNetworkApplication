package com.social.backend.repository;

import com.google.cloud.firestore.Firestore;
import com.social.backend.model.entity.NotificationEntity;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository extends AbstractFirestoreRepository<NotificationEntity> {
    public NotificationRepository(Firestore firestore) {
        super(firestore, "notifications", NotificationEntity.class);
    }
}
