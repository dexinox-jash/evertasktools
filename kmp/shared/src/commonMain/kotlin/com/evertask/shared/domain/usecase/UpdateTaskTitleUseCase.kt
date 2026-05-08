package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository

class UpdateTaskTitleUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(taskId: String, title: String) =
        taskRepository.updateTaskTitle(taskId, title)
}
