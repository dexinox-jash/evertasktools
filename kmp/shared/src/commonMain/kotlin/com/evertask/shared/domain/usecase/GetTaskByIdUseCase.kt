package com.evertask.shared.domain.usecase

import com.evertask.shared.domain.model.Task
import com.evertask.shared.data.repository.TaskRepository

class GetTaskByIdUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(id: String): Task? = taskRepository.getTaskById(id)
}
