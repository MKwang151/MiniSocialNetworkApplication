package com.example.minisocialnetworkapplication.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect modifier for loading states
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value)
        )
    )
}

/**
 * Shimmer placeholder for PostCard loading
 */
@Composable
fun PostCardShimmer(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shimmerEffect()
                        .background(Color.LightGray, CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    // Author name
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                            .shimmerEffect()
                            .background(Color.LightGray, RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Timestamp
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .shimmerEffect()
                            .background(Color.LightGray, RoundedCornerShape(4.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .shimmerEffect()
                    .background(Color.LightGray, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .shimmerEffect()
                    .background(Color.LightGray, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .shimmerEffect()
                    .background(Color.LightGray, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Like button
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .shimmerEffect()
                        .background(Color.LightGray, RoundedCornerShape(4.dp))
                )

                // Comment button
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .shimmerEffect()
                        .background(Color.LightGray, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

/**
 * Shimmer placeholder for Comment loading
 */
@Composable
fun CommentItemShimmer(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .shimmerEffect()
                .background(Color.LightGray, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Author name
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp)
                    .shimmerEffect()
                    .background(Color.LightGray, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Comment text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .shimmerEffect()
                    .background(Color.LightGray, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .shimmerEffect()
                    .background(Color.LightGray, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(10.dp)
                    .shimmerEffect()
                    .background(Color.LightGray, RoundedCornerShape(4.dp))
            )
        }
    }
}

/**
 * Shimmer placeholder for Profile header
 */
@Composable
fun ProfileHeaderShimmer(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .shimmerEffect()
                .background(Color.LightGray, CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(24.dp)
                .shimmerEffect()
                .background(Color.LightGray, RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(16.dp)
                .shimmerEffect()
                .background(Color.LightGray, RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Post count
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(16.dp)
                .shimmerEffect()
                .background(Color.LightGray, RoundedCornerShape(4.dp))
        )
    }
}

/**
 * Loading screen with multiple shimmer cards
 */
@Composable
fun FeedLoadingShimmer(
    modifier: Modifier = Modifier,
    itemCount: Int = 3
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(itemCount) {
            PostCardShimmer()
        }
    }
}

