package com.evertask.shared.di

import android.app.Application
import android.content.Context
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.evertask.database.EverTaskDatabase
import com.evertask.shared.data.repository.AndroidTemplateEngine
import com.evertask.shared.data.repository.SqlDelightTaskDataSource
import com.evertask.shared.data.repository.TaskDataSource
import com.evertask.shared.data.repository.TemplateEngine
import kotlinx.datetime.Instant
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun androidModule(context: Context) = module {
    single<Context> { context }
    single<Application> { context.applicationContext as? Application ?: error("Context is not an Application") }
    single<SqlDriver> { createAndroidDriver(context) }
    single<EverTaskDatabase> { createDatabase(get()) }
    single<TaskDataSource> { SqlDelightTaskDataSource(get()) }
    single<TemplateEngine> { AndroidTemplateEngine(get()) }
}

actual fun initKoin(): Koin {
    return startKoin {
        modules(commonModule())
    }.koin
}

fun createAndroidDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(EverTaskDatabase.Schema, context, "evertask.db")
}

fun createDatabase(driver: SqlDriver): EverTaskDatabase {
    return EverTaskDatabase(
        driver = driver,
        taskAdapter = com.evertask.database.Task.Adapter(
            created_atAdapter = InstantAdapter,
            completed_atAdapter = InstantAdapter
        ),
        subtaskAdapter = com.evertask.database.Subtask.Adapter(
            completed_atAdapter = InstantAdapter
        )
    )
}

object BooleanAdapter : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long): Boolean = databaseValue != 0L
    override fun encode(value: Boolean): Long = if (value) 1L else 0L
}

object InstantAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.fromEpochMilliseconds(databaseValue)
    override fun encode(value: Instant): Long = value.toEpochMilliseconds()
}

fun initKoinAndroid(context: Context): Koin {
    return startKoin {
        androidContext(context)
        modules(commonModule(), androidModule(context))
    }.koin
}
