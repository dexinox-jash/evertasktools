package com.evertask.data.engine

import android.content.Context
import com.evertask.data.model.DefaultTemplate
import com.evertask.data.model.Subtask
import com.evertask.data.model.TaskTemplate
import com.evertask.data.model.TemplateCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Deterministic template matching engine for Ever Task Tools.
 * 
 * Matching algorithm (in priority order):
 * 1. Exact phrase match - checks if task title contains a complete keyword phrase
 * 2. Keyword density scoring - calculates match score based on keyword occurrences
 * 3. Wildcard fallback - uses default template when no match found
 * 
 * Time estimation: Sum of all subtask durations + 20% buffer for transitions
 */
class TemplateEngine private constructor(
    private val templates: List<TaskTemplate>,
    private val defaultTemplate: DefaultTemplate
) {
    companion object {
        private const val ASSET_PATH = "templates/task_templates.json"
        private const val BUFFER_PERCENTAGE = 0.20 // 20% buffer for transitions
        private const val MINIMUM_MATCH_THRESHOLD = 25 // Minimum 25% keyword match
        
        /**
         * Creates a TemplateEngine instance by loading templates from assets.
         * Falls back to embedded JSON if asset loading fails.
         * This is a suspend function as it performs I/O operations.
         */
        suspend fun create(context: Context): TemplateEngine = withContext(Dispatchers.IO) {
            val jsonString = loadTemplatesFromAssets(context)
            val collection = TaskTemplate.parseCollection(jsonString)
                ?: TaskTemplate.parseCollection(TaskTemplate.FALLBACK_JSON)
                ?: createMinimalFallback()
            
            TemplateEngine(
                templates = collection.templates,
                defaultTemplate = collection.default_template
            )
        }
        
        /**
         * Attempts to load template JSON from assets folder.
         * Returns fallback JSON if asset not found or read fails.
         */
        private fun loadTemplatesFromAssets(context: Context): String {
            return runCatching {
                context.assets.open(ASSET_PATH).use { stream ->
                    stream.bufferedReader().readText()
                }
            }.getOrDefault(TaskTemplate.FALLBACK_JSON)
        }
        
        /**
         * Creates an absolute minimal fallback when even embedded JSON fails.
         * This should never happen but provides ultimate safety.
         */
        private fun createMinimalFallback(): TemplateCollection {
            return TemplateCollection(
                templates = emptyList(),
                default_template = DefaultTemplate(
                    subtasks = listOf(
                        "Break down the task (5 min)",
                        "Execute step by step (15 min)",
                        "Review completion (3 min)"
                    ),
                    icon = "checkmark.circle"
                )
            )
        }
    }
    
    /**
     * Main entry point: matches a task title to the best template.
     * Returns a MatchedTemplate containing subtasks and metadata.
     */
    fun matchTask(title: String): MatchedTemplate {
        val normalizedTitle = title.lowercase().trim()
        
        // Priority 1: Exact phrase match
        val exactMatch = findExactPhraseMatch(normalizedTitle)
        if (exactMatch != null) {
            return createMatchedTemplate(exactMatch, MatchType.EXACT)
        }
        
        // Priority 2: Keyword density scoring
        val densityMatch = findHighestDensityMatch(normalizedTitle)
        if (densityMatch != null && densityMatch.score >= MINIMUM_MATCH_THRESHOLD) {
            return createMatchedTemplate(densityMatch.template, MatchType.KEYWORD_DENSITY)
        }
        
        // Priority 3: Wildcard fallback
        return createDefaultTemplate()
    }
    
    /**
     * Priority 1: Checks for exact phrase matches in the title.
     * Looks for multi-word keyword phrases appearing in order.
     */
    private fun findExactPhraseMatch(title: String): TaskTemplate? {
        return templates.firstOrNull { template ->
            template.keywords.any { keyword ->
                // Check for exact phrase match (multi-word keywords)
                if (keyword.contains(" ")) {
                    title.contains(keyword.lowercase())
                } else {
                    // Single word - check as whole word using word boundaries
                    val wordRegex = "\\b${Regex.escape(keyword.lowercase())}\\b".toRegex()
                    wordRegex.containsMatchIn(title)
                }
            }
        }
    }
    
    /**
     * Priority 2: Calculates keyword density score for each template.
     * Score = (matched keywords / total keywords) * 100
     * Returns the template with highest score above threshold.
     */
    private fun findHighestDensityMatch(title: String): ScoredTemplate? {
        val words = title.split(WORD_DELIMITER).filter { it.length > 2 }
        
        return templates.map { template ->
            val matchedCount = template.keywords.count { keyword ->
                words.any { word ->
                    word == keyword.lowercase() || 
                    word.contains(keyword.lowercase()) ||
                    keyword.lowercase().contains(word)
                }
            }
            val score = (matchedCount * 100) / template.keywords.size.coerceAtLeast(1)
            ScoredTemplate(template, score, matchedCount)
        }.maxByOrNull { it.score }
    }
    
    /**
     * Creates a MatchedTemplate from a TaskTemplate with time estimation.
     */
    private fun createMatchedTemplate(
        template: TaskTemplate, 
        matchType: MatchType
    ): MatchedTemplate {
        val subtasks = template.subtasks.mapIndexed { index, templateString ->
            Subtask.fromTemplateString(templateString, index)
        }
        
        val baseMinutes = subtasks.sumOf { it.durationMinutes }
        val estimatedMinutes = (baseMinutes * (1 + BUFFER_PERCENTAGE)).toInt()
        
        return MatchedTemplate(
            templateId = template.id,
            subtasks = subtasks,
            icon = template.icon,
            estimatedMinutes = estimatedMinutes,
            baseMinutes = baseMinutes,
            matchType = matchType
        )
    }
    
    /**
     * Creates the default fallback template when no match is found.
     */
    private fun createDefaultTemplate(): MatchedTemplate {
        val subtasks = defaultTemplate.subtasks.mapIndexed { index, templateString ->
            Subtask.fromTemplateString(templateString, index)
        }
        
        val baseMinutes = subtasks.sumOf { it.durationMinutes }
        val estimatedMinutes = (baseMinutes * (1 + BUFFER_PERCENTAGE)).toInt()
        
        return MatchedTemplate(
            templateId = "default_${UUID.randomUUID().toString().take(8)}",
            subtasks = subtasks,
            icon = defaultTemplate.icon,
            estimatedMinutes = estimatedMinutes,
            baseMinutes = baseMinutes,
            matchType = MatchType.FALLBACK
        )
    }
    
    /**
     * Returns all available template IDs for UI display.
     */
    fun getAllTemplateIds(): List<String> = templates.map { it.id }
    
    /**
     * Returns a specific template by ID, or null if not found.
     */
    fun getTemplateById(id: String): TaskTemplate? = templates.find { it.id == id }
    
    private data class ScoredTemplate(
        val template: TaskTemplate,
        val score: Int,
        val matchedCount: Int
    )
    
    private val WORD_DELIMITER = Regex("[\\s\\p{Punct}]+")
}

/**
 * Represents the result of a template match operation.
 */
data class MatchedTemplate(
    val templateId: String,
    val subtasks: List<Subtask>,
    val icon: String,
    val estimatedMinutes: Int,
    val baseMinutes: Int,
    val matchType: MatchType
) {
    /**
     * Returns the 20% buffer amount added to base time.
     */
    fun getBufferMinutes(): Int = estimatedMinutes - baseMinutes
    
    /**
     * Formats estimated time as human-readable string.
     */
    fun getFormattedEstimate(): String {
        return when {
            estimatedMinutes < 60 -> "${estimatedMinutes} min"
            estimatedMinutes % 60 == 0 -> "${estimatedMinutes / 60} hr"
            else -> "${estimatedMinutes / 60} hr ${estimatedMinutes % 60} min"
        }
    }
}

/**
 * Indicates how the template was matched.
 */
enum class MatchType {
    EXACT,           // Exact phrase match found
    KEYWORD_DENSITY, // Matched based on keyword density score
    FALLBACK         // Used default template
}
