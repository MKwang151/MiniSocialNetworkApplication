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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact reaction display - small pills below messages
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
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        reactions.forEach { (emoji, userIds) ->
            val hasMyReaction = currentUserId != null && userIds.contains(currentUserId)
            val shape = RoundedCornerShape(999.dp)

            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()

            val containerColor by animateColorAsState(
                targetValue = if (hasMyReaction)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                animationSpec = tween(220),
                label = "reactionContainer"
            )

            val borderColor by animateColorAsState(
                targetValue = if (hasMyReaction)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                animationSpec = tween(220),
                label = "reactionBorder"
            )

            val elevation by animateDpAsState(
                targetValue = if (hasMyReaction) 2.dp else 0.dp,
                animationSpec = spring(stiffness = 500f, dampingRatio = 0.85f),
                label = "reactionElevation"
            )

            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.92f else 1f,
                animationSpec = spring(stiffness = 700f, dampingRatio = 0.7f),
                label = "reactionScale"
            )

            Surface(
                color = containerColor,
                shape = shape,
                tonalElevation = if (hasMyReaction) 1.dp else 0.dp,
                shadowElevation = elevation,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(shape)
                    .border(
                        width = if (hasMyReaction) 1.dp else 0.5.dp,
                        color = borderColor,
                        shape = shape
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onReactionClick(emoji) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Emoji - smaller
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp
                    )

                    // Count - only show if > 1
                    if (userIds.size > 1) {
                        Text(
                            text = userIds.size.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (hasMyReaction) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 10.sp
                            ),
                            color = if (hasMyReaction)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
