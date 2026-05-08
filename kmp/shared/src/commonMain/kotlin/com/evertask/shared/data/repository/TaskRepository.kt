package com.evertask.shared.data.repository

import com.evertask.shared.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getActiveTasks(): Flow<List<Task>>
    fun getArchivedTasks(): Flow<List<Task>>
    fun getAllTasks(): Flow<List<Task>>
    fun searchTasks(query: String): Flow<List<Task>>
    suspend fun getTaskById(id: String): Task?
    suspend fun createTask(title: String): Task
    suspend fun completeSubtask(taskId: String, subtaskId: String)
    suspend fun toggleSubtask(taskId: String, subtaskId: String)
    suspend fun archiveTask(taskId: String)
    suspend fun unarchiveTask(taskId: String)
    suspend fun deleteTask(taskId: String)
    suspend fun addSubtask(taskId: String, text: String, durationMinutes: Int = 0)
    suspend fun removeSubtask(taskId: String, subtaskId: String)
    suspend fun updateSubtaskText(taskId: String, subtaskId: String, text: String)
    suspend fun updateTaskTitle(taskId: String, title: String)
    suspend fun reorderTasks(orderedIds: List<String>)
    suspend fun reorderSubtasks(taskId: String, orderedSubtaskIds: List<String>)
}
