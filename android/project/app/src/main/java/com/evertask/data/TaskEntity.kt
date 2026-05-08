package com.evertask.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.evertask.data.model.Subtask
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room entity representing a task in the database.
 * Uses JSON serialization for the subtasks list to maintain flexibility.
 */
@Entity(tableName = "tasks")
@Serializable
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "subtasks_json")
    val subtasksJson: String,
    
    @ColumnInfo(name = "template_id")
    val templateId: String? = null,
    
    @ColumnInfo(name = "icon")
    val icon: String,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    
    @ColumnInfo(name = "estimated_minutes")
    val estimatedMinutes: Int = 0
) {
    companion object {
        private val json = Json { 
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        
        /**
         * Creates a TaskEntity from components with automatic subtask serialization.
         */
        fun create(
            id: String,
            title: String,
            subtasks: List<Subtask>,
            templateId: String? = null,
            icon: String = "checkmark.circle",
            estimatedMinutes: Int = 0
        ): TaskEntity {
            return TaskEntity(
                id = id,
                title = title,
                subtasksJson = json.encodeToString(subtasks),
                templateId = templateId,
                icon = icon,
                estimatedMinutes = estimatedMinutes
            )
        }
        
        /**
         * Deserializes the subtasks JSON string into a list of Subtask objects.
         * Returns empty list if deserialization fails (defensive fallback).
         */
        fun deserializeSubtasks(subtasksJson: String): List<Subtask> {
            return runCatching {
                json.decodeFromString<List<Subtask>>(subtasksJson)
            }.getOrDefault(emptyList())
        }
        
        /**
         * Serializes a list of Subtask objects to JSON string.
         */
        fun serializeSubtasks(subtasks: List<Subtask>): String {
            return runCatching {
                json.encodeToString(subtasks)
            }.getOrDefault("[]")
        }
    }
    
    /**
     * Convenience method to get deserialized subtasks.
     */
    fun getSubtasks(): List<Subtask> = deserializeSubtasks(subtasksJson)
    
    /**
     * Returns a copy with updated subtasks (re-serialized).
     */
    fun withSubtasks(subtasks: List<Subtask>): TaskEntity {
        return copy(subtasksJson = serializeSubtasks(subtasks))
    }
    
    /**
     * Returns a copy marked as completed with timestamp.
     */
    fun markCompleted(): TaskEntity {
        return copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Returns a copy marked as archived.
     */
    fun markArchived(): TaskEntity {
        return copy(isArchived = true)
    }
    
    /**
     * Calculates progress percentage based on completed subtasks.
     */
    fun calculateProgress(): Int {
        val subs = getSubtasks()
        if (subs.isEmpty()) return if (isCompleted) 100 else 0
        val completedCount = subs.count { it.isCompleted }
        return (completedCount * 100) / subs.size
    }
    
    /**
     * Calculates total estimated time from subtasks.
     */
    fun calculateTotalMinutes(): Int {
        return getSubtasks().sumOf { it.durationMinutes }
    }
    
    /**
     * Calculates remaining time from incomplete subtasks.
     */
    fun calculateRemainingMinutes(): Int {
        return getSubtasks()
            .filter { !it.isCompleted }
            .sumOf { it.durationMinutes }
    }
}
