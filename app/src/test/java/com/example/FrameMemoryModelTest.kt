package com.example

import com.example.model.CountryFilterOption
import com.example.model.DecadeFilterOption
import com.example.model.LanguageFilterOption
import com.example.model.LanguageMode
import com.example.model.MediaCategory
import com.example.model.MediaFilterOption
import com.example.model.MemorySearchFilters
import com.example.model.ProductionStyle
import com.example.model.VoteChoice
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameMemoryModelTest {

    @Test
    fun `filters count and summary reflect active selections`() {
        val empty = MemorySearchFilters()
        assertEquals(0, empty.activeCount())
        assertEquals("All Movies, TV & Anime", empty.toSummary())

        val narrowed = MemorySearchFilters(
            media = MediaFilterOption.ANIME,
            decade = DecadeFilterOption.D2000S,
            country = CountryFilterOption.JAPAN,
            language = LanguageFilterOption.SUBTITLED
        )
        assertEquals(4, narrowed.activeCount())
        assertEquals("Anime • 2000s • Japan • Subtitled", narrowed.toSummary())
    }

    @Test
    fun `enum wire parsers fall back safely on unknown values`() {
        assertEquals(MediaCategory.ANIME, MediaCategory.fromWire("ANIME"))
        assertEquals(MediaCategory.MOVIE, MediaCategory.fromWire("UNKNOWN_VALUE"))
        assertEquals(ProductionStyle.ANIMATED, ProductionStyle.fromWire("animated"))
        assertEquals(ProductionStyle.LIVE_ACTION, ProductionStyle.fromWire(null))
        assertEquals(LanguageMode.DUBBED, LanguageMode.fromWire("DUBBED"))
        assertEquals(LanguageMode.ORIGINAL, LanguageMode.fromWire("??"))
        assertEquals(VoteChoice.YES, VoteChoice.fromWire("YES"))
        assertEquals(VoteChoice.NONE, VoteChoice.fromWire("INVALID"))
    }
}
