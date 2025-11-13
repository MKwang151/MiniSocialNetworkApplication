# Firestore Database Schema

This document describes the Firestore database structure for the Mini Social Network Application.

## Collections Overview

```
firestore/
├── users/                    # User profiles
│   └── {uid}/               # Document per user
├── posts/                    # All posts
│   ├── {postId}/            # Post document
│   │   └── comments/        # Subcollection of comments
│   │       └── {commentId}/ # Comment document
└── likes/                    # All likes (flat collection)
    └── {likeId}/            # Like document
```

## Collection: `users`

Stores user profile information.

**Document Path**: `users/{uid}`

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| uid | string | Yes | User's Firebase Auth UID (same as document ID) |
| name | string | Yes | Display name |
| email | string | Yes | Email address |
| avatarUrl | string | No | Profile picture URL from Firebase Storage |
| bio | string | No | User bio/description (max 200 chars) |
| fcmToken | string | No | Firebase Cloud Messaging token for notifications |
| createdAt | timestamp | Yes | Account creation timestamp |

### Example Document

```json
{
  "uid": "abc123xyz",
  "name": "John Doe",
  "email": "john@example.com",
  "avatarUrl": "https://storage.googleapis.com/bucket/avatars/abc123xyz.jpg",
  "bio": "Software developer and coffee enthusiast ☕",
  "fcmToken": "fYq8X...",
  "createdAt": {
    "_seconds": 1699876543,
    "_nanoseconds": 0
  }
}
```

### Indexes

- Automatic: `uid` (document ID)
- Custom: None required for MVP

---

## Collection: `posts`

Stores all user posts.

**Document Path**: `posts/{postId}`

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | Yes | Post ID (same as document ID) |
| authorId | string | Yes | UID of the user who created the post |
| authorName | string | Yes | Cached display name (denormalized for performance) |
| authorAvatarUrl | string | No | Cached avatar URL (denormalized) |
| text | string | Yes | Post text content (max 1000 chars) |
| mediaUrls | array | No | Array of image URLs from Storage (max 3) |
| likeCount | number | Yes | Total number of likes (denormalized) |
| commentCount | number | Yes | Total number of comments (denormalized) |
| createdAt | timestamp | Yes | Post creation timestamp |

### Example Document

```json
{
  "id": "post_abc123",
  "authorId": "abc123xyz",
  "authorName": "John Doe",
  "authorAvatarUrl": "https://storage.googleapis.com/.../abc123xyz.jpg",
  "text": "Just launched my new Android app! 🚀",
  "mediaUrls": [
    "https://storage.googleapis.com/.../post1_img1.jpg",
    "https://storage.googleapis.com/.../post1_img2.jpg"
  ],
  "likeCount": 42,
  "commentCount": 5,
  "createdAt": {
    "_seconds": 1699880000,
    "_nanoseconds": 0
  }
}
```

### Indexes

Required for efficient queries:

1. **Feed Query**: `createdAt` (descending)
   ```
   Collection: posts
   Fields: createdAt (Descending)
   ```

2. **User Posts Query**: `authorId` (ascending) + `createdAt` (descending)
   ```
   Collection: posts
   Fields: authorId (Ascending), createdAt (Descending)
   ```

### Composite Index Creation (Firebase Console)

```javascript
// Via Firebase Console > Firestore > Indexes
{
  collectionId: "posts",
  fields: [
    { fieldPath: "authorId", mode: "ASCENDING" },
    { fieldPath: "createdAt", mode: "DESCENDING" }
  ]
}
```

---

## Subcollection: `posts/{postId}/comments`

Stores comments for each post as a subcollection.

**Document Path**: `posts/{postId}/comments/{commentId}`

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | Yes | Comment ID (same as document ID) |
| postId | string | Yes | Parent post ID |
| authorId | string | Yes | UID of comment author |
| authorName | string | Yes | Cached display name |
| authorAvatarUrl | string | No | Cached avatar URL |
| text | string | Yes | Comment text (max 500 chars) |
| createdAt | timestamp | Yes | Comment creation timestamp |

### Example Document

```json
{
  "id": "comment_xyz789",
  "postId": "post_abc123",
  "authorId": "def456uvw",
  "authorName": "Jane Smith",
  "authorAvatarUrl": "https://storage.googleapis.com/.../def456uvw.jpg",
  "text": "Congratulations! Looking forward to trying it out! 🎉",
  "createdAt": {
    "_seconds": 1699881000,
    "_nanoseconds": 0
  }
}
```

### Indexes

- **Comments List Query**: `createdAt` (ascending or descending)
  ```
  Collection Group: comments
  Fields: createdAt (Ascending)
  ```

### Notes
- Comments increment `commentCount` in parent post (via transaction)
- Real-time updates via Snapshot Listener
- Comments can be deleted by author OR post owner

---

## Collection: `likes`

Stores all likes in a flat collection for efficient querying.

**Document Path**: `likes/{likeId}`

**Document ID Format**: `{postId}_{userId}` (composite key)

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | Yes | Like ID (postId_userId) |
| postId | string | Yes | Post being liked |
| userId | string | Yes | User who liked |
| createdAt | timestamp | Yes | Like timestamp |

### Example Document

