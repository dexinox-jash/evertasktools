package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository

class RemoveSubtaskUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(taskId: String, subtaskId: String) =
        taskRepository.removeSubtask(taskId, subtaskId)
}
