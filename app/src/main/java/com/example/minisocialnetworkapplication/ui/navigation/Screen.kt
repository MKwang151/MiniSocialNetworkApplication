package com.example.minisocialnetworkapplication.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Feed : Screen("feed")
    data object ComposePost : Screen("compose_post")
    data object Settings : Screen("settings")
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
}
