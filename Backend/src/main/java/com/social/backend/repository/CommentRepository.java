package com.social.backend.repository;

import com.google.cloud.firestore.Firestore;
import com.social.backend.model.entity.CommentEntity;
import org.springframework.stereotype.Repository;

@Repository
public class CommentRepository extends AbstractFirestoreRepository<CommentEntity> {
    public CommentRepository(Firestore firestore) {
        super(firestore, "comments", CommentEntity.class);
    }
}
