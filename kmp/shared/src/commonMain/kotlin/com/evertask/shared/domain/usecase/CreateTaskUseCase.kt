package com.evertask.shared.domain.usecase

import com.evertask.shared.domain.model.Task
import com.evertask.shared.data.repository.TaskRepository

class CreateTaskUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(title: String): Task = taskRepository.createTask(title)
}
