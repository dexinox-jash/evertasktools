package com.evertask.shared.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.evertask.shared.domain.model.Subtask
import com.evertask.shared.domain.model.Task
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class TaskRepositoryImplTest {

    private lateinit var repository: TaskRepositoryImpl
    private lateinit var taskDataSource: TaskDataSource

    @Before
    fun setup() {
        val context = mockk<Context>()
        val assetManager = mockk<AssetManager>()
        every { context.assets } returns assetManager
        every { assetManager.open(any()) } throws IOException("Asset not found")

        val templateEngine = AndroidTemplateEngine(context)
        taskDataSource = mockk(relaxed = true)
        repository = TaskRepositoryImpl(taskDataSource, templateEngine)
    }

    @Test
    fun `createTask clean kitchen creates task with subtasks from template`() = runTest {
        val taskSlot = slot<Task>()
        coEvery { taskDataSource.insertTask(capture(taskSlot)) } returns Unit

        val task = repository.createTask("clean kitchen")

        assertEquals("clean kitchen", task.title)
        assertEquals("clean_room", task.sourceTemplateId)
        assertEquals(4, task.subtasks.size)
        assertTrue(task.subtasks.any { it.text.contains("Clear trash", ignoreCase = true) })

        coVerify(exactly = 1) { taskDataSource.insertTask(any()) }
    }

    @Test
    fun `completeSubtask updates task correctly`() = runTest {
        val taskId = "task-123"
        val subtaskId1 = "sub-1"
        val subtaskId2 = "sub-2"
        val now = Clock.System.now()

        val initialTask = Task(
            id = taskId,
            title = "Test Task",
            subtasks = listOf(
                Subtask(
                    id = subtaskId1,
                    taskId = taskId,
                    text = "Subtask 1",
                    isCompleted = false,
                    sortOrder = 0,
                    durationMinutes = 5,
                    completedAt = null
                ),
                Subtask(
                    id = subtaskId2,
                    taskId = taskId,
                    text = "Subtask 2",
                    isCompleted = false,
                    sortOrder = 1,
                    durationMinutes = 5,
                    completedAt = null
                )
            ),
            isCompleted = false,
            createdAt = now,
            completedAt = null
        )

        coEvery { taskDataSource.getTaskById(taskId) } returns initialTask

        repository.completeSubtask(taskId, subtaskId1)

        val taskSlot = slot<Task>()
        coVerify { taskDataSource.updateTask(capture(taskSlot)) }

        val updatedTask = taskSlot.captured
        assertTrue(updatedTask.subtasks.find { it.id == subtaskId1 }?.isCompleted == true)
        assertTrue(updatedTask.subtasks.find { it.id == subtaskId2 }?.isCompleted == false)
        assertFalse(updatedTask.isCompleted)
    }
}
