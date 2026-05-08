package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository

class UpdateSubtaskTextUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(taskId: String, subtaskId: String, text: String) =
        taskRepository.updateSubtaskText(taskId, subtaskId, text)
}
