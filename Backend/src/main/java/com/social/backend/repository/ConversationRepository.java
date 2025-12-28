package com.social.backend.repository;

import com.google.cloud.firestore.Firestore;
import com.social.backend.model.entity.ConversationEntity;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationRepository extends AbstractFirestoreRepository<ConversationEntity> {
    public ConversationRepository(Firestore firestore) {
        super(firestore, "conversations", ConversationEntity.class);
    }
}
