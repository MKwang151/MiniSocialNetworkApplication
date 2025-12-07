package com.example.minisocialnetworkapplication.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Feed : Screen("feed")
    data object ComposePost : Screen("compose_post")
    data object Settings : Screen("settings")
    data object Chat : Screen("chat")
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
}

