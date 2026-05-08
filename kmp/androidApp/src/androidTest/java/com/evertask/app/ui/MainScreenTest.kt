package com.evertask.app.ui

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.evertask.app.ui.screens.MainScreen
import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.domain.model.Task
import com.evertask.shared.domain.usecase.ArchiveTaskUseCase
import com.evertask.shared.domain.usecase.CompleteSubtaskUseCase
import com.evertask.shared.domain.usecase.CreateTaskUseCase
import com.evertask.shared.domain.usecase.DeleteTaskUseCase
import com.evertask.shared.domain.usecase.GetArchivedTasksUseCase
import com.evertask.shared.domain.usecase.GetTasksUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeTaskRepository()

        val viewModel = TaskViewModel(
            app = app,
            createTaskUseCase = CreateTaskUseCase(fakeRepository),
            getTasksUseCase = GetTasksUseCase(fakeRepository),
            getArchivedTasksUseCase = GetArchivedTasksUseCase(fakeRepository),
            completeSubtaskUseCase = CompleteSubtaskUseCase(fakeRepository),
            archiveTaskUseCase = ArchiveTaskUseCase(fakeRepository),
            deleteTaskUseCase = DeleteTaskUseCase(fakeRepository),
            reminderScheduler = FakeReminderScheduler(),
            widgetUpdater = FakeWidgetUpdater()
        )

        composeTestRule.setContent {
            MainScreen(viewModel = viewModel)
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun mainScreen_rendersEmptyStateText() {
        composeTestRule.onNodeWithText("No tasks yet").assertExists()
    }

    @Test
    fun clickingFabShowsCreateDialog() {
        composeTestRule.onNodeWithText("New Task").performClick()
        composeTestRule.onNodeWithText("Create New Task").assertExists()
    }

    private class FakeReminderScheduler : com.evertask.app.scheduler.ReminderScheduler {
        override fun scheduleReminder(task: com.evertask.shared.domain.model.Task, delayMs: Long) {}
        override fun cancelReminder(taskId: String) {}
        override fun snoozeReminder(taskId: String) {}
    }

    private class FakeWidgetUpdater : com.evertask.app.widget.WidgetUpdater {
        override suspend fun updateAllWidgets() {}
    }

    private class FakeTaskRepository : TaskRepository {
        override fun getActiveTasks(): Flow<List<Task>> = flowOf(emptyList())
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
    }
}
