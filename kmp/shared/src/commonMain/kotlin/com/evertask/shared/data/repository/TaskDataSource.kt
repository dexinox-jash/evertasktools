package com.evertask.shared.data.repository

import com.evertask.shared.domain.model.Task
import com.evertask.shared.domain.model.TaskTemplate
import kotlinx.coroutines.flow.Flow

interface TaskDataSource {
    fun getActiveTasks(): Flow<List<Task>>
    fun getArchivedTasks(): Flow<List<Task>>
    fun getAllTasks(): Flow<List<Task>>
    fun searchTasks(query: String): Flow<List<Task>>
    suspend fun getTaskById(id: String): Task?
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: String)
    suspend fun getTemplates(): List<TaskTemplate>
    suspend fun insertTemplate(template: TaskTemplate)
    suspend fun updateTaskSortOrder(id: String, sortOrder: Int)
    suspend fun updateSubtaskSortOrder(taskId: String, orderedIds: List<String>)
}
