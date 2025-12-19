const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();

/**
 * Cloud Function: Send push notification when a new notification document is created
 * Triggers on: notifications/{notificationId} create
 */
exports.sendPushNotificationOnCreate = functions.firestore
  .document("notifications/{notificationId}")
  .onCreate(async (snap, context) => {
    const notification = snap.data();
    const userId = notification.userId;

    console.log(`New notification for user ${userId}: ${notification.title}`);

    if (!userId) {
      console.error("No userId in notification");
      return null;
    }

    try {
      // Get the user's FCM token
      const userDoc = await db.collection("users").doc(userId).get();

      if (!userDoc.exists) {
        console.log(`User ${userId} not found`);
        return null;
      }

      const userData = userDoc.data();
      const fcmToken = userData.fcmToken;

      if (!fcmToken) {
        console.log(`No FCM token for user ${userId}`);
        return null;
      }

      // Build the FCM message
      // Flatten data object: combine base fields and notification.data
      const dataPayload = {
        notificationId: context.params.notificationId,
        type: notification.type || "",
        click_action: "OPEN_NOTIFICATION",
      };

      // Add all fields from notification.data directly to the payload
      if (notification.data) {
        Object.keys(notification.data).forEach((key) => {
          // FCM data values MUST be strings
          dataPayload[key] = String(notification.data[key]);
        });
      }

      const message = {
        notification: {
          title: notification.title || "Mini Social Network",
          body: notification.message || "You have a new notification",
        },
        data: dataPayload,
        token: fcmToken,
        android: {
          priority: "high",
          notification: {
            sound: "default",
            channelId: "mini_social_channel",
            defaultSound: true,
            defaultVibrateTimings: true,
          },
        },
      };

      // Send the message
      const response = await admin.messaging().send(message);
      console.log(`Push notification sent successfully to ${userId}`);

      return response;
    } catch (error) {
      console.error("Error sending push notification:", error);

      // If token is invalid, remove it from user document
      if (error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered") {
        console.log(`Removing invalid FCM token for user ${userId}`);
        await db.collection("users").doc(userId).update({
          fcmToken: admin.firestore.FieldValue.delete(),
        });
      }

      return null;
    }
  });

/**
 * Cloud Function: Create notification when a new message is sent
 * Triggers on: conversations/{conversationId}/messages/{messageId} create
 */
exports.onMessageCreate = functions.firestore
  .document("conversations/{conversationId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const message = snap.data();
    const conversationId = context.params.conversationId;
    const senderId = message.senderId;

    if (!senderId) {
      console.error("No senderId in message");
      return null;
    }

    try {
      // Get conversation participants
      const conversationDoc = await db.collection("conversations").doc(conversationId).get();
      if (!conversationDoc.exists) {
        console.error(`Conversation ${conversationId} not found`);
        return null;
      }

      const participantIds = conversationDoc.data().participantIds || [];
      const senderName = message.senderName || "Someone";
      const messageContent = message.content || "Sent an attachment";
      const messagePreview = messageContent.length > 50 ? messageContent.substring(0, 50) + "..." : messageContent;

      // Create notification for each participant except the sender
      const notificationPromises = participantIds
        .filter(uid => uid !== senderId)
        .map(async (recipientId) => {
          const notificationId = db.collection("notifications").doc().id;
          const notificationData = {
            id: notificationId,
            userId: recipientId,
            type: "NEW_MESSAGE",
            title: `New message from ${senderName}`,
            message: messagePreview,
            data: {
              conversationId: conversationId,
              senderId: senderId,
              senderName: senderName
            },
            read: false,
            createdAt: Date.now()
          };

          return db.collection("notifications").doc(notificationId).set(notificationData);
        });

      await Promise.all(notificationPromises);
      console.log(`Created notifications for ${notificationPromises.length} participants in conversation ${conversationId}`);

      return null;
    } catch (error) {
      console.error(`Error processing message notification for conversation ${conversationId}:`, error);
      return null;
    }
  });

/**
 * Cloud Function: Sync user profile info (name, avatar) across all posts and comments
 * Triggers on: users/{userId} update
 */
