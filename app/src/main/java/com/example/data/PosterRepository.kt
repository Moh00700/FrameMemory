package com.example.data

import android.util.Log
import com.example.model.MediaCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val POSTER_TAG = "PosterRepository"

class PosterRepository {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, String>()

    suspend fun resolvePosterUrl(
        title: String,
        year: Int,
        mediaType: MediaCategory
    ): String {
        val key = "${mediaType.wireValue}|${title.trim().lowercase()}|$year"
        cache[key]?.let { return it }

        val resolved = withContext(Dispatchers.IO) {
            try {
                when (mediaType) {
                    MediaCategory.ANIME -> {
                        fetchFromJikan(title)
                            ?: fetchFromItunes(title, year, "movie")
                            ?: fetchFromWikipedia(title, "anime")
                            ?: ""
                    }
                    MediaCategory.TV -> {
                        fetchFromTvMaze(title)
                            ?: fetchFromItunes(title, year, "tvSeason")
                            ?: fetchFromWikipedia(title, "TV series")
                            ?: ""
                    }
                    MediaCategory.MOVIE -> {
                        fetchFromItunes(title, year, "movie")
                            ?: fetchFromWikipedia(title, "$year film")
                            ?: fetchFromWikipedia(title, "film")
                            ?: ""
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(POSTER_TAG, "Poster lookup failed for $title: ${e.message}")
                ""
            }
        }

        val sanitized = resolved.take(800)
        cache[key] = sanitized
        return sanitized
    }

    private fun fetchJson(url: String): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "FrameMemoryAndroid/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            return JSONObject(body)
        }
    }

    private fun fetchFromJikan(title: String): String? {
        return try {
            val encoded = URLEncoder.encode(title, "UTF-8")
            val json = fetchJson("https://api.jikan.moe/v4/anime?q=$encoded&limit=3") ?: return null
            val data = json.optJSONArray("data") ?: return null
            if (data.length() == 0) return null
            val first = data.getJSONObject(0)
            val images = first.optJSONObject("images") ?: return null
            val jpg = images.optJSONObject("jpg") ?: return null
            jpg.optString("large_image_url")
                .ifBlank { jpg.optString("image_url") }
                .takeIf { it.startsWith("https://") }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchFromTvMaze(title: String): String? {
        return try {
            val encoded = URLEncoder.encode(title, "UTF-8")
            val json = fetchJson("https://api.tvmaze.com/singlesearch/shows?q=$encoded") ?: return null
            val image = json.optJSONObject("image") ?: return null
            image.optString("original")
                .ifBlank { image.optString("medium") }
                .replace("http://", "https://")
                .takeIf { it.startsWith("https://") }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchFromItunes(title: String, year: Int, entity: String): String? {
        return try {
            val encoded = URLEncoder.encode(title, "UTF-8")
            val media = if (entity == "tvSeason") "tvShow" else "movie"
            val json = fetchJson(
                "https://itunes.apple.com/search?term=$encoded&media=$media&entity=$entity&limit=6"
            ) ?: return null
            val results = json.optJSONArray("results") ?: return null
            if (results.length() == 0) return null

            var bestUrl: String? = null
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val art = item.optString("artworkUrl100")
                if (art.isBlank()) continue
                val releaseDate = item.optString("releaseDate")
                val trackName = item.optString("trackName").ifBlank { item.optString("collectionName") }
                val highRes = art.replace("100x100bb", "600x900bb")
                    .replace("http://", "https://")

                val matchesYear = releaseDate.startsWith(year.toString())
                val matchesTitle = trackName.equals(title, ignoreCase = true)
                if (matchesTitle && matchesYear) {
                    return highRes
                }
                if (bestUrl == null && (matchesYear || trackName.contains(title, ignoreCase = true))) {
                    bestUrl = highRes
                }
                if (bestUrl == null) {
                    bestUrl = highRes
                }
            }
            bestUrl
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchFromWikipedia(title: String, qualifier: String): String? {
        return try {
            val query = URLEncoder.encode("$title ($qualifier)", "UTF-8")
            val searchJson = fetchJson(
                "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$query&format=json&srlimit=1"
            ) ?: return null
            val searchResults = searchJson.optJSONObject("query")?.optJSONArray("search") ?: return null
            val pageTitle = if (searchResults.length() > 0) {
                searchResults.getJSONObject(0).optString("title")
            } else {
                title
            }
            if (pageTitle.isBlank()) return null

            val encodedTitle = URLEncoder.encode(pageTitle.replace(" ", "_"), "UTF-8")
                .replace("+", "%20")
            val summary = fetchJson("https://en.wikipedia.org/api/rest_v1/page/summary/$encodedTitle")
                ?: return null
            summary.optJSONObject("originalimage")?.optString("source")
                ?.takeIf { it.startsWith("https://") }
                ?: summary.optJSONObject("thumbnail")?.optString("source")
                    ?.takeIf { it.startsWith("https://") }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
