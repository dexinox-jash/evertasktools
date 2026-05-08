package com.evertask.shared.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Subtask(
    val id: String,
    val taskId: String,
    val text: String,
    val isCompleted: Boolean = false,
    val durationMinutes: Int = 0,
    val sortOrder: Int = 0,
    val completedAt: Instant? = null
)
