package com.evertask.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.evertask.app.receiver.VoiceCommandReceiver.Companion.ACTION_COMPLETE_ITEM
import com.evertask.app.receiver.VoiceCommandReceiver.Companion.ACTION_CREATE_TASK
import com.evertask.app.receiver.VoiceCommandReceiver.Companion.ACTION_DELETE_ITEM
import com.evertask.app.receiver.VoiceCommandReceiver.Companion.ACTION_READ_TASKS
import com.evertask.app.widget.WidgetUpdater
import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.domain.model.Subtask
import com.evertask.shared.domain.model.Task
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
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
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceCommandReceiverTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var repository: TaskRepository
    private lateinit var receiver: VoiceCommandReceiver
    private lateinit var pendingResult: BroadcastReceiver.PendingResult

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        pendingResult = mockk(relaxed = true)

        receiver = spyk(VoiceCommandReceiver())
        every { receiver.goAsync() } returns pendingResult

        val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
        mockkObject(GlobalContext)
        every { GlobalContext.get() } returns mockk {
            every { get<TaskRepository>() } returns repository
            every { get<WidgetUpdater>() } returns widgetUpdater
        }

        mockkStatic(Toast::class)
        every { Toast.makeText(any<Context>(), any<String>(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onReceive with CREATE_TASK action creates task and shows toast`() = runBlocking {
        val title = "Buy groceries"
        val task = createTask(title)
        coEvery { repository.createTask(title) } returns task

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.getQueryParameter("title") } returns title
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_CREATE_TASK
        every { intent.data } returns dataUri

        receiver.onReceive(context, intent)
        delay(100)

        coVerify { repository.createTask(title) }
        verify { Toast.makeText(context, "Created task: $title", Toast.LENGTH_SHORT) }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with CREATE_TASK without title uses default`() = runBlocking {
        val task = createTask("New Task")
        coEvery { repository.createTask("New Task") } returns task

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_CREATE_TASK
        every { intent.data } returns null

        receiver.onReceive(context, intent)
        delay(100)

        coVerify { repository.createTask("New Task") }
        verify { Toast.makeText(context, "Created task: New Task", Toast.LENGTH_SHORT) }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with READ_TASKS shows active task count`() = runBlocking {
        val tasks = listOf(createTask("Task 1"), createTask("Task 2"))
        every { repository.getActiveTasks() } returns flowOf(tasks)

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_READ_TASKS

        receiver.onReceive(context, intent)
        delay(100)

        verify { Toast.makeText(context, "You have 2 active tasks: Task 1 with 0 steps remaining, Task 2 with 0 steps remaining", Toast.LENGTH_LONG) }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with COMPLETE_ITEM matches subtask and completes it`() = runBlocking {
        val subtask = createSubtask("Buy milk")
        val task = createTask("Shopping", subtasks = listOf(subtask))
        every { repository.getActiveTasks() } returns flowOf(listOf(task))
        coEvery { repository.completeSubtask(task.id, subtask.id) } just runs

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.getQueryParameter("item") } returns "Buy milk"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_COMPLETE_ITEM
        every { intent.data } returns dataUri

        receiver.onReceive(context, intent)
        delay(100)

        coVerify { repository.completeSubtask(task.id, subtask.id) }
        verify { Toast.makeText(context, "Completed Buy milk", Toast.LENGTH_SHORT) }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with COMPLETE_ITEM matches task when no subtask found`() = runBlocking {
        val task = createTask("Clean room")
        every { repository.getActiveTasks() } returns flowOf(listOf(task))
        coEvery { repository.archiveTask(task.id) } just runs

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.getQueryParameter("item") } returns "Clean"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_COMPLETE_ITEM
        every { intent.data } returns dataUri

        receiver.onReceive(context, intent)
        delay(100)

        coVerify { repository.archiveTask(task.id) }
        verify { Toast.makeText(context, "Completed Clean room", Toast.LENGTH_SHORT) }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with COMPLETE_ITEM without item shows error toast`() = runBlocking {
        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.getQueryParameter("item") } returns null
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_COMPLETE_ITEM
        every { intent.data } returns dataUri

        receiver.onReceive(context, intent)
        delay(100)

        verify { Toast.makeText(context, "Please specify what to complete", Toast.LENGTH_SHORT) }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with COMPLETE_ITEM not found shows not found toast`() = runBlocking {
        every { repository.getActiveTasks() } returns flowOf(emptyList())

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.getQueryParameter("item") } returns "Unknown"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_COMPLETE_ITEM
        every { intent.data } returns dataUri

        receiver.onReceive(context, intent)
        delay(100)

        verify { Toast.makeText(context, "Item not found", Toast.LENGTH_SHORT) }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with DELETE_ITEM deletes matched task`() = runBlocking {
        val task = createTask("Old task")
        every { repository.getActiveTasks() } returns flowOf(listOf(task))
        coEvery { repository.deleteTask(task.id) } just runs

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.getQueryParameter("item") } returns "Old"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_DELETE_ITEM
        every { intent.data } returns dataUri

        receiver.onReceive(context, intent)
        delay(100)

        coVerify { repository.deleteTask(task.id) }
        verify { Toast.makeText(context, "Deleted Old task", Toast.LENGTH_SHORT) }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with DELETE_ITEM without item shows error toast`() = runBlocking {
        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.getQueryParameter("item") } returns null
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_DELETE_ITEM
        every { intent.data } returns dataUri

        receiver.onReceive(context, intent)
        delay(100)

        verify { Toast.makeText(context, "Please specify what to delete", Toast.LENGTH_SHORT) }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive with DELETE_ITEM not found shows not found toast`() = runBlocking {
        every { repository.getActiveTasks() } returns flowOf(emptyList())

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.getQueryParameter("item") } returns "Unknown"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_DELETE_ITEM
        every { intent.data } returns dataUri

        receiver.onReceive(context, intent)
        delay(100)

        verify { Toast.makeText(context, "Item not found", Toast.LENGTH_SHORT) }
        verify { pendingResult.finish() }
    }

    @Test
    fun `onReceive handles repository exception and shows error toast`() = runBlocking {
        coEvery { repository.createTask(any()) } throws RuntimeException("DB error")

        val dataUri = mockk<Uri>(relaxed = true)
        every { dataUri.getQueryParameter("title") } returns "Test"
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns ACTION_CREATE_TASK
        every { intent.data } returns dataUri

        receiver.onReceive(context, intent)
        delay(100)

        verify { Toast.makeText(context, "Error: DB error", Toast.LENGTH_SHORT) }
        verify { pendingResult.finish() }
    }

    private fun createTask(
        title: String,
        subtasks: List<Subtask> = emptyList()
    ): Task {
        return Task(
            id = "task-${System.nanoTime()}",
            title = title,
            subtasks = subtasks,
            createdAt = Clock.System.now()
        )
    }

    private fun createSubtask(text: String): Subtask {
        return Subtask(
            id = "sub-${System.nanoTime()}",
            taskId = "task-1",
            text = text
        )
    }
}
