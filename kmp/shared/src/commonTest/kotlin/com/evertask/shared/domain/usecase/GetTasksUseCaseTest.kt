package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTasksUseCaseTest {

    private val fakeRepository = FakeTaskRepository()
    private val useCase = GetTasksUseCase(fakeRepository)

    @Test
    fun `invoke returns active tasks`() = runTest {
        val tasks = useCase().first()
        assertEquals(1, tasks.size)
        assertEquals("Active Task", tasks[0].title)
    }

    private class FakeTaskRepository : TaskRepository {
        override fun getActiveTasks(): Flow<List<Task>> = flowOf(
            listOf(
                Task(
                    id = "task-1",
                    title = "Active Task",
                    subtasks = emptyList(),
                    isCompleted = false,
                    isArchived = false,
                    createdAt = Instant.parse("2024-01-01T00:00:00Z")
                )
            )
        )
        override fun getArchivedTasks(): Flow<List<Task>> = flowOf(emptyList())
        override fun getAllTasks(): Flow<List<Task>> = flowOf(emptyList())
        override fun searchTasks(query: String): Flow<List<Task>> = flowOf(emptyList())
        override suspend fun getTaskById(id: String): Task? = null
        override suspend fun createTask(title: String): Task = throw NotImplementedError()
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
