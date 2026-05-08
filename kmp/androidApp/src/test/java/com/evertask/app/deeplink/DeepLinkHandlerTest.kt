package com.evertask.app.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.evertask.app.scheduler.AndroidReminderScheduler
import com.evertask.app.widget.WidgetUpdater
import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.domain.model.Task
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.java.KoinJavaComponent

@OptIn(ExperimentalCoroutinesApi::class)
class DeepLinkHandlerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var repository: TaskRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        DeepLinkHandler.ioDispatcher = testDispatcher
        context = mockk(relaxed = true)
        repository = mockk(relaxed = true)

        val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
        mockkStatic(KoinJavaComponent::class)
        every { KoinJavaComponent.getKoin() } returns mockk {
            every { get<TaskRepository>() } returns repository
            every { get<WidgetUpdater>() } returns widgetUpdater
        }

        mockkStatic(Toast::class)
        every { Toast.makeText(any<Context>(), any<String>(), any()) } returns mockk(relaxed = true)

        mockkConstructor(AndroidReminderScheduler::class)
        every { anyConstructed<AndroidReminderScheduler>().snoozeReminder(any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        DeepLinkHandler.ioDispatcher = Dispatchers.IO
    }

    @Test
    fun `handleDeepLink with evertask create creates task`() = runBlocking {
        val title = "New task"
        coEvery { repository.createTask(title) } returns createTask(title)

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.scheme } returns "evertask"
        every { dataUri.host } returns "create"
        every { dataUri.getQueryParameter("title") } returns title
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns dataUri
        every { intent.action } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify { repository.createTask(title) }
        verify { Toast.makeText(context, "Task created", Toast.LENGTH_SHORT) }
    }

    @Test
    fun `handleDeepLink with evertask create missing title does nothing`() = runBlocking {
        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.scheme } returns "evertask"
        every { dataUri.host } returns "create"
        every { dataUri.getQueryParameter("title") } returns null
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns dataUri
        every { intent.action } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify(exactly = 0) { repository.createTask(any()) }
    }

    @Test
    fun `handleDeepLink with evertask complete archives task`() = runBlocking {
        coEvery { repository.archiveTask("task-1") } just runs

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.scheme } returns "evertask"
        every { dataUri.host } returns "complete"
        every { dataUri.getQueryParameter("taskId") } returns "task-1"
        every { dataUri.getQueryParameter("subtaskId") } returns null
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns dataUri
        every { intent.action } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify { repository.archiveTask("task-1") }
        verify { Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT) }
    }

    @Test
    fun `handleDeepLink with evertask complete with subtaskId completes subtask`() = runBlocking {
        coEvery { repository.completeSubtask("task-1", "sub-1") } just runs

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.scheme } returns "evertask"
        every { dataUri.host } returns "complete"
        every { dataUri.getQueryParameter("taskId") } returns "task-1"
        every { dataUri.getQueryParameter("subtaskId") } returns "sub-1"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns dataUri
        every { intent.action } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify { repository.completeSubtask("task-1", "sub-1") }
        verify { Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT) }
    }

    @Test
    fun `handleDeepLink with evertask complete missing taskId does nothing`() = runBlocking {
        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.scheme } returns "evertask"
        every { dataUri.host } returns "complete"
        every { dataUri.getQueryParameter("taskId") } returns null
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns dataUri
        every { intent.action } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify(exactly = 0) { repository.archiveTask(any()) }
        coVerify(exactly = 0) { repository.completeSubtask(any(), any()) }
    }

    @Test
    fun `handleDeepLink with evertask read shows active tasks`() = runBlocking {
        val tasks = listOf(createTask("Task A"), createTask("Task B"))
        every { repository.getActiveTasks() } returns flowOf(tasks)

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.scheme } returns "evertask"
        every { dataUri.host } returns "read"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns dataUri
        every { intent.action } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        verify { Toast.makeText(context, "You have 2 active tasks: Task A with 0 steps remaining, Task B with 0 steps remaining", Toast.LENGTH_LONG) }
    }

    @Test
    fun `handleDeepLink with evertask delete deletes task`() = runBlocking {
        coEvery { repository.deleteTask("task-1") } just runs

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.scheme } returns "evertask"
        every { dataUri.host } returns "delete"
        every { dataUri.getQueryParameter("taskId") } returns "task-1"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns dataUri
        every { intent.action } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify { repository.deleteTask("task-1") }
        verify { Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT) }
    }

    @Test
    fun `handleDeepLink with evertask delete missing taskId does nothing`() = runBlocking {
        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.scheme } returns "evertask"
        every { dataUri.host } returns "delete"
        every { dataUri.getQueryParameter("taskId") } returns null
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns dataUri
        every { intent.action } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify(exactly = 0) { repository.deleteTask(any()) }
    }

    @Test
    fun `handleDeepLink with notification action COMPLETE archives task`() = runBlocking {
        coEvery { repository.archiveTask("task-1") } just runs

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns "com.evertask.app.ACTION_COMPLETE"
        every { intent.getStringExtra("taskId") } returns "task-1"
        every { intent.getStringExtra("subtaskId") } returns null
        every { intent.data } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify { repository.archiveTask("task-1") }
        verify { Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT) }
    }

    @Test
    fun `handleDeepLink with notification action COMPLETE with subtaskId completes subtask`() = runBlocking {
        coEvery { repository.completeSubtask("task-1", "sub-1") } just runs

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns "com.evertask.app.ACTION_COMPLETE"
        every { intent.getStringExtra("taskId") } returns "task-1"
        every { intent.getStringExtra("subtaskId") } returns "sub-1"
        every { intent.data } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify { repository.completeSubtask("task-1", "sub-1") }
        verify { Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT) }
    }

    @Test
    fun `handleDeepLink with notification action SKIP archives task`() = runBlocking {
        coEvery { repository.archiveTask("task-1") } just runs

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns "com.evertask.app.ACTION_SKIP"
        every { intent.getStringExtra("taskId") } returns "task-1"
        every { intent.data } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify { repository.archiveTask("task-1") }
        verify { Toast.makeText(context, "Task skipped", Toast.LENGTH_SHORT) }
    }

    @Test
    fun `handleDeepLink with notification action SNOOZE snoozes reminder`() {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns "com.evertask.app.ACTION_SNOOZE"
        every { intent.getStringExtra("taskId") } returns "task-1"
        every { intent.data } returns null

        DeepLinkHandler.handleDeepLink(context, intent)

        verify { anyConstructed<AndroidReminderScheduler>().snoozeReminder("task-1") }
        verify { Toast.makeText(context, "Snoozed for 15 minutes", Toast.LENGTH_SHORT) }
    }

    @Test
    fun `handleDeepLink with non evertask scheme does nothing`() = runBlocking {
        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.scheme } returns "https"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns dataUri
        every { intent.action } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify(exactly = 0) { repository.createTask(any()) }
    }

    @Test
    fun `handleDeepLink with null data and unknown action does nothing`() = runBlocking {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns null
        every { intent.action } returns "unknown.action"

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        coVerify(exactly = 0) { repository.createTask(any()) }
    }

    @Test
    fun `handleDeepLink with evertask create exception shows error toast`() = runBlocking {
        coEvery { repository.createTask(any()) } throws RuntimeException("DB error")

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.scheme } returns "evertask"
        every { dataUri.host } returns "create"
        every { dataUri.getQueryParameter("title") } returns "Test"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.data } returns dataUri
        every { intent.action } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        verify { Toast.makeText(context, "Failed to create task", Toast.LENGTH_SHORT) }
    }

    @Test
    fun `handleDeepLink with notification action COMPLETE exception shows error toast`() = runBlocking {
        coEvery { repository.completeSubtask(any(), any()) } throws RuntimeException("DB error")

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns "com.evertask.app.ACTION_COMPLETE"
        every { intent.getStringExtra("taskId") } returns "task-1"
        every { intent.getStringExtra("subtaskId") } returns "sub-1"
        every { intent.data } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        verify { Toast.makeText(context, "Failed to complete task", Toast.LENGTH_SHORT) }
    }

    @Test
    fun `handleDeepLink with notification action SKIP exception shows error toast`() = runBlocking {
        coEvery { repository.archiveTask(any()) } throws RuntimeException("DB error")

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns "com.evertask.app.ACTION_SKIP"
        every { intent.getStringExtra("taskId") } returns "task-1"
        every { intent.data } returns null

        DeepLinkHandler.handleDeepLink(context, intent)
        delay(100)

        verify { Toast.makeText(context, "Failed to skip task", Toast.LENGTH_SHORT) }
    }

    private fun createTask(title: String): Task {
        return Task(
            id = "task-${System.nanoTime()}",
            title = title,
            createdAt = Clock.System.now()
        )
    }
}