```json
{
  "id": "post_abc123_def456uvw",
  "postId": "post_abc123",
  "userId": "def456uvw",
  "createdAt": {
    "_seconds": 1699882000,
    "_nanoseconds": 0
  }
}
```

### Indexes

1. **Check if user liked post**: `postId` + `userId`
   ```
   Collection: likes
   Fields: postId (Ascending), userId (Ascending)
   ```

2. **Get all likes for a post**: `postId` (ascending)
   ```
   Collection: likes
   Fields: postId (Ascending)
   ```

3. **Get all posts user liked**: `userId` (ascending)
   ```
   Collection: likes
   Fields: userId (Ascending)
   ```

### Like/Unlike Flow

**Toggle Like Transaction**:
```kotlin
// 1. Check if like exists: likes/{postId}_{userId}
// 2. If exists: Delete like doc, decrement post.likeCount
// 3. If not exists: Create like doc, increment post.likeCount
```

---

## Data Denormalization Strategy

To optimize read performance, we denormalize certain data:

### Denormalized Fields in Posts
- `authorName` - From `users/{authorId}/name`
- `authorAvatarUrl` - From `users/{authorId}/avatarUrl`

### Denormalized Fields in Comments
- `authorName` - From `users/{authorId}/name`
- `authorAvatarUrl` - From `users/{authorId}/avatarUrl`

### Counters
- `posts/{postId}/likeCount` - Count of documents in `likes` where `postId` matches
- `posts/{postId}/commentCount` - Count of documents in `posts/{postId}/comments`

### Why Denormalize?
1. **Read Performance**: Avoid multiple document reads
2. **Offline Support**: All data in one document for Room cache
3. **Cost Reduction**: Fewer Firestore reads

### When to Update Denormalized Data?
- **User profile change**: Update all posts/comments by that user (Cloud Function or batch job)
- **Like/Unlike**: Update `likeCount` in transaction
- **Comment add/delete**: Update `commentCount` in transaction

---

## Firestore Rules Summary

See `README.md` for complete security rules.

**Key Rules**:
- ✅ Anyone can read posts and comments (public feed)
- ✅ Only authenticated users can create posts/comments
- ✅ Only post/comment author can delete their content
- ✅ Post owners can also delete comments on their posts
- ✅ Users can only like/unlike with their own UID
- ✅ Users can only edit their own profile

---

## Cloud Functions (Optional for MVP)

### Function: `onCommentCreate`

Trigger push notification to post author when someone comments.

```javascript
exports.onCommentCreate = functions.firestore
  .document('posts/{postId}/comments/{commentId}')
  .onCreate(async (snap, context) => {
    const comment = snap.data();
    const postId = context.params.postId;
    
    // Get post to find author
    const post = await admin.firestore()
      .doc(`posts/${postId}`).get();
    const postAuthorId = post.data().authorId;
    
    // Don't notify if commenting on own post
    if (postAuthorId === comment.authorId) return;
    
    // Get author's FCM token
    const author = await admin.firestore()
      .doc(`users/${postAuthorId}`).get();
    const fcmToken = author.data().fcmToken;
    
    if (!fcmToken) return;
    
    // Send notification
    await admin.messaging().send({
      token: fcmToken,
      notification: {
        title: `${comment.authorName} commented on your post`,
        body: comment.text
      },
      data: {
        postId: postId,
        type: 'comment'
      }
    });
  });
```

### Function: `onUserProfileUpdate`

Update denormalized user data in posts/comments when profile changes.

```javascript
exports.onUserProfileUpdate = functions.firestore
  .document('users/{userId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    const userId = context.params.userId;
    
    // Check if name or avatar changed
    if (before.name === after.name && 
        before.avatarUrl === after.avatarUrl) {
      return;
    }
    
    const batch = admin.firestore().batch();
    
    // Update posts
    const posts = await admin.firestore()
      .collection('posts')
      .where('authorId', '==', userId)
      .get();
    
    posts.docs.forEach(doc => {
      batch.update(doc.ref, {
        authorName: after.name,
        authorAvatarUrl: after.avatarUrl
      });
    });
    
    // Update comments (collection group query)
    const comments = await admin.firestore()
      .collectionGroup('comments')
      .where('authorId', '==', userId)
      .get();
    
    comments.docs.forEach(doc => {
      batch.update(doc.ref, {
        authorName: after.name,
        authorAvatarUrl: after.avatarUrl
      });
    });
    
    await batch.commit();
  });
```

---

## Migration Strategy

### Adding New Fields
- Add field with default value
- Update code to handle missing field
- Backfill existing documents (Cloud Function or script)

### Changing Schema
- Create new collection
- Dual-write to both collections
- Migrate data
- Update code to read from new collection
- Delete old collection

---

## Backup Strategy

1. **Automatic Backups**: Enable in Firebase Console
2. **Export Schedule**: Weekly exports to Cloud Storage
3. **Point-in-Time Recovery**: Available for Firestore

---

## Performance Optimization

1. **Indexes**: Create composite indexes for complex queries
2. **Caching**: Use Room for offline-first architecture
3. **Pagination**: Limit queries with `limit()` and cursor pagination
4. **Denormalization**: Reduce document reads
5. **Batching**: Use batch writes for multiple updates

---

**Last Updated**: November 2024

