package com.evertask.shared.domain.usecase

import com.evertask.shared.data.repository.TaskRepository

class ArchiveTaskUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(taskId: String) =
        taskRepository.archiveTask(taskId)
}
