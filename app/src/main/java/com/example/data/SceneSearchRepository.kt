package com.example.data

import com.example.model.LanguageMode
import com.example.model.MediaCategory
import com.example.model.MemorySearchFilters
import com.example.model.ProductionStyle
import com.example.model.SceneCandidate
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

class SceneSearchRepository {

    private val candidateSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Official title of the movie, TV show, or anime"),
            "year" to Schema.integer(description = "Release year as a 4-digit integer"),
            "mediaType" to Schema.enumeration(
                values = listOf("MOVIE", "TV", "ANIME"),
                description = "Category of media"
            ),
            "country" to Schema.string(description = "Primary country of origin, e.g. USA, Japan, France"),
            "style" to Schema.enumeration(
                values = listOf("LIVE_ACTION", "ANIMATED"),
                description = "Whether it is live-action or animated"
            ),
            "languageMode" to Schema.enumeration(
                values = listOf("ORIGINAL", "SUBTITLED", "DUBBED"),
                description = "Most likely viewing mode matching the user's memory"
            ),
            "directorOrCreator" to Schema.string(description = "Director, showrunner, or animation studio"),
            "confidencePercent" to Schema.integer(description = "Match confidence from 1 to 100"),
            "whyItMatched" to Schema.string(
                description = "2-3 concise sentences explaining exactly which scene, episode, visual detail, or line matches the user's memory"
            ),
            "iconicCue" to Schema.string(
                description = "One short recognizable detail, character name, or quote from that scene to jog the user's memory"
            )
        )
    )

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "candidates" to Schema.array(
                items = candidateSchema,
                description = "Exactly 5 ranked candidate titles ordered from strongest match to plausible wildcard"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@SceneSearchRepository.responseSchema
                temperature = 0.4f
            }
        )
    }

    suspend fun findMatchingScenes(
        memoryQuery: String,
        filters: MemorySearchFilters,
        excludedTitles: List<String> = emptyList()
    ): List<SceneCandidate> {
        val cleanQuery = memoryQuery.trim()
        require(cleanQuery.isNotBlank()) { "Please describe a scene, character, quote, or visual detail." }

        val filterDirectives = buildList {
            filters.media.promptHint?.let { add("- Media type constraint: $it") }
            filters.decade.promptHint?.let { add("- Release decade constraint: $it") }
            filters.country.promptHint?.let { add("- Country/region constraint: $it") }
            filters.style.promptHint?.let { add("- Format constraint: $it") }
            filters.language.promptHint?.let { add("- Audio/subtitles clue: $it") }
            if (excludedTitles.isNotEmpty()) {
                add("- Do NOT suggest any of these already-ruled-out titles: ${excludedTitles.joinToString(", ")}")
            }
        }

        val prompt = buildString {
            appendLine("You are Frame Memory, an expert cinema, television, and anime archivist who identifies titles from vague, half-remembered human scene descriptions.")
            appendLine("Remembered scene description:")
            appendLine("\"$cleanQuery\"")
            appendLine()
            if (filterDirectives.isNotEmpty()) {
                appendLine("Strict narrowing clues from the user:")
                filterDirectives.forEach { appendLine(it) }
                appendLine()
            } else {
                appendLine("Search across movies, TV series, and anime worldwide without format restrictions.")
                appendLine()
            }
            appendLine("Return exactly 5 ranked candidates in JSON. Rank #1 should be the strongest match, followed by #2-#5 plausible alternatives (accounting for misremembered colors, paraphrased quotes, or similar scenes).")
            appendLine("Make 'whyItMatched' specific to the user's clues (characters, objects, setting, episode if TV/anime, dialogue vibe). Keep each 'whyItMatched' concise and spoiler-considerate where possible.")
        }

        val response = withTimeout(60_000L) {
            model.generateContent(prompt)
        }

        val jsonText = response.text?.trim()
            ?: throw IllegalStateException("Gemini returned an empty response.")

        val root = JSONObject(jsonText)
        val array = root.getJSONArray("candidates")
        val results = mutableListOf<SceneCandidate>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val title = obj.optString("title").trim()
            if (title.isBlank()) continue
            results.add(
                SceneCandidate(
                    candidateId = UUID.randomUUID().toString(),
                    title = title,
                    year = obj.optInt("year", 2000).coerceIn(1880, 2035),
                    mediaType = MediaCategory.fromWire(obj.optString("mediaType")),
                    country = obj.optString("country", "Unknown").ifBlank { "Unknown" },
                    style = ProductionStyle.fromWire(obj.optString("style")),
                    languageMode = LanguageMode.fromWire(obj.optString("languageMode")),
                    directorOrCreator = obj.optString("directorOrCreator", "").trim(),
                    confidencePercent = obj.optInt("confidencePercent", 75).coerceIn(1, 99),
                    whyItMatched = obj.optString("whyItMatched", "").ifBlank { "Matches key visual and story cues." },
                    iconicCue = obj.optString("iconicCue", "").trim(),
                    posterUrl = "",
                    isPosterLoading = true
                )
            )
        }

        if (results.isEmpty()) {
            throw IllegalStateException("No matching titles could be parsed. Try adding one more clue.")
        }

        return results.take(5)
    }
}
