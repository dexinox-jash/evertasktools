package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository

class ReorderSubtasksUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(taskId: String, orderedSubtaskIds: List<String>) = repository.reorderSubtasks(taskId, orderedSubtaskIds)
}
