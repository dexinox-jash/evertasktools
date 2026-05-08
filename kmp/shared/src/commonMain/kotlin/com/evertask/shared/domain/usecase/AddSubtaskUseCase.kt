package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository

class AddSubtaskUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(taskId: String, text: String, durationMinutes: Int = 0) =
        taskRepository.addSubtask(taskId, text, durationMinutes)
}
