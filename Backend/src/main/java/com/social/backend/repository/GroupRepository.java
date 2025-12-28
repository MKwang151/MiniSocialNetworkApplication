package com.social.backend.repository;

import com.google.cloud.firestore.Firestore;
import com.social.backend.model.entity.GroupEntity;
import org.springframework.stereotype.Repository;

@Repository
public class GroupRepository extends AbstractFirestoreRepository<GroupEntity> {
    public GroupRepository(Firestore firestore) {
        super(firestore, "groups", GroupEntity.class);
    }
}
