package com.social.backend.repository;

import com.google.cloud.firestore.Firestore;
import com.social.backend.model.entity.MessageEntity;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository extends AbstractFirestoreRepository<MessageEntity> {
    public MessageRepository(Firestore firestore) {
        super(firestore, "messages", MessageEntity.class);
    }
}
