package com.evertask.shared.di

import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.data.repository.TaskRepositoryImpl
import com.evertask.shared.domain.usecase.*
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Common Koin module for shared code
 * This module is used by both Android and iOS
 */
fun commonModule(): Module = module {
    // Repository
    single<TaskRepository> { createTaskRepository() }
    
    // Use Cases
    factory { CreateTaskUseCase(get()) }
    factory { GetTasksUseCase(get()) }
    factory { GetArchivedTasksUseCase(get()) }
    factory { CompleteSubtaskUseCase(get()) }
    factory { ToggleSubtaskUseCase(get()) }
    factory { ArchiveTaskUseCase(get()) }
    factory { UnarchiveTaskUseCase(get()) }
    factory { DeleteTaskUseCase(get()) }
    factory { SearchTasksUseCase(get()) }
    factory { GetTaskByIdUseCase(get()) }
    factory { AddSubtaskUseCase(get()) }
    factory { RemoveSubtaskUseCase(get()) }
    factory { UpdateSubtaskTextUseCase(get()) }
    factory { UpdateTaskTitleUseCase(get()) }
    factory { ReorderTasksUseCase(get()) }
    factory { ReorderSubtasksUseCase(get()) }
}

/**
 * Expect declaration for creating the TaskRepository
 * Platform-specific implementations provide the database and template engine
 */
expect fun createTaskRepository(): TaskRepository

/**
 * Initialize Koin with all modules
 */
expect fun initKoin(): org.koin.core.Koin
