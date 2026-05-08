package com.evertask.app.ui.screens

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import com.evertask.app.util.Haptics
import kotlin.random.Random
import com.evertask.app.ui.theme.components.HeroButton
import com.evertask.app.ui.theme.components.HeroButtonVariant
import com.evertask.app.ui.theme.components.HeroCard

private data class Particle(
    val id: Int,
    var x: Float,
    var y: Float,
    val color: Color,
    val size: Float,
    val velocityX: Float,
    val velocityY: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val gravity: Float = 0.5f,
    val drag: Float = 0.98f
)

@Composable
fun TaskCompletionScreen(
    taskTitle: String,
    totalMinutes: Int,
    completedCount: Int,
    skippedCount: Int,
    onUndo: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val reduceMotion = remember { isReduceMotionEnabled(context) }
    
    var countdown by remember { mutableIntStateOf(3) }
    
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics {
                contentDescription = "Task completed successfully. Auto-archiving in $countdown seconds"
            }
    ) {
        if (!reduceMotion) {
            ConfettiAnimation(
                modifier = Modifier.fillMaxSize()
            )
        }
        
        HeroCard(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Task completed successfully",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "All Done!",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "\"$taskTitle\"",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StatItem(
                        value = "$totalMinutes",
                        label = "minutes",
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatItem(
                        value = "$completedCount",
                        label = "completed",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (skippedCount > 0) {
                        StatItem(
                            value = "$skippedCount",
                            label = "skipped",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                HeroButton(
                    text = "Undo (${countdown}s)",
                    onClick = { Haptics.performClick(context); onUndo() },
                    variant = HeroButtonVariant.SECONDARY,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .semantics {
                            contentDescription = "Undo completion and continue task"
                        }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HeroButton(
                    text = "Done",
                    onClick = { Haptics.performClick(context); onDone() },
                    variant = HeroButtonVariant.PRIMARY,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .semantics {
                            contentDescription = "Archive task to history"
                        }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Archiving to history...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ConfettiAnimation(
    modifier: Modifier = Modifier,
    particleCount: Int = 40
) {
    val context = LocalContext.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val colors = listOf(
        primaryColor,
        primaryColor.copy(alpha = 0.8f),
        primaryColor.copy(alpha = 0.6f),
        primaryColor.copy(alpha = 0.4f),
        primaryColor.copy(alpha = 0.2f)
    )

    var particles by remember {
        mutableStateOf<List<Particle>>(emptyList())
    }

    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()

        particles = List(particleCount) { index ->
            val angle = Random.nextFloat() * 360f
            val velocity = Random.nextFloat() * 15f + 10f
            Particle(
                id = index,
                x = screenWidth / 2,
                y = screenHeight / 3,
                color = colors.random(),
                size = Random.nextFloat() * 8f + 4f,
                velocityX = cos(Math.toRadians(angle.toDouble())).toFloat() * velocity,
                velocityY = sin(Math.toRadians(angle.toDouble())).toFloat() * velocity - 10f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 10f - 5f
            )
        }
    }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val maxDuration = 3000L
        val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()

        while (isRunning) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= maxDuration) {
                isRunning = false
                break
            }

            val updated = particles.map { p ->
                p.copy(
                    x = p.x + p.velocityX,
                    y = p.y + p.velocityY,
                    velocityY = p.velocityY + p.gravity,
                    velocityX = p.velocityX * p.drag,
                    rotation = p.rotation + p.rotationSpeed
                )
            }

            if (updated.all { it.y > screenHeight + 50f }) {
                isRunning = false
            } else {
                particles = updated
                delay(16)
            }
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            drawParticle(particle)
        }
    }
}

private fun DrawScope.drawParticle(particle: Particle) {
    val halfSize = particle.size / 2
    
    drawRect(
        color = particle.color,
        topLeft = Offset(particle.x - halfSize, particle.y - halfSize / 2),
        size = androidx.compose.ui.geometry.Size(particle.size, particle.size / 2)
    )
}

private fun isReduceMotionEnabled(context: Context): Boolean {
    return Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        1.0f
    ) == 0f
}
