package com.evertask.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Represents a task template loaded from bundled JSON.
 * Contains keywords for matching, subtask templates, and an icon identifier.
 */
@Serializable
data class TaskTemplate(
    val id: String,
    val keywords: List<String>,
    val subtasks: List<String>,
    val icon: String
) {
    companion object {
        private val json = Json { 
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        
        /**
         * Embedded fallback JSON for when external assets fail to load.
         * This ensures the app always has templates available even if assets are corrupted.
         */
        const val FALLBACK_JSON = """{
            "templates": [
                {"id": "clean_room", "keywords": ["clean", "tidy", "room", "house", "apartment"], "subtasks": ["Clear trash & clutter (2 min)", "Dust visible surfaces (3 min)", "Vacuum/sweep floors (5 min)", "Arrange cushions & items (2 min)"], "icon": "sparkles"},
                {"id": "email_write", "keywords": ["email", "write", "send", "message", "mail"], "subtasks": ["Draft subject line (1 min)", "Write 3 bullet points (3 min)", "Expand to full text (5 min)", "Proofread & send (2 min)"], "icon": "envelope"},
                {"id": "grocery_shop", "keywords": ["grocery", "shop", "buy", "food", "supermarket"], "subtasks": ["Check fridge/pantry (3 min)", "Write list by category (5 min)", "Shop efficiently (20 min)", "Put away items (10 min)"], "icon": "cart"},
                {"id": "study_session", "keywords": ["study", "learn", "read", "homework", "exam"], "subtasks": ["Gather materials (2 min)", "Set 25min timer (Pomodoro)", "Focus reading (25 min)", "Review notes (5 min)"], "icon": "book"},
                {"id": "cook_meal", "keywords": ["cook", "dinner", "lunch", "food", "meal"], "subtasks": ["Gather ingredients (5 min)", "Prep vegetables (10 min)", "Cook main dish (15 min)", "Plate & serve (3 min)", "Clean as you go (5 min)"], "icon": "fork.knife"},
                {"id": "workout_gym", "keywords": ["gym", "workout", "exercise", "fitness", "run"], "subtasks": ["Change & fill water (5 min)", "Warm up stretch (5 min)", "Main workout (30 min)", "Cool down (5 min)", "Shower & recover (10 min)"], "icon": "figure.run"},
                {"id": "meeting_prep", "keywords": ["meeting", "call", "zoom", "presentation"], "subtasks": ["Review agenda (3 min)", "Prepare 2 talking points (5 min)", "Test tech/screen share (2 min)", "Join 2 min early (0 min)"], "icon": "person.2"},
                {"id": "travel_pack", "keywords": ["pack", "travel", "trip", "luggage", "suitcase"], "subtasks": ["Check weather forecast (1 min)", "List clothes by day (5 min)", "Pack toiletries (3 min)", "Charge devices & cables (5 min)", "Final check & lock (2 min)"], "icon": "suitcase"}
            ],
            "default_template": {"subtasks": ["Define the goal clearly (2 min)", "Gather needed materials (5 min)", "Execute main task (15 min)", "Review & finalize (3 min)"], "icon": "checkmark.circle"}
        }"""
        
        /**
         * Parses the template collection from JSON string.
         * Returns null if parsing fails - caller should use fallback.
         */
        fun parseCollection(jsonString: String): TemplateCollection? {
            return runCatching {
                json.decodeFromString<TemplateCollection>(jsonString)
            }.getOrNull()
        }
    }
}

/**
 * Wrapper class for the root JSON structure containing all templates.
 */
@Serializable
data class TemplateCollection(
    val templates: List<TaskTemplate>,
    val default_template: DefaultTemplate
)

/**
 * Default template used when no keyword match is found.
 */
@Serializable
data class DefaultTemplate(
    val subtasks: List<String>,
    val icon: String
)
