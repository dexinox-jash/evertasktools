package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.domain.model.Subtask
import com.evertask.shared.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateTaskUseCaseTest {

    private val fakeRepository = FakeTaskRepository()
    private val useCase = CreateTaskUseCase(fakeRepository)

    @Test
    fun `invoke creates task via repository`() = runTest {
        val task = useCase("clean my room")
        assertEquals("clean my room", task.title)
        assertTrue(task.subtasks.isNotEmpty())
    }

    private class FakeTaskRepository : TaskRepository {
        private val tasks = mutableListOf<Task>()

        override fun getActiveTasks(): Flow<List<Task>> = flowOf(tasks.filter { !it.isArchived })
        override fun getArchivedTasks(): Flow<List<Task>> = flowOf(tasks.filter { it.isArchived })
        override fun getAllTasks(): Flow<List<Task>> = flowOf(tasks)
        override fun searchTasks(query: String): Flow<List<Task>> = flowOf(emptyList())
        override suspend fun getTaskById(id: String): Task? = tasks.find { it.id == id }
        override suspend fun createTask(title: String): Task {
            val task = Task(
                id = "task-1",
                title = title,
                subtasks = listOf(
                    Subtask(id = "sub-1", taskId = "task-1", text = "Step 1", durationMinutes = 5, sortOrder = 0)
                ),
                isCompleted = false,
                isArchived = false,
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                completedAt = null
            )
            tasks.add(task)
            return task
        }
        override suspend fun completeSubtask(taskId: String, subtaskId: String) {}
        override suspend fun toggleSubtask(taskId: String, subtaskId: String) {}
        override suspend fun archiveTask(taskId: String) {}
        override suspend fun unarchiveTask(taskId: String) {}
        override suspend fun deleteTask(taskId: String) {}
        override suspend fun addSubtask(taskId: String, text: String, durationMinutes: Int) {}
        override suspend fun removeSubtask(taskId: String, subtaskId: String) {}
        override suspend fun updateSubtaskText(taskId: String, subtaskId: String, text: String) {}
        override suspend fun updateTaskTitle(taskId: String, title: String) {}
        override suspend fun reorderTasks(orderedIds: List<String>) {}
        override suspend fun reorderSubtasks(taskId: String, orderedSubtaskIds: List<String>) {}
    }
}
