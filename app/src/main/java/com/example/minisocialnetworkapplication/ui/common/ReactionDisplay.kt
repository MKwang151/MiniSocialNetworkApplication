package com.example.minisocialnetworkapplication.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Display reactions below message bubble or comment
 * UI-only upgrade: Instagram-like pill chips + animations
 */
@Composable
fun ReactionDisplay(
    reactions: Map<String, List<String>>,
    currentUserId: String?,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    Row(
        modifier = modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        reactions.forEach { (emoji, userIds) ->
            val hasMyReaction = currentUserId != null && userIds.contains(currentUserId)
            val shape = RoundedCornerShape(999.dp)


            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()


            val containerColor by animateColorAsState(
                targetValue = if (hasMyReaction)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                animationSpec = tween(180),
                label = "reactionContainer"
            )

            val borderColor by animateColorAsState(
                targetValue = if (hasMyReaction)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                animationSpec = tween(180),
                label = "reactionBorder"
            )

            val elevation by animateDpAsState(
                targetValue = if (hasMyReaction) 6.dp else 1.dp,
                animationSpec = spring(stiffness = 450f, dampingRatio = 0.9f),
                label = "reactionElevation"
            )

            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.96f else 1f,
                animationSpec = spring(stiffness = 650f, dampingRatio = 0.75f),
                label = "reactionScale"
            )

            Surface(
                color = containerColor,
                shape = shape,
                tonalElevation = if (hasMyReaction) 2.dp else 0.dp,
                shadowElevation = elevation,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(shape)
                    .border(1.dp, borderColor, shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onReactionClick(emoji) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.bodyMedium
                    )


                    if (userIds.size > 0) {
                        val countBg by animateColorAsState(
                            targetValue = if (hasMyReaction)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            animationSpec = tween(180),
                            label = "countBg"
                        )

                        val countColor by animateColorAsState(
                            targetValue = if (hasMyReaction)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "countColor"
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(countBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userIds.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = countColor
                            )
                        }
                    }
                }
            }
        }
    }
}
