package com.social.backend.repository;

import com.google.cloud.firestore.Firestore;
import com.social.backend.model.entity.ReportEntity;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRepository extends AbstractFirestoreRepository<ReportEntity> {
    public ReportRepository(Firestore firestore) {
        super(firestore, "reports", ReportEntity.class);
    }
}
