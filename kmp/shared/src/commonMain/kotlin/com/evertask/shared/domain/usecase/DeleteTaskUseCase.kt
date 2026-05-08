package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository

class DeleteTaskUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(taskId: String) =
        taskRepository.deleteTask(taskId)
}
