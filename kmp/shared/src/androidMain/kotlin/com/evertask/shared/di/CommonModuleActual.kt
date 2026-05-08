package com.evertask.shared.di

import com.evertask.shared.data.repository.TaskRepository
import com.evertask.shared.data.repository.TaskRepositoryImpl
import org.koin.core.context.GlobalContext

actual fun createTaskRepository(): TaskRepository {
    return TaskRepositoryImpl(
        taskDataSource = GlobalContext.get().get(),
        templateEngine = GlobalContext.get().get()
    )
}
