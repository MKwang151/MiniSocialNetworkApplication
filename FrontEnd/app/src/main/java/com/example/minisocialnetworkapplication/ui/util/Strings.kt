package com.example.minisocialnetworkapplication.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.minisocialnetworkapplication.R

/**
 * Helper object for accessing string resources in Composables
 */
object Strings {
    // Authentication
    val login @Composable get() = stringResource(R.string.login)
    val register @Composable get() = stringResource(R.string.register)
    val logout @Composable get() = stringResource(R.string.logout)
    val email @Composable get() = stringResource(R.string.email)
    val password @Composable get() = stringResource(R.string.password)
    val confirmPassword @Composable get() = stringResource(R.string.confirm_password)
    val displayName @Composable get() = stringResource(R.string.display_name)
    val createAccount @Composable get() = stringResource(R.string.create_account)
    val alreadyHaveAccount @Composable get() = stringResource(R.string.already_have_account)
    val dontHaveAccount @Composable get() = stringResource(R.string.dont_have_account)

    // Feed
    val feed @Composable get() = stringResource(R.string.feed)
    val createPost @Composable get() = stringResource(R.string.create_post)
    val noPostsYet @Composable get() = stringResource(R.string.no_posts_yet)
    val beFirstToShare @Composable get() = stringResource(R.string.be_first_to_share)
    val failedToLoadFeed @Composable get() = stringResource(R.string.failed_to_load_feed)
    val retry @Composable get() = stringResource(R.string.retry)

    // Post Creation
    val whatsOnYourMind @Composable get() = stringResource(R.string.whats_on_your_mind)
    val post @Composable get() = stringResource(R.string.post)
    val selectedImages @Composable get() = stringResource(R.string.selected_images)
    val uploadingPost @Composable get() = stringResource(R.string.uploading_post)
    val back @Composable get() = stringResource(R.string.back)

    // Post Interactions
    val like @Composable get() = stringResource(R.string.like)
    val comment @Composable get() = stringResource(R.string.comment)
    val moreOptions @Composable get() = stringResource(R.string.more_options)

    // General
    val loading @Composable get() = stringResource(R.string.loading)

    // Functions with parameters
    @Composable
    fun addPhotos(max: Int) = stringResource(R.string.add_photos, max)

    @Composable
    fun addMorePhotos(current: Int, max: Int) = stringResource(R.string.add_more_photos, current, max)

    @Composable
    fun likesCount(count: Int) = stringResource(R.string.likes_count, count)

    @Composable
    fun commentsCount(count: Int) = stringResource(R.string.comments_count, count)
}

