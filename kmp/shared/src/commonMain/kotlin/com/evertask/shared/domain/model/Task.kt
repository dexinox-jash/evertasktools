package com.evertask.shared.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String,
    val title: String,
    val subtasks: List<Subtask> = emptyList(),
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val sourceTemplateId: String? = null,
    val iconName: String = "default_task",
    val sortOrder: Int = 0
) {
    fun calculateProgress(): Int {
        if (subtasks.isEmpty()) return if (isCompleted) 100 else 0
        val completed = subtasks.count { it.isCompleted }
        return (completed * 100 / subtasks.size)
    }
}
