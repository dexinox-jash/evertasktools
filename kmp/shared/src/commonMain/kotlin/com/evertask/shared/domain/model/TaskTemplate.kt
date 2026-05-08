package com.evertask.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TaskTemplate(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val subtasks: List<TemplateSubtask>,
    val icon: String = "default_template",
    val isUserCreated: Boolean = false,
    val isSystem: Boolean = false
)

@Serializable
data class TemplateSubtask(
    val text: String,
    val durationMinutes: Int = 0
)
