package com.evertask.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a single subtask within a task.
 * Contains the text description, estimated duration, and completion status.
 */
@Serializable
data class Subtask(
    val id: String,
    val text: String,
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val skippedAt: Long? = null
) {
    companion object {
        /**
         * Parses a subtask string in format "Description (X min)"
         * Returns a Subtask with extracted duration or default 5 minutes if parsing fails
         */
        fun fromTemplateString(templateString: String, index: Int): Subtask {
            val durationRegex = "\\((\\d+)\\s*min\\)".toRegex(RegexOption.IGNORE_CASE)
            val matchResult = durationRegex.find(templateString)
            
            val duration = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 5
            val cleanText = templateString.replace(durationRegex, "").trim()
            
            return Subtask(
                id = "subtask_${index}_${System.currentTimeMillis()}",
                text = cleanText,
                durationMinutes = duration,
                isCompleted = false
            )
        }
    }
}
