package com.evertask.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.SpeechRecognizer.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.evertask.app.ui.theme.HeroShapes
import com.evertask.app.ui.theme.components.HeroCard
import com.evertask.app.ui.theme.components.HeroIconButton
import com.evertask.app.ui.theme.components.HeroInput
import com.evertask.app.util.Haptics
import kotlinx.coroutines.delay

private enum class VoiceContentState { IDLE, KEYBOARD, LISTENING, PROCESSING }

@Composable
fun VoiceInputComponent(
    voiceInputState: VoiceInputState,
    currentText: String,
    onTextChange: (String) -> Unit,
    onTextSubmit: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showKeyboard by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var pendingVoiceStart by remember { mutableStateOf(false) }
    var showPermissionMessage by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(SpeechRecognizer.createSpeechRecognizer(context)) }
    var currentRms by remember { mutableStateOf(0f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingVoiceStart) {
            pendingVoiceStart = false
            speechRecognizer?.let { startVoiceRecognition(context, it) }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            val sr = speechRecognizer
            speechRecognizer = null
            sr?.let {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try { it.destroy() } catch (_: Exception) {}
                }, 300)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (voiceInputState is VoiceInputState.Listening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (voiceInputState is VoiceInputState.Listening) 1f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_alpha"
    )

    LaunchedEffect(speechRecognizer) {
        val recognizer = speechRecognizer ?: return@LaunchedEffect
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                try { onStartVoice() } catch (_: Exception) {}
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                try { currentRms = rmsdB } catch (_: Exception) {}
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                try { onStopVoice() } catch (_: Exception) {}
            }

            override fun onError(error: Int) {
                try {
                    val message = when (error) {
                        ERROR_AUDIO -> "Audio recording error"
                        ERROR_CLIENT -> "Client side error"
                        ERROR_INSUFFICIENT_PERMISSIONS -> {
                            showPermissionMessage = true
                            "Insufficient permissions"
                        }
                        ERROR_NETWORK -> "Network error"
                        ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        ERROR_NO_MATCH -> "No speech recognized"
                        ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        ERROR_SERVER -> "Server error"
                        ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    onVoiceError(message)
                    showKeyboard = true
                } catch (_: Exception) {}
            }

            override fun onResults(results: Bundle?) {
                try {
                    val matches = results?.getStringArrayList(RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onVoiceResult(matches[0])
                    } else {
                        onVoiceError("No speech recognized")
                        showKeyboard = true
                    }
                } catch (_: Exception) {}
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    LaunchedEffect(showKeyboard) {
        if (showKeyboard) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (_: IllegalStateException) {
                // FocusRequester was disposed before focus could be requested
            }
        }
    }

    HeroCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = when (voiceInputState) {
                        is VoiceInputState.Listening -> MaterialTheme.colorScheme.primaryContainer
                        is VoiceInputState.Processing -> MaterialTheme.colorScheme.secondaryContainer
                        is VoiceInputState.Error -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val contentState = when {
                showKeyboard -> VoiceContentState.KEYBOARD
                voiceInputState is VoiceInputState.Listening -> VoiceContentState.LISTENING
                voiceInputState is VoiceInputState.Processing -> VoiceContentState.PROCESSING
                else -> VoiceContentState.IDLE
            }
            AnimatedContent(
                targetState = contentState,
                transitionSpec = {
                    fadeIn() + scaleIn(initialScale = 0.98f) togetherWith fadeOut() + scaleOut(targetScale = 0.98f)
                },
                label = "voice_content"
            ) { state ->
                when (state) {
                    VoiceContentState.KEYBOARD -> {
                        KeyboardInputField(
                            text = currentText,
                            onTextChange = onTextChange,
                            onSubmit = {
                                onTextSubmit()
                                showKeyboard = false
                                focusManager.clearFocus()
                            },
                            focusRequester = focusRequester,
                            onSwitchToVoice = {
                                Haptics.performClick(context)
                                showKeyboard = false
                                focusManager.clearFocus()
                                when {
                                    speechRecognizer == null -> {
                                        showKeyboard = true
                                    }
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                                        speechRecognizer?.let { startVoiceRecognition(context, it) }
                                    }
                                    else -> {
                                        pendingVoiceStart = true
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        )
                    }
                    VoiceContentState.LISTENING -> {
                        VoiceListeningView(
                            scale = scale,
                            alpha = alpha,
                            onTap = {
                                Haptics.performClick(context)
                                speechRecognizer?.stopListening()
                                onStopVoice()
                            },
                            rmsdB = currentRms
                        )
                    }
                    VoiceContentState.PROCESSING -> {
                        VoiceProcessingView()
                    }
                    VoiceContentState.IDLE -> {
                        VoiceIdleView(
                            onTap = {
                                Haptics.performClick(context)
                                if (speechRecognizer == null || !SpeechRecognizer.isRecognitionAvailable(context)) {
                                    showKeyboard = true
                                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    speechRecognizer?.let { startVoiceRecognition(context, it) }
                                } else {
                                    pendingVoiceStart = true
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onSwitchToKeyboard = { 
                                Haptics.performClick(context)
                                showKeyboard = true 
                            }
                        )
                    }
                }
            }
            if (showPermissionMessage) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Microphone permission is required for voice input. Please enable it in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun VoiceIdleView(
    onTap: () -> Unit,
    onSwitchToKeyboard: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                )
                .semantics {
                    contentDescription = "Tap to speak voice command"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroIconButton(onClick = onTap) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microphone",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Tap to speak or type your task",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        HeroIconButton(
            onClick = onSwitchToKeyboard,
            modifier = Modifier.semantics {
                contentDescription = "Switch to keyboard input"
            }
        ) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VoiceListeningView(
    scale: Float,
    alpha: Float,
    onTap: () -> Unit,
    rmsdB: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
            .semantics {
                contentDescription = "Listening for voice input, tap to stop"
            }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(scale)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            HeroIconButton(onClick = onTap) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Stop listening",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        VoiceWaveform(rmsdB = rmsdB)
    }
}

@Composable
private fun VoiceWaveform(rmsdB: Float) {
    val barHeights = remember { List(5) { Animatable(4f) } }
    barHeights.forEach { animatable ->
        val target = ((rmsdB + 2f + kotlin.random.Random.nextFloat() * 6f).coerceIn(0f, 18f))
        LaunchedEffect(rmsdB) {
            animatable.animateTo(target, animationSpec = tween(80))
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barHeights.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(anim.value.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

@Composable
private fun VoiceProcessingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Processing...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KeyboardInputField(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
    onSwitchToVoice: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeroInput(
            value = text,
            onValueChange = onTextChange,
            placeholder = "Type your task here",
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = "Type task description"
                },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            )
        )
        
        HeroIconButton(
            onClick = onSwitchToVoice,
            modifier = Modifier.semantics {
                contentDescription = "Switch to voice input"
            }
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun startVoiceRecognition(context: Context, speechRecognizer: SpeechRecognizer) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        return
    }

    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        }

        speechRecognizer.startListening(intent)
    } catch (e: RuntimeException) {
        // Some OEM speech services throw here; fallback handled by caller
    }
}
