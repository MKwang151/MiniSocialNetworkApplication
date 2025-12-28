package com.social.backend.repository;

import com.google.cloud.firestore.Firestore;
import com.social.backend.model.entity.PostEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PostRepository extends AbstractFirestoreRepository<PostEntity> {
    public PostRepository(Firestore firestore) {
        super(firestore, "posts", PostEntity.class);
    }
}
