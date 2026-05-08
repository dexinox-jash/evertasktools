package com.evertask.shared.domain.usecase

import com.evertask.shared.domain.model.Task
import com.evertask.shared.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class SearchTasksUseCase(private val taskRepository: TaskRepository) {
    operator fun invoke(query: String): Flow<List<Task>> = taskRepository.searchTasks(query)
}
