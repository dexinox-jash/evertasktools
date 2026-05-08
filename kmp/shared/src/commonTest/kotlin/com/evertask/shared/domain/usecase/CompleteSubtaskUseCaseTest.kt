package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.domain.model.Subtask
import com.evertask.shared.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertTrue

class CompleteSubtaskUseCaseTest {

    private val fakeRepository = FakeTaskRepository()
    private val useCase = CompleteSubtaskUseCase(fakeRepository)

    @Test
    fun `invoke completes subtask`() = runTest {
        useCase("task-1", "sub-1")
        assertTrue(fakeRepository.wasCompleteSubtaskCalled)
    }

    private class FakeTaskRepository : TaskRepository {
        var wasCompleteSubtaskCalled = false

        override fun getActiveTasks(): Flow<List<Task>> = flowOf(emptyList())
        override fun getArchivedTasks(): Flow<List<Task>> = flowOf(emptyList())
        override fun getAllTasks(): Flow<List<Task>> = flowOf(emptyList())
        override fun searchTasks(query: String): Flow<List<Task>> = flowOf(emptyList())
        override suspend fun getTaskById(id: String): Task? = null
        override suspend fun createTask(title: String): Task = throw NotImplementedError()
        override suspend fun completeSubtask(taskId: String, subtaskId: String) {
            wasCompleteSubtaskCalled = true
        }
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
