package com.evertask.data.repository

import android.content.Context
import android.database.sqlite.SQLiteFullException
import com.evertask.data.dao.TaskDao
import com.evertask.data.database.TaskDatabase
import com.evertask.data.engine.MatchedTemplate
import com.evertask.data.engine.TemplateEngine
import com.evertask.data.entity.TaskEntity
import com.evertask.data.exception.StorageFullException
import com.evertask.data.model.Subtask
import com.evertask.data.security.SecureBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository pattern implementation for Task operations.
 *
 * Responsibilities:
 * - Mediates between data sources (database, templates)
 * - Provides clean API for ViewModels
 * - Handles all error boundaries with runCatching
 * - Ensures all I/O on Dispatchers.IO
 *
 * This is a singleton - use getInstance() to obtain reference.
 */
class TaskRepository private constructor(
    private val context: Context,
    private val taskDao: TaskDao,
    private val templateEngine: TemplateEngine
) {
    companion object {
        @Volatile
        private var INSTANCE: TaskRepository? = null

        /**
         * Gets singleton repository instance.
         * Initializes TemplateEngine on first call (suspend).
         */
        suspend fun getInstance(context: Context): TaskRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: runBlocking(Dispatchers.IO) { createInstance(context) }.also { INSTANCE = it }
            }
        }

        private suspend fun createInstance(context: Context): TaskRepository {
            val database = TaskDatabase.getInstance(context)
            val templateEngine = TemplateEngine.create(context)
            return TaskRepository(
                context = context.applicationContext,
                taskDao = database.taskDao(),
                templateEngine = templateEngine
            )
        }

        /**
         * Destroys instance. Use for testing only.
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }

    /**
     * Triggers an async secure backup after every successful mutation.
     */
    private fun triggerBackupAsync() {
        GlobalScope.launch(Dispatchers.IO) {
            SecureBackupManager.getInstance(context).exportSecureBackup()
        }
    }

    /**
     * Maps a SQLiteFullException to a user-friendly StorageFullException.
     */
    private fun <T> Result<T>.mapStorageFull(): Result<T> {
        val error = exceptionOrNull()
        return if (error is SQLiteFullException) {
            Result.failure(StorageFullException("Device storage is full. Unable to save data."))
        } else {
            this
        }
    }

    // ==================== QUERY OPERATIONS ====================

    /**
     * Returns all active (non-archived) tasks as Flow.
     * UI automatically updates when data changes.
     */
    fun getActiveTasks(): Flow<List<TaskEntity>> {
        return taskDao.getAllActive()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Returns all tasks (active + completed/archived).
     */
    fun getAllTasks(): Flow<List<TaskEntity>> {
        return taskDao.getAll()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Returns completed but not archived tasks.
     * Useful for "archive all" functionality.
     */
    fun getCompletedTasks(): Flow<List<TaskEntity>> {
        return taskDao.getAllCompleted()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Returns a single task by ID.
     * Null if not found.
     */
    suspend fun getTaskById(taskId: String): TaskEntity? = withContext(Dispatchers.IO) {
        runCatching {
            taskDao.getById(taskId)
        }.getOrNull()
    }

    /**
     * Returns task count statistics.
     */
    suspend fun getTaskStats(): TaskStats = withContext(Dispatchers.IO) {
        runCatching {
            TaskStats(
                activeCount = taskDao.getActiveCount(),
                completedCount = taskDao.getCompletedCount()
            )
        }.getOrDefault(TaskStats(0, 0))
    }

    /**
     * Searches tasks by title query.
     */
    fun searchTasks(query: String): Flow<List<TaskEntity>> {
        return taskDao.searchByTitle(query)
            .flowOn(Dispatchers.IO)
    }

    // ==================== CREATE OPERATIONS ====================

    /**
     * Creates a new task from a title using template matching.
     * This is the primary entry point for task creation.
     *
     * Process:
     * 1. Match title against templates
     * 2. Create subtasks from template
     * 3. Persist to database
     * 4. Return created task
     */
    suspend fun createTask(title: String): Result<TaskEntity> = withContext(Dispatchers.IO) {
        runCatching {
            // Match against templates
            val matchedTemplate = templateEngine.matchTask(title)

            // Create task entity
            val task = TaskEntity.create(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                subtasks = matchedTemplate.subtasks,
                templateId = matchedTemplate.templateId,
                icon = matchedTemplate.icon,
                estimatedMinutes = matchedTemplate.estimatedMinutes
            )

            // Persist and return
            taskDao.insert(task)
            triggerBackupAsync()
            task
        }.mapStorageFull()
    }

    /**
     * Creates a task with a specific template (bypasses auto-matching).
     * Useful for "quick add" from template gallery.
     */
    suspend fun createTaskWithTemplate(title: String, templateId: String): Result<TaskEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val template = templateEngine.getTemplateById(templateId)
                    ?: throw IllegalArgumentException("Template not found: $templateId")

                val subtasks = template.subtasks.mapIndexed { index, templateString ->
                    Subtask.fromTemplateString(templateString, index)
                }

                val baseMinutes = subtasks.sumOf { it.durationMinutes }
                val estimatedMinutes = (baseMinutes * 1.20).toInt()

                val task = TaskEntity.create(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    subtasks = subtasks,
                    templateId = templateId,
                    icon = template.icon,
                    estimatedMinutes = estimatedMinutes
                )

                taskDao.insert(task)
                triggerBackupAsync()
                task
            }.mapStorageFull()
        }

    // ==================== UPDATE OPERATIONS ====================

    /**
     * Toggles completion status of a specific subtask.
     * Automatically marks parent task complete if all subtasks done.
     *
     * Returns updated task or null if operation failed.
     */
    suspend fun completeSubtask(taskId: String, subtaskId: String): Result<TaskEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Get current task
                val task = taskDao.getById(taskId)
                    ?: throw IllegalArgumentException("Task not found: $taskId")

                // Get and update subtasks
                val subtasks = task.getSubtasks().toMutableList()
                val subtaskIndex = subtasks.indexOfFirst { it.id == subtaskId }

                if (subtaskIndex == -1) {
                    throw IllegalArgumentException("Subtask not found: $subtaskId")
                }

                // Toggle the subtask
                val currentSubtask = subtasks[subtaskIndex]
                subtasks[subtaskIndex] = currentSubtask.copy(
                    isCompleted = !currentSubtask.isCompleted
                )

                // Check if all subtasks are now complete
                val allCompleted = subtasks.all { it.isCompleted }

                // Update in database
                val updatedTask = task.withSubtasks(subtasks)
                taskDao.updateSubtasksAndCheckCompletion(
                    taskId = taskId,
                    subtasksJson = TaskEntity.serializeSubtasks(subtasks),
                    allCompleted = allCompleted
                )

                // Return updated task
                val result = taskDao.getById(taskId) ?: updatedTask
                triggerBackupAsync()
                result
            }.mapStorageFull()
        }

    /**
     * Marks a subtask as skipped.
     */
    suspend fun skipSubtask(taskId: String, subtaskId: String): Result<TaskEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val task = taskDao.getById(taskId)
                    ?: throw IllegalArgumentException("Task not found: $taskId")

                val subtasks = task.getSubtasks().toMutableList()
                val subtaskIndex = subtasks.indexOfFirst { it.id == subtaskId }

                if (subtaskIndex == -1) {
                    throw IllegalArgumentException("Subtask not found: $subtaskId")
                }

                subtasks[subtaskIndex] = subtasks[subtaskIndex].copy(
                    isSkipped = true,
                    skippedAt = System.currentTimeMillis()
                )

                val updatedTask = task.withSubtasks(subtasks)
                taskDao.updateSubtasks(taskId, TaskEntity.serializeSubtasks(subtasks))
                val result = taskDao.getById(taskId) ?: updatedTask
                triggerBackupAsync()
                result
            }.mapStorageFull()
        }

    /**
     * Marks a subtask as not skipped (undo skip).
     */
    suspend fun unskipSubtask(taskId: String, subtaskId: String): Result<TaskEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val task = taskDao.getById(taskId)
                    ?: throw IllegalArgumentException("Task not found: $taskId")

                val subtasks = task.getSubtasks().toMutableList()
                val subtaskIndex = subtasks.indexOfFirst { it.id == subtaskId }

                if (subtaskIndex == -1) {
                    throw IllegalArgumentException("Subtask not found: $subtaskId")
                }

                subtasks[subtaskIndex] = subtasks[subtaskIndex].copy(
                    isSkipped = false,
                    skippedAt = null
                )

                val updatedTask = task.withSubtasks(subtasks)
                taskDao.updateSubtasks(taskId, TaskEntity.serializeSubtasks(subtasks))
                val result = taskDao.getById(taskId) ?: updatedTask
                triggerBackupAsync()
                result
            }.mapStorageFull()
        }

    /**
     * Marks a task as completed (manual override).
     * Does not affect subtask completion status.
     */
    suspend fun completeTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val updated = taskDao.updateCompletionStatus(
                taskId = taskId,
                isCompleted = true,
                completedAt = System.currentTimeMillis()
            )
            if (updated == 0) {
                throw IllegalArgumentException("Task not found: $taskId")
            }
            triggerBackupAsync()
        }.mapStorageFull()
    }

    /**
     * Updates task title.
     */
    suspend fun updateTaskTitle(taskId: String, newTitle: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val task = taskDao.getById(taskId)
                    ?: throw IllegalArgumentException("Task not found: $taskId")
                taskDao.update(task.copy(title = newTitle.trim()))
                triggerBackupAsync()
                Unit
            }.mapStorageFull()
        }

    // ==================== DELETE/ARCHIVE OPERATIONS ====================

    /**
     * Archives all completed tasks.
     * Returns count of archived tasks.
     */
    suspend fun archiveCompleted(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val count = taskDao.archiveAllCompleted()
            triggerBackupAsync()
            count
        }.mapStorageFull()
    }

    /**
     * Archives a specific task by ID.
     */
    suspend fun archiveTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val updated = taskDao.archiveById(taskId)
            if (updated == 0) {
                throw IllegalArgumentException("Task not found: $taskId")
            }
            triggerBackupAsync()
        }.mapStorageFull()
    }

    /**
     * Convenience wrapper that permanently deletes a task.
     * Prefer archiveTask for user-facing delete.
     */
    suspend fun deleteTask(taskId: String): Result<Unit> = deleteTaskPermanently(taskId)

    /**
     * Permanently deletes a task.
     * Prefer archiveTask for user-facing delete.
     */
    suspend fun deleteTaskPermanently(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val deleted = taskDao.deleteById(taskId)
            if (deleted == 0) {
                throw IllegalArgumentException("Task not found: $taskId")
            }
            triggerBackupAsync()
        }.mapStorageFull()
    }

    /**
     * Permanently deletes all archived tasks.
     * Use for cleanup operations.
     */
    suspend fun cleanupArchivedTasks(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val count = taskDao.deleteAllArchived()
            triggerBackupAsync()
            count
        }.mapStorageFull()
    }

    // ==================== UTILITY OPERATIONS ====================

    /**
     * Marks a task as not completed (undo completion).
     */
    suspend fun uncompleteTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val updated = taskDao.updateCompletionStatus(
                taskId = taskId,
                isCompleted = false,
                completedAt = null
            )
            if (updated == 0) {
                throw IllegalArgumentException("Task not found: $taskId")
            }
            triggerBackupAsync()
        }.mapStorageFull()
    }

    /**
     * Gets all available template IDs for template gallery.
     */
    fun getAvailableTemplates(): List<String> {
        return templateEngine.getAllTemplateIds()
    }

    /**
     * Previews what template would match a given title.
     * Does not create a task.
     */
    fun previewTemplateMatch(title: String): MatchedTemplate {
        return templateEngine.matchTask(title)
    }

    /**
     * Exports all active tasks as JSON for backup.
     */
    suspend fun exportTasksForBackup(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // This would be implemented with kotlinx.serialization
            // For now, return placeholder
            "{\"export\": \"not implemented\"}"
        }
    }
}

/**
 * Simple data class for task statistics.
 */
data class TaskStats(
    val activeCount: Int,
    val completedCount: Int
) {
    val totalCount: Int get() = activeCount + completedCount
    val completionRate: Float get() = if (totalCount > 0) {
        completedCount.toFloat() / totalCount
    } else 0f
}
