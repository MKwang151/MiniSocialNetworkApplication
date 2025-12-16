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
      const message = {
        notification: {
          title: notification.title || "Mini Social Network",
          body: notification.message || "You have a new notification",
        },
        data: {
          notificationId: context.params.notificationId,
          type: notification.type || "",
          ...(notification.data || {}),
        },
        token: fcmToken,
        android: {
          priority: "high",
          notification: {
            sound: "default",
            clickAction: "FLUTTER_NOTIFICATION_CLICK",
          },
        },
      };

      // Send the message
      const response = await admin.messaging().send(message);
      console.log(`Push notification sent successfully: ${response}`);

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
 * Optional: Clean up old notifications (older than 30 days)
 * Runs daily at midnight
 */
exports.cleanupOldNotifications = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async (context) => {
    const thirtyDaysAgo = Date.now() - (30 * 24 * 60 * 60 * 1000);

    try {
      const oldNotifications = await db.collection("notifications")
        .where("createdAt", "<", thirtyDaysAgo)
        .limit(500) // Process in batches
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
