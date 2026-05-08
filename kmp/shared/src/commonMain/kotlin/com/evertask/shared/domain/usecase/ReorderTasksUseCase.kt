package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository

class ReorderTasksUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(orderedIds: List<String>) = repository.reorderTasks(orderedIds)
}
