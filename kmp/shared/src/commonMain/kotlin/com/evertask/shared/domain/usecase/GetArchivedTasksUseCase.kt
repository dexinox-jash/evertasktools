package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.domain.model.Task
import kotlinx.coroutines.flow.Flow

class GetArchivedTasksUseCase(private val taskRepository: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> = taskRepository.getArchivedTasks()
}
