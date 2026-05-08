package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository

class UnarchiveTaskUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(taskId: String) =
        taskRepository.unarchiveTask(taskId)
}
