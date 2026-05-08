package com.evertask.shared.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Shared Task model used across all platforms
 * This is the single source of truth for task data
 */
@Serializable
data class SharedTask(
    val id: String,
    val title: String,
    val subtasks: List<SharedSubtask>,
    val isCompleted: Boolean,
    val isArchived: Boolean,
    val createdAt: Instant,
    val completedAt: Instant?,
    val sourceTemplateId: String?
) {
    val totalEstimatedMinutes: Int
        get() = (subtasks.sumOf { it.durationMinutes } * 1.2).toInt()
    
    val completedSubtaskCount: Int
        get() = subtasks.count { it.isCompleted }
    
    val progressPercentage: Double
        get() = if (subtasks.isEmpty()) 0.0 else completedSubtaskCount.toDouble() / subtasks.size
}

/**
 * Shared Subtask model
 */
@Serializable
data class SharedSubtask(
    val id: String,
    val text: String,
    val isCompleted: Boolean,
    val durationMinutes: Int
)

/**
 * Task filter options
 */
enum class TaskFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    ARCHIVED
}