exports.syncUserProfile = functions.firestore
  .document("users/{userId}")
  .onUpdate(async (change, context) => {
    const newValue = change.after.data();
    const previousValue = change.before.data();
    const userId = context.params.userId;

    // Only sync if name or avatarUrl changed
    const nameChanged = newValue.name !== previousValue.name;
    const avatarChanged = newValue.avatarUrl !== previousValue.avatarUrl;

    if (!nameChanged && !avatarChanged) {
      return null;
    }

    console.log(`Syncing profile for user ${userId}: nameChanged=${nameChanged}, avatarChanged=${avatarChanged}`);

    const updates = {};
    if (nameChanged) updates.authorName = newValue.name;
    if (avatarChanged) updates.authorAvatarUrl = newValue.avatarUrl;

    try {
      // 1. Update all posts by this user
      const postsSnapshot = await db.collection("posts")
        .where("authorId", "==", userId)
        .get();

      if (!postsSnapshot.empty) {
        const batch = db.batch();
        postsSnapshot.docs.forEach((doc) => {
          batch.update(doc.ref, updates);
        });
        await batch.commit();
        console.log(`Updated ${postsSnapshot.size} posts for user ${userId}`);
      }

      // 2. Update all comments by this user
      // Note: In this app, comments are subcollections of posts. 
      // Firestore does not support collection group updates as easily, 
      // but we can use a collectionGroup query to find all comments by this user.
      const commentsSnapshot = await db.collectionGroup("comments")
        .where("authorId", "==", userId)
        .get();

      if (!commentsSnapshot.empty) {
        const batch = db.batch();
        commentsSnapshot.docs.forEach((doc) => {
          batch.update(doc.ref, updates);
        });
        await batch.commit();
        console.log(`Updated ${commentsSnapshot.size} comments for user ${userId}`);
      }

      return null;
    } catch (error) {
      console.error(`Error syncing profile for user ${userId}:`, error);
      return null;
    }
  });

/**
 * Optional: Clean up old notifications (older than 30 days)
 * Runs daily at midnight
 */
exports.cleanupOldNotifications = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async (context) => {
    // 30 days in milliseconds
    const thirtyDaysMs = 30 * 24 * 60 * 60 * 1000;
    const cutoffDate = Date.now() - thirtyDaysMs;

    try {
      const oldNotifications = await db.collection("notifications")
        .where("createdAt", "<", cutoffDate)
        .limit(500)
        .get();

      if (oldNotifications.empty) {
        console.log("No old notifications to clean up");
        return null;
      }

      const batch = db.batch();
      oldNotifications.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      console.log(`Deleted ${oldNotifications.size} old notifications`);

      return null;
    } catch (error) {
      console.error("Error cleaning up notifications:", error);
      return null;
    }
  });

/**
 * Cloud Function: Sync group info (name, avatar) across posts, invitations, and join requests
 * Triggers on: groups/{groupId} update
 */
exports.onGroupUpdate = functions.firestore
  .document("groups/{groupId}")
  .onUpdate(async (change, context) => {
    const newValue = change.after.data();
    const previousValue = change.before.data();
    const groupId = context.params.groupId;

    const nameChanged = newValue.name !== previousValue.name;
    const avatarChanged = newValue.avatarUrl !== previousValue.avatarUrl;

    if (!nameChanged && !avatarChanged) {
      return null;
    }

    console.log(`Syncing group ${groupId}: nameChanged=${nameChanged}, avatarChanged=${avatarChanged}`);

    const postUpdates = {};
    if (nameChanged) postUpdates.groupName = newValue.name;
    if (avatarChanged) postUpdates.groupAvatarUrl = newValue.avatarUrl;

    const invitationUpdates = {};
    if (nameChanged) invitationUpdates.groupName = newValue.name;
    if (avatarChanged) invitationUpdates.groupAvatarUrl = newValue.avatarUrl;

    const joinRequestUpdates = {};
    if (nameChanged) joinRequestUpdates.groupName = newValue.name;

    try {
      const promises = [];

      // 1. Update posts
      const postsSnapshot = await db.collection("posts")
        .where("groupId", "==", groupId)
        .get();
      if (!postsSnapshot.empty) {
        const batch = db.batch();
        postsSnapshot.docs.forEach(doc => batch.update(doc.ref, postUpdates));
        promises.push(batch.commit());
      }

      // 2. Update group_invitations
      const invitationsSnapshot = await db.collection("group_invitations")
        .where("groupId", "==", groupId)
        .get();
      if (!invitationsSnapshot.empty) {
        const batch = db.batch();
        invitationsSnapshot.docs.forEach(doc => batch.update(doc.ref, invitationUpdates));
        promises.push(batch.commit());
      }

      // 3. Update join_requests
      if (nameChanged) {
        const requestsSnapshot = await db.collection("join_requests")
          .where("groupId", "==", groupId)
          .get();
        if (!requestsSnapshot.empty) {
          const batch = db.batch();
          requestsSnapshot.docs.forEach(doc => batch.update(doc.ref, joinRequestUpdates));
          promises.push(batch.commit());
        }
      }

      await Promise.all(promises);
      console.log(`Sync completed for group ${groupId}`);
      return null;
    } catch (error) {
      console.error(`Error syncing group ${groupId}:`, error);
      return null;
    }
  });

