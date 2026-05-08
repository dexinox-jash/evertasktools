package com.evertask.shared.data.repository

import com.evertask.shared.domain.model.TaskTemplate

interface TemplateEngine {
    fun findTemplate(input: String): TaskTemplate
    fun getDefaultTemplates(): List<TaskTemplate>
}
