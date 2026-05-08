package com.evertask.shared.data.repository

import com.evertask.shared.domain.model.Subtask
import com.evertask.shared.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import java.util.UUID

class TaskRepositoryImpl(
    private val taskDataSource: TaskDataSource,
    private val templateEngine: TemplateEngine
) : TaskRepository {

    override fun getActiveTasks(): Flow<List<Task>> = taskDataSource.getActiveTasks()
    override fun getArchivedTasks(): Flow<List<Task>> = taskDataSource.getArchivedTasks()
    override fun getAllTasks(): Flow<List<Task>> = taskDataSource.getAllTasks()
    override fun searchTasks(query: String): Flow<List<Task>> = taskDataSource.searchTasks(query)

    override suspend fun getTaskById(id: String): Task? = taskDataSource.getTaskById(id)

    override suspend fun createTask(title: String): Task {
        val template = templateEngine.findTemplate(title)
        val now = Clock.System.now()
        val taskId = generateId()

        val subtasks = template.subtasks.mapIndexed { index, templateSubtask ->
            Subtask(
                id = generateId(),
                taskId = taskId,
                text = templateSubtask.text,
                durationMinutes = templateSubtask.durationMinutes,
                sortOrder = index
            )
        }

        val task = Task(
            id = taskId,
            title = title,
            subtasks = subtasks,
            isCompleted = false,
            isArchived = false,
            createdAt = now,
            completedAt = null,
            sourceTemplateId = template.id,
            iconName = template.icon
        )

        taskDataSource.insertTask(task)
        return task
    }

    override suspend fun completeSubtask(taskId: String, subtaskId: String) {
        val task = taskDataSource.getTaskById(taskId) ?: return
        val now = Clock.System.now()
        val updatedSubtasks = task.subtasks.map { subtask ->
            if (subtask.id == subtaskId) {
                subtask.copy(isCompleted = true, completedAt = now)
            } else {
                subtask
            }
        }
        val allCompleted = updatedSubtasks.isNotEmpty() && updatedSubtasks.all { it.isCompleted }
        taskDataSource.updateTask(
            task.copy(
                subtasks = updatedSubtasks,
                isCompleted = allCompleted,
                completedAt = if (allCompleted) now else task.completedAt
            )
        )
    }

    override suspend fun toggleSubtask(taskId: String, subtaskId: String) {
        val task = taskDataSource.getTaskById(taskId) ?: return
        val subtask = task.subtasks.find { it.id == subtaskId } ?: return
        val now = Clock.System.now()
        val isCompleted = !subtask.isCompleted
        val updatedSubtasks = task.subtasks.map {
            if (it.id == subtaskId) {
                it.copy(isCompleted = isCompleted, completedAt = if (isCompleted) now else null)
            } else {
                it
            }
        }
        val allCompleted = updatedSubtasks.isNotEmpty() && updatedSubtasks.all { it.isCompleted }
        val anyCompleted = updatedSubtasks.any { it.isCompleted }
        taskDataSource.updateTask(
            task.copy(
                subtasks = updatedSubtasks,
                isCompleted = allCompleted,
                completedAt = when {
                    allCompleted -> now
                    anyCompleted -> task.completedAt
                    else -> null
                }
            )
        )
    }

    override suspend fun archiveTask(taskId: String) {
        val task = taskDataSource.getTaskById(taskId) ?: return
        taskDataSource.updateTask(task.copy(isArchived = true))
    }

    override suspend fun unarchiveTask(taskId: String) {
        val task = taskDataSource.getTaskById(taskId) ?: return
        taskDataSource.updateTask(task.copy(isArchived = false))
    }

    override suspend fun deleteTask(taskId: String) {
        taskDataSource.deleteTask(taskId)
    }

    override suspend fun addSubtask(taskId: String, text: String, durationMinutes: Int) {
        val task = taskDataSource.getTaskById(taskId) ?: return
        val newSubtask = Subtask(
            id = generateId(),
            taskId = taskId,
            text = text,
            durationMinutes = durationMinutes,
            sortOrder = task.subtasks.size
        )
        taskDataSource.updateTask(task.copy(subtasks = task.subtasks + newSubtask))
    }

    override suspend fun removeSubtask(taskId: String, subtaskId: String) {
        val task = taskDataSource.getTaskById(taskId) ?: return
        val updatedSubtasks = task.subtasks.filter { it.id != subtaskId }
        val allCompleted = updatedSubtasks.isNotEmpty() && updatedSubtasks.all { it.isCompleted }
        taskDataSource.updateTask(
            task.copy(
                subtasks = updatedSubtasks,
                isCompleted = allCompleted,
                completedAt = if (allCompleted) task.completedAt else null
            )
        )
    }

    override suspend fun updateSubtaskText(taskId: String, subtaskId: String, text: String) {
        val task = taskDataSource.getTaskById(taskId) ?: return
        val updatedSubtasks = task.subtasks.map {
            if (it.id == subtaskId) it.copy(text = text) else it
        }
        taskDataSource.updateTask(task.copy(subtasks = updatedSubtasks))
    }

    override suspend fun updateTaskTitle(taskId: String, title: String) {
        val task = taskDataSource.getTaskById(taskId) ?: return
        taskDataSource.updateTask(task.copy(title = title))
    }

    override suspend fun reorderTasks(orderedIds: List<String>) {
        orderedIds.withIndex().forEach { (index, id) ->
            taskDataSource.updateTaskSortOrder(id, index)
        }
    }

    override suspend fun reorderSubtasks(taskId: String, orderedSubtaskIds: List<String>) {
        taskDataSource.updateSubtaskSortOrder(taskId, orderedSubtaskIds)
    }

    private fun generateId(): String = UUID.randomUUID().toString()
}
