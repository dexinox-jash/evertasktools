package com.evertask.app.ui

import android.app.Application
import com.evertask.app.scheduler.ReminderScheduler
import com.evertask.app.widget.WidgetUpdater
import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.domain.model.Subtask
import com.evertask.shared.domain.model.Task
import com.evertask.shared.domain.usecase.AddSubtaskUseCase
import com.evertask.shared.domain.usecase.ArchiveTaskUseCase
import com.evertask.shared.domain.usecase.CompleteSubtaskUseCase
import com.evertask.shared.domain.usecase.CreateTaskUseCase
import com.evertask.shared.domain.usecase.DeleteTaskUseCase
import com.evertask.shared.domain.usecase.GetArchivedTasksUseCase
import com.evertask.shared.domain.usecase.GetTasksUseCase
import com.evertask.shared.domain.usecase.RemoveSubtaskUseCase
import com.evertask.shared.domain.usecase.UpdateSubtaskTextUseCase
import com.evertask.shared.domain.usecase.ReorderSubtasksUseCase
import com.evertask.shared.domain.usecase.ReorderTasksUseCase
import com.evertask.shared.domain.usecase.UpdateTaskTitleUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var app: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = mockk(relaxed = true)
        every { app.applicationContext } returns app
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty tasks and history`() = runTest {
        val (viewModel, _) = createViewModelAndRepository()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.tasks.isEmpty())
        assertTrue(state.history.isEmpty())
        assertFalse(state.isHistoryVisible)
        assertNull(state.completedTask)
    }

    @Test
    fun `createTask updates uiState with new task`() = runTest {
        val (viewModel, _) = createViewModelAndRepository()
        viewModel.createTask("test task")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("test task", state.tasks[0].title)
        assertEquals(VoiceInputState.Idle, state.voiceInputState)
        assertNull(state.error)
    }

    @Test
    fun `completeSubtask marks subtask completed`() = runTest {
        val repo = FakeTaskRepository()
        val task = repo.addTask("test task", listOf("Step 1"))
        val viewModel = createViewModelWithRepository(repo)
        advanceUntilIdle()

        val subtask = viewModel.uiState.value.tasks.first().subtasks.first()
        viewModel.completeSubtask(task.id, subtask.id)

        val updatedTask = viewModel.uiState.value.tasks.first()
        assertTrue(updatedTask.subtasks.first { it.id == subtask.id }.isCompleted)
    }

    @Test
    fun `archiveTask removes task from uiState`() = runTest {
        val repo = FakeTaskRepository()
        val task = repo.addTask("test task", listOf("Step 1"))
        val viewModel = createViewModelWithRepository(repo)
        advanceUntilIdle()

        viewModel.archiveTask(task.id)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.tasks.isEmpty())
    }

    @Test
    fun `showHistory and hideHistory toggle visibility`() = runTest {
        val (viewModel, _) = createViewModelAndRepository()
        viewModel.showHistory()
        assertTrue(viewModel.uiState.value.isHistoryVisible)

        viewModel.hideHistory()
        assertFalse(viewModel.uiState.value.isHistoryVisible)
    }

    @Test
    fun `updateInputText updates current text`() = runTest {
        val (viewModel, _) = createViewModelAndRepository()
        viewModel.updateInputText("new text")
        assertEquals("new text", viewModel.uiState.value.currentInputText)
    }

    @Test
    fun `voice input states transition correctly`() = runTest {
        val (viewModel, _) = createViewModelAndRepository()
        viewModel.startVoiceInput()
        assertEquals(VoiceInputState.Listening, viewModel.uiState.value.voiceInputState)

        viewModel.stopVoiceInput()
        assertEquals(VoiceInputState.Idle, viewModel.uiState.value.voiceInputState)
    }

    @Test
    fun `onVoiceError sets error state`() = runTest {
        val (viewModel, _) = createViewModelAndRepository()
        viewModel.onVoiceError("Audio error")

        val state = viewModel.uiState.value.voiceInputState
        assertTrue(state is VoiceInputState.Error)
        assertEquals("Audio error", (state as VoiceInputState.Error).message)
    }

    @Test
    fun `onVoiceError auto resets after delay`() = runTest {
        val (viewModel, _) = createViewModelAndRepository()
        viewModel.onVoiceError("Audio error")
        assertTrue(viewModel.uiState.value.voiceInputState is VoiceInputState.Error)

        advanceTimeBy(2500)
        assertEquals(VoiceInputState.Idle, viewModel.uiState.value.voiceInputState)
    }

    @Test
    fun `clearError removes error message`() = runTest {
        val (viewModel, _) = createViewModelAndRepository()
        viewModel.onVoiceError("test error")
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    private fun createViewModelAndRepository(): Pair<TaskViewModel, FakeTaskRepository> {
        val repo = FakeTaskRepository()
        return createViewModelWithRepository(repo) to repo
    }

    private fun createViewModelWithRepository(repo: FakeTaskRepository): TaskViewModel {
        return TaskViewModel(
            app = app,
            createTaskUseCase = CreateTaskUseCase(repo),
            getTasksUseCase = GetTasksUseCase(repo),
            getArchivedTasksUseCase = GetArchivedTasksUseCase(repo),
            completeSubtaskUseCase = CompleteSubtaskUseCase(repo),
            archiveTaskUseCase = ArchiveTaskUseCase(repo),
            deleteTaskUseCase = DeleteTaskUseCase(repo),
            updateTaskTitleUseCase = UpdateTaskTitleUseCase(repo),
            addSubtaskUseCase = AddSubtaskUseCase(repo),
            removeSubtaskUseCase = RemoveSubtaskUseCase(repo),
            updateSubtaskTextUseCase = UpdateSubtaskTextUseCase(repo),
            reorderTasksUseCase = ReorderTasksUseCase(repo),
            reorderSubtasksUseCase = ReorderSubtasksUseCase(repo),
            reminderScheduler = FakeReminderScheduler(),
            widgetUpdater = FakeWidgetUpdater()
        )
    }

    class FakeTaskRepository : TaskRepository {
        private val tasks = mutableListOf<Task>()
        private val archived = mutableListOf<Task>()
        private var idCounter = 1
        private val now = Clock.System.now()
        private val _tasksFlow = MutableStateFlow<List<Task>>(emptyList())

        fun addTask(title: String, subtaskTexts: List<String>): Task {
            val taskId = "task-${idCounter++}"
            val subs = subtaskTexts.mapIndexed { index, text ->
                Subtask(
                    id = "sub-$index",
                    taskId = taskId,
                    text = text,
                    durationMinutes = 5,
                    sortOrder = index
                )
            }
            val task = Task(
                id = taskId,
                title = title,
                subtasks = subs,
                isCompleted = false,
                isArchived = false,
                createdAt = now
            )
            tasks.add(task)
            _tasksFlow.value = tasks.toList()
            return task
        }

        override fun getActiveTasks(): Flow<List<Task>> = _tasksFlow.asStateFlow()
        override fun getArchivedTasks(): Flow<List<Task>> = MutableStateFlow(archived).asStateFlow()
        override fun getAllTasks(): Flow<List<Task>> = MutableStateFlow(tasks + archived).asStateFlow()
        override fun searchTasks(query: String): Flow<List<Task>> = MutableStateFlow(emptyList<Task>()).asStateFlow()
        override suspend fun getTaskById(id: String): Task? = tasks.find { it.id == id }

        override suspend fun createTask(title: String): Task {
            return addTask(title, listOf("Step 1"))
        }

        override suspend fun completeSubtask(taskId: String, subtaskId: String) {
            val index = tasks.indexOfFirst { it.id == taskId }
            if (index != -1) {
                val task = tasks[index]
                val updatedSubtasks = task.subtasks.map {
                    if (it.id == subtaskId) it.copy(isCompleted = true) else it
                }
                tasks[index] = task.copy(subtasks = updatedSubtasks)
                _tasksFlow.value = tasks.toList()
            }
        }

        override suspend fun toggleSubtask(taskId: String, subtaskId: String) {}

        override suspend fun archiveTask(taskId: String) {
            val task = tasks.find { it.id == taskId }
            if (task != null) {
                tasks.remove(task)
                archived.add(task.copy(isArchived = true, createdAt = now))
                _tasksFlow.value = tasks.toList()
            }
        }

        override suspend fun unarchiveTask(taskId: String) {}

        override suspend fun deleteTask(taskId: String) {
            tasks.removeAll { it.id == taskId }
            archived.removeAll { it.id == taskId }
            _tasksFlow.value = tasks.toList()
        }

        override suspend fun addSubtask(taskId: String, text: String, durationMinutes: Int) {}
        override suspend fun removeSubtask(taskId: String, subtaskId: String) {}
        override suspend fun updateSubtaskText(taskId: String, subtaskId: String, text: String) {}
        override suspend fun updateTaskTitle(taskId: String, title: String) {
            val index = tasks.indexOfFirst { it.id == taskId }
            if (index != -1) {
                tasks[index] = tasks[index].copy(title = title)
                _tasksFlow.value = tasks.toList()
            }
        }
        override suspend fun reorderTasks(orderedIds: List<String>) {}
        override suspend fun reorderSubtasks(taskId: String, orderedSubtaskIds: List<String>) {}
    }

    class FakeReminderScheduler : ReminderScheduler {
        override fun scheduleReminder(task: Task, delayMs: Long): Boolean = true
        override fun cancelReminder(taskId: String) {}
        override fun snoozeReminder(taskId: String): Boolean = true
        override fun canScheduleExactAlarms(): Boolean = true
        override fun openExactAlarmSettings() {}
    }

    class FakeWidgetUpdater : WidgetUpdater {
        override suspend fun updateAllWidgets() {}
    }
}
