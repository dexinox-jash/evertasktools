package com.evertasktools.ui.screens

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.random.Random

// Particle data class for confetti
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Check reduce motion setting
    val reduceMotion = remember { isReduceMotionEnabled(context) }
    
    // Countdown for auto-archive
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
        // Confetti animation (only if reduce motion is off)
        if (!reduceMotion) {
            ConfettiAnimation(
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(60.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Congratulations text
            Text(
                text = "All Done!",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Task title
            Text(
                text = "\"$taskTitle\"",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats
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
            
            // Undo button with countdown
            OutlinedButton(
                onClick = onUndo,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp)
                    .semantics {
                        contentDescription = "Undo completion and continue task"
                    },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                Text("Undo (${countdown}s)")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Archiving to history...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
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
    particleCount: Int = 80
) {
    val context = LocalContext.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val colors = listOf(
        primaryColor,
        primaryColor.copy(alpha = 0.8f),
        Color.White,
        Color.White.copy(alpha = 0.8f),
        primaryColor.copy(alpha = 0.6f)
    )

    var particles by remember {
        mutableStateOf<List<Particle>>(emptyList())
    }

    var isRunning by remember { mutableStateOf(true) }

    // Initialize particles
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

    // Fixed-duration animation loop (max 3 seconds)
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
    // Draw rectangular confetti piece
    val halfSize = particle.size / 2
    
    // Calculate corners based on rotation
    val radians = Math.toRadians(particle.rotation.toDouble())
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()
    
    val topLeft = Offset(
        particle.x + (-halfSize * cos - -halfSize * sin),
        particle.y + (-halfSize * sin + -halfSize * cos)
    )
    val topRight = Offset(
        particle.x + (halfSize * cos - -halfSize * sin),
        particle.y + (halfSize * sin + -halfSize * cos)
    )
    val bottomRight = Offset(
        particle.x + (halfSize * cos - halfSize * sin),
        particle.y + (halfSize * sin + halfSize * cos)
    )
    val bottomLeft = Offset(
        particle.x + (-halfSize * cos - halfSize * sin),
        particle.y + (-halfSize * sin + halfSize * cos)
    )
    
    // Draw the particle as a small rectangle
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
