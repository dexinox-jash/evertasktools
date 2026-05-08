package com.evertask.shared.data.repository

import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteFullException
import com.evertask.database.EverTaskDatabase
import com.evertask.shared.domain.exception.StorageFullException
import com.evertask.shared.domain.model.Subtask
import com.evertask.shared.domain.model.Task
import com.evertask.shared.domain.model.TaskTemplate
import com.evertask.shared.domain.model.TemplateSubtask
import kotlinx.coroutines.delay
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.evertask.database.Task as DbTask
import com.evertask.database.Subtask as DbSubtask
import com.evertask.database.Task_template as DbTaskTemplate

class SqlDelightTaskDataSource(
    private val database: EverTaskDatabase
) : TaskDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getActiveTasks(): Flow<List<Task>> =
        database.taskQueries.getActiveTasks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map { task -> task.toDomain() } }

    override fun getArchivedTasks(): Flow<List<Task>> =
        database.taskQueries.getArchivedTasks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map { task -> task.toDomain() } }

    override fun getAllTasks(): Flow<List<Task>> =
        database.taskQueries.getAllTasks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map { task -> task.toDomain() } }

    override fun searchTasks(query: String): Flow<List<Task>> =
        database.taskQueries.searchTasks(query)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map { task -> task.toDomain() } }

    override suspend fun getTaskById(id: String): Task? {
        val dbTask = database.taskQueries.getTaskById(id).executeAsOneOrNull() ?: return null
        return dbTask.toDomain()
    }

    override suspend fun insertTask(task: Task) {
        withDbRetry {
            database.transaction {
                database.taskQueries.insertTask(
                    id = task.id,
                    title = task.title,
                    source_template_id = task.sourceTemplateId,
                    icon_name = task.iconName,
                    is_completed = if (task.isCompleted) 1L else 0L,
                    is_archived = if (task.isArchived) 1L else 0L,
                    created_at = task.createdAt,
                    completed_at = task.completedAt,
                    sort_order = task.sortOrder.toLong()
                )
                task.subtasks.forEach { subtask ->
                    database.taskQueries.insertSubtask(
                        id = subtask.id,
                        task_id = subtask.taskId,
                        text = subtask.text,
                        duration_minutes = subtask.durationMinutes.toLong(),
                        is_completed = if (subtask.isCompleted) 1L else 0L,
                        completed_at = subtask.completedAt,
                        sort_order = subtask.sortOrder.toLong()
                    )
                }
            }
        }
    }

    override suspend fun updateTask(task: Task) {
        withDbRetry {
            database.transaction {
                database.taskQueries.updateTask(
                    title = task.title,
                    source_template_id = task.sourceTemplateId,
                    icon_name = task.iconName,
                    is_completed = if (task.isCompleted) 1L else 0L,
                    is_archived = if (task.isArchived) 1L else 0L,
                    completed_at = task.completedAt,
                    id = task.id
                )
                database.taskQueries.deleteSubtasksByTaskId(task.id)
                task.subtasks.forEachIndexed { index, subtask ->
                    database.taskQueries.insertSubtask(
                        id = subtask.id,
                        task_id = subtask.taskId,
                        text = subtask.text,
                        duration_minutes = subtask.durationMinutes.toLong(),
                        is_completed = if (subtask.isCompleted) 1L else 0L,
                        completed_at = subtask.completedAt,
                        sort_order = index.toLong()
                    )
                }
            }
        }
    }

    override suspend fun deleteTask(id: String) {
        database.taskQueries.deleteTask(id)
    }

    override suspend fun getTemplates(): List<TaskTemplate> {
        return database.taskQueries.getAllTemplates().executeAsList().map { it.toDomain() }
    }

    override suspend fun insertTemplate(template: TaskTemplate) {
        database.taskQueries.insertTemplate(
            id = template.id,
            name = template.name,
            keywords = json.encodeToString(template.keywords),
            subtasks = json.encodeToString(template.subtasks),
            icon = template.icon,
            is_user_created = if (template.isUserCreated) 1L else 0L,
            is_system = if (template.isSystem) 1L else 0L
        )
    }

    private fun DbTask.toDomain(): Task {
        val subtasks = database.taskQueries.getSubtasksByTaskId(id).executeAsList().map { it.toDomain() }
        return Task(
            id = id,
            title = title,
            subtasks = subtasks,
            isCompleted = is_completed != 0L,
            isArchived = is_archived != 0L,
            createdAt = created_at,
            completedAt = completed_at,
            sourceTemplateId = source_template_id,
            iconName = icon_name,
            sortOrder = sort_order.toInt()
        )
    }

    private fun DbSubtask.toDomain(): Subtask {
        return Subtask(
            id = id,
            taskId = task_id,
            text = text,
            isCompleted = is_completed != 0L,
            durationMinutes = duration_minutes.toInt(),
            sortOrder = sort_order.toInt(),
            completedAt = completed_at
        )
    }

    override suspend fun updateTaskSortOrder(id: String, sortOrder: Int) {
        database.taskQueries.updateTaskSortOrder(sortOrder.toLong(), id)
    }

    override suspend fun updateSubtaskSortOrder(taskId: String, orderedIds: List<String>) {
        database.transaction {
            orderedIds.forEachIndexed { index, id ->
                database.taskQueries.updateSubtaskSortOrder(index.toLong(), id)
            }
        }
    }

    private fun DbTaskTemplate.toDomain(): TaskTemplate {
        return TaskTemplate(
            id = id,
            name = name,
            keywords = json.decodeFromString(keywords),
            subtasks = json.decodeFromString(subtasks),
            icon = icon,
            isUserCreated = is_user_created != 0L,
            isSystem = is_system != 0L
        )
    }

    private suspend fun <T> withDbRetry(block: suspend () -> T): T {
        var attempt = 0
        val maxAttempts = 3
        while (true) {
            try {
                return block()
            } catch (e: SQLiteFullException) {
                throw StorageFullException("Device storage is full. Unable to save data. Please free up space and try again.")
            } catch (e: SQLiteDatabaseLockedException) {
                if (++attempt >= maxAttempts) throw e
                delay(50L * attempt)
            }
        }
    }
}
