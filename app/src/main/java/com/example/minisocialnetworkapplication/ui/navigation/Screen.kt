package com.example.minisocialnetworkapplication.ui.navigation

sealed class Screen(val route: String) {
    data object AuthCheck : Screen("auth_check")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Feed : Screen("feed")
    data object ComposePost : Screen("compose_post?groupId={groupId}") {
        fun createRoute(groupId: String? = null) = if (groupId != null) "compose_post?groupId=$groupId" else "compose_post"
    }
    data object Notifications : Screen("notifications")
    data object ConversationList : Screen("conversation_list")
    data object ChatDetail : Screen("chat_detail/{conversationId}") {
        fun createRoute(conversationId: String) = "chat_detail/$conversationId"
    }
    data object StartChat : Screen("start_chat/{userId}") {
        fun createRoute(userId: String) = "start_chat/$userId"
    }
    data object Friends : Screen("friends")
    data object SearchUser: Screen("search")
    data object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    data object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    data object EditProfile : Screen("edit_profile/{userId}") {
        fun createRoute(userId: String) = "edit_profile/$userId"
    }
    data object ImageGallery : Screen("image_gallery/{postId}/{initialIndex}") {
        fun createRoute(postId: String, initialIndex: Int = 0) = "image_gallery/$postId/$initialIndex"
    }
    data object ChatSettings : Screen("chat_settings/{conversationId}") {
        fun createRoute(conversationId: String) = "chat_settings/$conversationId"
    }
    data object ChatMedia : Screen("chat_media/{conversationId}") {
        fun createRoute(conversationId: String) = "chat_media/$conversationId"
    }
    data object MessageSearch : Screen("message_search/{conversationId}") {
        fun createRoute(conversationId: String) = "message_search/$conversationId"
    }
    data object SelectParticipants : Screen("select_participants")
    data object CreateGroup : Screen("create_group/{participantIds}") {
        fun createRoute(participantIds: String) = "create_group/$participantIds"
    }

    data object ChatMembers : Screen("chat_members/{conversationId}") {
        fun createRoute(conversationId: String) = "chat_members/$conversationId"
    }
    data object AddMember : Screen("add_member/{conversationId}") {
        fun createRoute(conversationId: String) = "add_member/$conversationId"
    }
    data object JoinRequests : Screen("join_requests/{conversationId}") {
        fun createRoute(conversationId: String) = "join_requests/$conversationId"
    }

    data object GroupList : Screen("group_list")
    data object CreateSocialGroup : Screen("create_social_group")
    data object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    data object GroupInvite : Screen("group_invite/{groupId}") {
        fun createRoute(groupId: String) = "group_invite/$groupId"
    }
    
    data object Report : Screen("report/{targetId}/{targetType}?authorId={authorId}&groupId={groupId}") {
        fun createPostReport(postId: String, authorId: String, groupId: String? = null): String {
            return if (groupId != null) {
                "report/$postId/POST?authorId=$authorId&groupId=$groupId"
            } else {
                "report/$postId/POST?authorId=$authorId"
            }
        }

        fun createUserReport(userId: String): String {
            return "report/$userId/USER"
        }

        fun createGroupReport(groupId: String): String {
            return "report/$groupId/GROUP"
        }
    }

    data object GroupJoinRequests : Screen("group_join_requests/{groupId}") {
        fun createRoute(groupId: String) = "group_join_requests/$groupId"
    }

    data object GroupManagement : Screen("group_management/{groupId}/{groupName}") {
        fun createRoute(groupId: String, groupName: String) = 
            "group_management/$groupId/${java.net.URLEncoder.encode(groupName, "UTF-8")}"
    }

    data object GroupMembers : Screen("group_members/{groupId}") {
        fun createRoute(groupId: String) = "group_members/$groupId"
    }
    
    data object PendingPosts : Screen("pending_posts/{groupId}") {
        fun createRoute(groupId: String) = "pending_posts/$groupId"
    }
    
    data object GroupReports : Screen("group_reports/{groupId}") {
        fun createRoute(groupId: String) = "group_reports/$groupId"
    }

    data object EditSocialGroup : Screen("edit_social_group/{groupId}") {
        fun createRoute(groupId: String) = "edit_social_group/$groupId"
    }

    data object EditChatGroup : Screen("edit_chat_group/{conversationId}") {
        fun createRoute(conversationId: String) = "edit_chat_group/$conversationId"
    }

    // Admin Screens
    data object AdminDashboard : Screen("admin_dashboard")
    data object AdminUserManagement : Screen("admin_user_management")
    data object AdminContentModeration : Screen("admin_content_moderation")
    data object AdminReportManagement : Screen("admin_report_management")
    data object AdminGroupManagement : Screen("admin_group_management")
}
