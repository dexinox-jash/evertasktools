package com.evertasktools.ui.components

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.evertask.data.model.Subtask


@Composable
fun SubtaskItem(
    subtask: Subtask,
    isActive: Boolean,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onUndoComplete: () -> Unit,
    onUndoSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Animation values
    val alpha by animateFloatAsState(
        targetValue = if (subtask.isCompleted || subtask.isSkipped) 0.6f else 1f,
        label = "alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            subtask.isCompleted -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            subtask.isSkipped -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        label = "background"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
            .semantics {
                contentDescription = buildContentDescription(subtask)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Checkbox and text
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                SubtaskCheckbox(
                    isCompleted = subtask.isCompleted,
                    isSkipped = subtask.isSkipped,
                    onToggle = {
                        if (subtask.isCompleted) {
                            onUndoComplete()
                        } else if (!subtask.isSkipped) {
                            triggerHapticFeedback(context)
                            onComplete()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Task text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subtask.text,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (subtask.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                        color = when {
                            subtask.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            subtask.isSkipped -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    // Duration badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when {
                            subtask.isCompleted -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            subtask.isSkipped -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "${subtask.durationMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                subtask.isCompleted -> MaterialTheme.colorScheme.tertiary
                                subtask.isSkipped -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // Skip button (only show if not completed or skipped)
            if (!subtask.isCompleted && !subtask.isSkipped) {
                TextButton(
                    onClick = {
                        triggerHapticFeedback(context, isSkip = true)
                        onSkip()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Skip ${subtask.text}"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skip")
                }
            } else if (subtask.isSkipped) {
                // Undo skip button
                TextButton(
                    onClick = onUndoSkip,
                    modifier = Modifier.semantics {
                        contentDescription = "Undo skip for ${subtask.text}"
                    }
                ) {
                    Text("Undo")
                }
            }
        }
    }
}

@Composable
private fun SubtaskCheckbox(
    isCompleted: Boolean,
    isSkipped: Boolean,
    onToggle: () -> Unit
) {
    val icon = when {
        isCompleted -> Icons.Default.CheckCircle
        isSkipped -> Icons.Default.CheckCircle
        else -> Icons.Default.RadioButtonUnchecked
    }
    
    val iconColor = when {
        isCompleted -> MaterialTheme.colorScheme.tertiary
        isSkipped -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.primary
    }
    
    IconButton(
        onClick = onToggle,
        modifier = Modifier.semantics {
            contentDescription = if (isCompleted) {
                "Mark as incomplete"
            } else if (isSkipped) {
                "Skipped, tap to undo"
            } else {
                "Mark as complete"
            }
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun buildContentDescription(subtask: Subtask): String {
    return buildString {
        append(subtask.text)
        append(", ${subtask.durationMinutes} minutes")
        when {
            subtask.isCompleted -> append(", completed")
            subtask.isSkipped -> append(", skipped")
        }
    }
}

private fun triggerHapticFeedback(context: Context, isSkip: Boolean = false) {
    // Check if reduce motion is enabled
    val transitionScale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        1.0f
    )
    
    if (transitionScale == 0f) {
        // Reduce motion enabled, skip haptic
        return
    }
    
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.let {
        if (it.hasVibrator()) {
            val effect = if (isSkip) {
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                VibrationEffect.createOneShot(50, VibrationEffect.EFFECT_CLICK)
            }
            it.vibrate(effect)
        }
    }
}