/**
 * Cloud Function: Clean up all group-related data when a group is deleted
 * Triggers on: groups/{groupId} delete
 */
exports.onGroupDelete = functions.firestore
  .document("groups/{groupId}")
  .onDelete(async (snap, context) => {
    const groupId = context.params.groupId;
    console.log(`Cleaning up data for deleted group ${groupId}`);

    try {
      const promises = [];

      // 1. Delete all posts and their images
      const postsSnapshot = await db.collection("posts")
        .where("groupId", "==", groupId)
        .get();

      if (!postsSnapshot.empty) {
        const storage = admin.storage();
        postsSnapshot.docs.forEach(async (doc) => {
          const postData = doc.data();
          const mediaUrls = postData.mediaUrls || [];

          // Delete Storage images
          mediaUrls.forEach(async (url) => {
            try {
              // Convert download URL to storage path or use getFileFromUrl logic
              // Simple way if URLs are standard:
              const bucket = storage.bucket();
              const path = decodeURIComponent(url.split("/o/")[1].split("?")[0]);
              await bucket.file(path).delete();
            } catch (e) {
              console.warn(`Failed to delete storage file: ${url}`, e);
            }
          });

          // Delete post document
          promises.push(doc.ref.delete());
        });
      }

      // 2. Delete Invitations
      const invitationsSnapshot = await db.collection("group_invitations")
        .where("groupId", "==", groupId)
        .get();
      invitationsSnapshot.docs.forEach(doc => promises.push(doc.ref.delete()));

      // 3. Delete Join Requests
      const requestsSnapshot = await db.collection("join_requests")
        .where("groupId", "==", groupId)
        .get();
      requestsSnapshot.docs.forEach(doc => promises.push(doc.ref.delete()));

      // 4. Delete Members (subcollection)
      const membersSnapshot = await db.collection("groups").doc(groupId).collection("members").get();
      membersSnapshot.docs.forEach(doc => promises.push(doc.ref.delete()));

      await Promise.all(promises);
      console.log(`Cleanup completed for group ${groupId}`);
      return null;
    } catch (error) {
      console.error(`Error cleaning up group ${groupId}:`, error);
      return null;
    }
  });
/**
 * Cloud Function: Update Chat Group metadata and notify all participants
 * Call this when group name or avatar changes
 */
exports.updateChatGroup = functions.https.onCall(async (data, context) => {
  // Check auth
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be logged in");
  }

  const { conversationId, name, avatarUrl } = data;
  if (!conversationId) {
    throw new functions.https.HttpsError("invalid-argument", "conversationId is required");
  }

  try {
    const conversationRef = db.collection("conversations").doc(conversationId);
    const conversationDoc = await conversationRef.get();

    if (!conversationDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Conversation not found");
    }

    const conversationData = conversationDoc.data();

    // Authorization: Only admin can update
    const isAdmin = conversationData.adminIds && conversationData.adminIds.includes(context.auth.uid);
    if (!isAdmin) {
      throw new functions.https.HttpsError("permission-denied", "Only admins can update group settings");
    }

    const updates = {};
    if (name) updates.name = name;
    if (avatarUrl) updates.avatarUrl = avatarUrl;
    updates.updatedAt = Date.now();

    await conversationRef.update(updates);
    console.log(`Updated conversation ${conversationId} metadata`);

    // Note: We don't need a trigger because clients can listen to doc changes,
    // but if we want to force a refresh event via notifications:
    const participantIds = conversationData.participantIds || [];
    const notificationPromises = participantIds
      .filter(uid => uid !== context.auth.uid)
      .map(async (recipientId) => {
        const notificationId = db.collection("notifications").doc().id;
        return db.collection("notifications").doc(notificationId).set({
          id: notificationId,
          userId: recipientId,
          type: "GROUP_METADATA_UPDATE",
          title: "Group Updated",
          message: `Group "${name || conversationData.name}" info has been updated`,
          data: {
            conversationId: conversationId
          },
          read: false,
          createdAt: Date.now()
        });
      });

    await Promise.all(notificationPromises);

    return { success: true };
  } catch (error) {
    console.error("Error updating chat group:", error);
    throw new functions.https.HttpsError("internal", error.message);
  }
});
