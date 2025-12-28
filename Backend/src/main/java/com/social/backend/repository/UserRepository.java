package com.social.backend.repository;

import com.google.cloud.firestore.Firestore;
import com.social.backend.model.entity.UserEntity;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends AbstractFirestoreRepository<UserEntity> {
    public UserRepository(Firestore firestore) {
        super(firestore, "users", UserEntity.class);
    }
}
