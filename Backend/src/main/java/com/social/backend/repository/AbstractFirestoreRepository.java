package com.social.backend.repository;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;

public abstract class AbstractFirestoreRepository<T> {

    protected final Firestore firestore;
    private final String collectionName;
    private final Class<T> clazz;

    protected AbstractFirestoreRepository(Firestore firestore, String collectionName, Class<T> clazz) {
        this.firestore = firestore;
        this.collectionName = collectionName;
        this.clazz = clazz;
    }

    public ApiFuture<WriteResult> save(String id, T entity) {
        return firestore.collection(collectionName).document(id).set(entity);
    }

    public ApiFuture<T> findById(String id) {
        return ApiFutures.transform(
                firestore.collection(collectionName).document(id).get(),
                snapshot -> snapshot.toObject(clazz),
                MoreExecutors.directExecutor()
        );
    }

    public ApiFuture<WriteResult> delete(String id) {
        return firestore.collection(collectionName).document(id).delete();
    }

    public ApiFuture<List<T>> findAll(int limit) {
        return ApiFutures.transform(
                firestore.collection(collectionName).limit(limit).get(),
                snapshot -> snapshot.toObjects(clazz),
                MoreExecutors.directExecutor()
        );
    }
}
