package com.evertask.app.ui

sealed class VoiceInputState {
    data object Idle : VoiceInputState()
    data object Listening : VoiceInputState()
    data object Processing : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}
