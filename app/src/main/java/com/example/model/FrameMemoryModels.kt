package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MediaCategory(val wireValue: String, val label: String) {
    MOVIE("MOVIE", "Movie"),
    TV("TV", "TV Series"),
    ANIME("ANIME", "Anime");

    companion object {
        fun fromWire(raw: String?): MediaCategory =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) } ?: MOVIE
    }
}

enum class ProductionStyle(val wireValue: String, val label: String) {
    LIVE_ACTION("LIVE_ACTION", "Live-Action"),
    ANIMATED("ANIMATED", "Animated");

    companion object {
        fun fromWire(raw: String?): ProductionStyle =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) } ?: LIVE_ACTION
    }
}

enum class LanguageMode(val wireValue: String, val label: String) {
    ORIGINAL("ORIGINAL", "Original Audio"),
    SUBTITLED("SUBTITLED", "Subtitled"),
    DUBBED("DUBBED", "Dubbed");

    companion object {
        fun fromWire(raw: String?): LanguageMode =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) } ?: ORIGINAL
    }
}

enum class VoteChoice(val wireValue: String) {
    YES("YES"),
    NO("NO"),
    NONE("NONE");

    companion object {
        fun fromWire(raw: String?): VoteChoice =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) } ?: NONE
    }
}

enum class MediaFilterOption(val label: String, val promptHint: String?) {
    ALL("All Formats", null),
    MOVIE("Movies", "Feature film / Movie only"),
    TV("TV Series", "TV show / series only"),
    ANIME("Anime", "Japanese anime series or anime film only")
}

enum class DecadeFilterOption(val label: String, val promptHint: String?) {
    ANY("Any Era", null),
    D2020S("2020s", "Released in the 2020s (2020–present)"),
    D2010S("2010s", "Released in the 2010s (2010–2019)"),
    D2000S("2000s", "Released in the 2000s (2000–2009)"),
    D1990S("1990s", "Released in the 1990s (1990–1999)"),
    D1980S("1980s", "Released in the 1980s (1980–1989)"),
    CLASSIC("Pre-1980", "Released before 1980")
}

enum class CountryFilterOption(val label: String, val promptHint: String?) {
    ANY("Any Country", null),
    US_UK("USA / UK", "United States, UK, or English-language cinema"),
    JAPAN("Japan", "Japan"),
    KOREA("South Korea", "South Korea"),
    EUROPE("Europe", "European cinema (France, Germany, Spain, Nordic, Italy, etc.)"),
    INDIA("India", "India (Bollywood / regional Indian cinema)"),
    LATAM("Latin America", "Mexico, Brazil, Argentina, or Latin American cinema")
}

enum class StyleFilterOption(val label: String, val promptHint: String?) {
    ANY("Any Style", null),
    LIVE_ACTION("Live-Action", "Live-action only (not animated)"),
    ANIMATED("Animated", "Animated (2D, 3D, stop-motion, or anime)")
}

enum class LanguageFilterOption(val label: String, val promptHint: String?) {
    ANY("Any Audio", null),
    ORIGINAL("Original Audio", "Watched in original native language"),
    SUBTITLED("Subtitled", "Foreign-language title typically watched with subtitles"),
    DUBBED("Dubbed", "Commonly aired or watched with an English/localized dub")
}

data class MemorySearchFilters(
    val media: MediaFilterOption = MediaFilterOption.ALL,
    val decade: DecadeFilterOption = DecadeFilterOption.ANY,
    val country: CountryFilterOption = CountryFilterOption.ANY,
    val style: StyleFilterOption = StyleFilterOption.ANY,
    val language: LanguageFilterOption = LanguageFilterOption.ANY
) {
    fun activeCount(): Int {
        var count = 0
        if (media != MediaFilterOption.ALL) count++
        if (decade != DecadeFilterOption.ANY) count++
        if (country != CountryFilterOption.ANY) count++
        if (style != StyleFilterOption.ANY) count++
        if (language != LanguageFilterOption.ANY) count++
        return count
    }

    fun toSummary(): String {
        val parts = buildList {
            if (media != MediaFilterOption.ALL) add(media.label)
            if (decade != DecadeFilterOption.ANY) add(decade.label)
            if (country != CountryFilterOption.ANY) add(country.label)
            if (style != StyleFilterOption.ANY) add(style.label)
            if (language != LanguageFilterOption.ANY) add(language.label)
        }
        return if (parts.isEmpty()) "All Movies, TV & Anime" else parts.joinToString(" • ").take(200)
    }
}

data class SceneCandidate(
    val candidateId: String,
    val title: String,
    val year: Int,
    val mediaType: MediaCategory,
    val country: String,
    val style: ProductionStyle,
    val languageMode: LanguageMode,
    val directorOrCreator: String,
    val confidencePercent: Int,
    val whyItMatched: String,
    val iconicCue: String,
    val posterUrl: String = "",
    val isPosterLoading: Boolean = true,
    val isDismissed: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class SolvedMemory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val queryText: String = "",
    val filterSummary: String = "",
    val title: String = "",
    val year: Long = 2000L,
    val mediaType: String = MediaCategory.MOVIE.wireValue,
    val country: String = "",
    val style: String = ProductionStyle.LIVE_ACTION.wireValue,
    val languageMode: String = LanguageMode.ORIGINAL.wireValue,
    val directorOrCreator: String = "",
    val whyItMatched: String = "",
    val iconicCue: String = "",
    val posterUrl: String = "",
    val personalNote: String = "",
    val sharedToCommunity: Boolean = false,
    val communityMemoryId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun mediaEnum(): MediaCategory = MediaCategory.fromWire(mediaType)
    fun styleEnum(): ProductionStyle = ProductionStyle.fromWire(style)
    fun languageEnum(): LanguageMode = LanguageMode.fromWire(languageMode)
}

@Keep
@IgnoreExtraProperties
data class CommunityMemory(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val status: String = "PUBLISHED",
    val queryText: String = "",
    val filterSummary: String = "",
    val title: String = "",
    val year: Long = 2000L,
    val mediaType: String = MediaCategory.MOVIE.wireValue,
    val country: String = "",
    val style: String = ProductionStyle.LIVE_ACTION.wireValue,
    val languageMode: String = LanguageMode.ORIGINAL.wireValue,
    val directorOrCreator: String = "",
    val whyItMatched: String = "",
    val iconicCue: String = "",
    val posterUrl: String = "",
    val yesCount: Long = 0L,
    val noCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun mediaEnum(): MediaCategory = MediaCategory.fromWire(mediaType)
    fun styleEnum(): ProductionStyle = ProductionStyle.fromWire(style)
    fun languageEnum(): LanguageMode = LanguageFilterHelper.fromWire(languageMode)
}

private object LanguageFilterHelper {
    fun fromWire(raw: String?): LanguageMode = LanguageMode.fromWire(raw)
}

@Keep
@IgnoreExtraProperties
data class CommunityVoteRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memoryId: String = "",
    val vote: String = VoteChoice.NONE.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun voteEnum(): VoteChoice = VoteChoice.fromWire(vote)
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Searching : SearchUiState
    data class Results(
        val queryText: String,
        val filterSummary: String,
        val candidates: List<SceneCandidate>
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
