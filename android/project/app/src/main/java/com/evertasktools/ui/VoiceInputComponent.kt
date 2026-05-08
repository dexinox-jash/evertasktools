package com.evertasktools.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.SpeechRecognizer.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import com.evertasktools.R
import com.evertasktools.ui.VoiceInputState
import kotlinx.coroutines.delay

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
    
    // Speech recognizer setup
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    // Animation for pulsing mic
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

    // Setup recognition listener
    LaunchedEffect(speechRecognizer) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onStartVoice()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                onStopVoice()
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    ERROR_AUDIO -> "Audio recording error"
                    ERROR_CLIENT -> "Client side error"
                    ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    ERROR_NETWORK -> "Network error"
                    ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    ERROR_NO_MATCH -> "No speech recognized"
                    ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    ERROR_SERVER -> "Server error"
                    ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                onVoiceError(message)
                showKeyboard = true // Auto-switch to keyboard on error
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onVoiceResult(matches[0])
                } else {
                    onVoiceError("No speech recognized")
                    showKeyboard = true
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // Auto-focus keyboard when shown
    LaunchedEffect(showKeyboard) {
        if (showKeyboard) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (voiceInputState) {
                is VoiceInputState.Listening -> MaterialTheme.colorScheme.primaryContainer
                is VoiceInputState.Processing -> MaterialTheme.colorScheme.secondaryContainer
                is VoiceInputState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                showKeyboard -> {
                    // Keyboard input mode
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
                            showKeyboard = false
                            focusManager.clearFocus()
                            startVoiceRecognition(context, speechRecognizer)
                        }
                    )
                }
                voiceInputState is VoiceInputState.Listening -> {
                    // Voice listening mode
                    VoiceListeningView(
                        scale = scale,
                        alpha = alpha,
                        onTap = {
                            speechRecognizer.stopListening()
                            onStopVoice()
                        }
                    )
                }
                voiceInputState is VoiceInputState.Processing -> {
                    // Processing indicator
                    VoiceProcessingView()
                }
                else -> {
                    // Idle mode - tap to speak
                    VoiceIdleView(
                        onTap = {
                            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                                startVoiceRecognition(context, speechRecognizer)
                            } else {
                                showKeyboard = true
                            }
                        },
                        onSwitchToKeyboard = { showKeyboard = true }
                    )
                }
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
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Tap to speak or type your task",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(
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
    onTap: () -> Unit
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
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
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
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = "Type task description"
                },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = "Type your task here",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        IconButton(
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
}
