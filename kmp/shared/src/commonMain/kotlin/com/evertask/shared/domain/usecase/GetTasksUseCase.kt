package com.evertask.shared.domain.usecase

import com.evertask.shared.domain.model.Task
import com.evertask.shared.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class GetTasksUseCase(private val taskRepository: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> = taskRepository.getActiveTasks()
}
