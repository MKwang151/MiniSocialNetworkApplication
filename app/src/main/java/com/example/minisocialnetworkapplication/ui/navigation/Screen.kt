package com.example.minisocialnetworkapplication.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Feed : Screen("feed")
    data object ComposePost : Screen("compose_post?groupId={groupId}") {
        fun createRoute(groupId: String? = null) = if (groupId != null) "compose_post?groupId=$groupId" else "compose_post"
    }
    data object Settings : Screen("settings")
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
}
