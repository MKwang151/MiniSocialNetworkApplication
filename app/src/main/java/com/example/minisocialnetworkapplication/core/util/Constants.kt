package com.example.minisocialnetworkapplication.core.util

object Constants {
    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_POSTS = "posts"
    const val COLLECTION_COMMENTS = "comments"
    const val COLLECTION_LIKES = "likes"

    // Firestore Fields
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_AUTHOR_ID = "authorId"
    const val FIELD_POST_ID = "postId"
    const val FIELD_USER_ID = "userId"
    const val FIELD_LIKE_COUNT = "likeCount"
    const val FIELD_COMMENT_COUNT = "commentCount"

    // Pagination
    const val PAGE_SIZE = 20
    const val INITIAL_LOAD_SIZE = 30
    const val PREFETCH_DISTANCE = 5

    // Image
    const val MAX_IMAGE_COUNT = 3
    const val IMAGE_QUALITY = 80
    const val MAX_IMAGE_WIDTH = 1080
    const val MAX_IMAGE_HEIGHT = 1920

    // Validation
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_POST_TEXT_LENGTH = 1000
    const val MAX_COMMENT_TEXT_LENGTH = 500
    const val MAX_BIO_LENGTH = 200

    // Storage
    const val STORAGE_POSTS_PATH = "posts"
    const val STORAGE_AVATARS_PATH = "avatars"

    // Work Manager
    const val WORK_UPLOAD_POST = "upload_post_work"
    const val WORK_TAG_UPLOAD = "upload"

    // DataStore Keys
    const val PREF_USER_ID = "user_id"
    const val PREF_FCM_TOKEN = "fcm_token"
    const val PREF_DARK_MODE = "dark_mode"

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "mini_social_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Mini Social Notifications"
}

