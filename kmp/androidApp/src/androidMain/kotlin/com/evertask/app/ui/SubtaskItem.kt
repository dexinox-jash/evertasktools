package com.evertask.app.ui

import com.evertask.app.util.Haptics
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.evertask.app.ui.theme.HeroShapes
import com.evertask.app.ui.theme.components.HeroCard
import com.evertask.app.ui.theme.components.HeroCheckbox
import com.evertask.app.ui.theme.components.HeroChip
import com.evertask.shared.domain.model.Subtask

@Composable
fun SubtaskItem(
    subtask: Subtask,
    isActive: Boolean,
    onComplete: () -> Unit,
    onUndoComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val alpha by animateFloatAsState(
        targetValue = if (subtask.isCompleted) 0.6f else 1f,
        label = "alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            subtask.isCompleted -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        label = "background"
    )

    HeroCard(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
            .semantics {
                contentDescription = buildContentDescription(subtask)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape = HeroShapes.medium)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroCheckbox(
                        checked = subtask.isCompleted,
                        onCheckedChange = {
                            if (subtask.isCompleted) {
                                Haptics.performClick(context)
                                onUndoComplete()
                            } else {
                                Haptics.performSuccess(context)
                                onComplete()
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subtask.text,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (subtask.isCompleted) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            },
                            color = if (subtask.isCompleted) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        HeroChip(
                            label = { Text("${subtask.durationMinutes} min") },
                            onClick = { }
                        )
                    }
                }
            }
        }
    }
}

private fun buildContentDescription(subtask: Subtask): String {
    return buildString {
        append(subtask.text)
        append(", ${subtask.durationMinutes} minutes")
        if (subtask.isCompleted) {
            append(", completed")
        }
    }
}
