package com.tanasi.streamflix.providers

import com.tanasi.streamflix.models.*
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.utils.JsUnpacker
import com.tanasi.streamflix.utils.Extensions
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import android.util.Log
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class VegaMoviesProvider : Provider {
    companion object {
        private const val TAG = "VegaMoviesProvider"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://vegamovies.band"
    private val headers = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        .add("Accept-Language", "en-US,en;q=0.5")
        .add("Connection", "keep-alive")
        .build()

    override suspend fun getHome(): List<Category> {
        return try {
            Log.d(TAG, "Fetching home categories")
            val categories = listOf(
                Category(
                    name = "Latest",
                    list = getPosts("$baseUrl/latest-movies/"),
                    selectedIndex = 0,
                    itemSpacing = 0,
                    itemType = AppAdapter.Type.CATEGORY
                ),
                Category(
                    name = "Web Series",
                    list = getPosts("$baseUrl/web-series/"),
                    selectedIndex = 0,
                    itemSpacing = 0,
                    itemType = AppAdapter.Type.CATEGORY
                ),
                Category(
                    name = "Movies",
                    list = getPosts("$baseUrl/movies/"),
                    selectedIndex = 0,
                    itemSpacing = 0,
                    itemType = AppAdapter.Type.CATEGORY
                ),
                Category(
                    name = "4K HDR",
                    list = getPosts("$baseUrl/4k-hdr/"),
                    selectedIndex = 0,
                    itemSpacing = 0,
                    itemType = AppAdapter.Type.CATEGORY
                )
            )
            Log.d(TAG, "Successfully fetched ${categories.size} categories")
            categories
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch home categories", e)
            emptyList()
        }
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        return try {
            Log.d(TAG, "Searching for: $query, page: $page")
            if (query.isBlank()) {
                Log.w(TAG, "Empty search query provided")
                return emptyList()
            }

            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = if (page == 1) {
                "$baseUrl/search/$encodedQuery/"
            } else {
                "$baseUrl/search/$encodedQuery/page/$page/"
            }
            val results = getPosts(url)
            Log.d(TAG, "Found ${results.size} results for query: $query")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for query: $query", e)
            emptyList()
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        return try {
            Log.d(TAG, "Fetching movies page: $page")
            if (page < 1) {
                Log.w(TAG, "Invalid page number: $page")
                return emptyList()
            }

            val url = if (page == 1) {
                "$baseUrl/movies/"
            } else {
                "$baseUrl/movies/page/$page/"
            }
            val movies = getPosts(url).filterIsInstance<Movie>()
            Log.d(TAG, "Found ${movies.size} movies on page $page")
            movies
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch movies page: $page", e)
            emptyList()
        }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        return try {
            Log.d(TAG, "Fetching TV shows page: $page")
            if (page < 1) {
                Log.w(TAG, "Invalid page number: $page")
                return emptyList()
            }

            val url = if (page == 1) {
                "$baseUrl/web-series/"
            } else {
                "$baseUrl/web-series/page/$page/"
            }
            val shows = getPosts(url).filterIsInstance<TvShow>()
            Log.d(TAG, "Found ${shows.size} TV shows on page $page")
            shows
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch TV shows page: $page", e)
            emptyList()
        }
    }

    override suspend fun getPeople(page: Int): List<People> {
        return emptyList() // VegaMovies doesn't have people/actors section
    }

    override suspend fun getServers(video: Video): List<Video.Server> {
        return try {
            Log.d(TAG, "Fetching servers for video: ${video.id}")
            if (video.id.isBlank()) {
                Log.w(TAG, "Invalid video ID")
                return emptyList()
            }

            val url = when (video.type) {
                Video.Type.Movie -> "$baseUrl/movie/${video.id}/"
                Video.Type.Episode -> "$baseUrl/episode/${video.id}/"
                else -> {
                    Log.w(TAG, "Unsupported video type: ${video.type}")
                    return emptyList()
                }
            }
            
            val doc = getDocument(url)
            val servers = mutableListOf<Video.Server>()
            
            doc.select("div.server-list div.server-item").forEach { server ->
                val serverId = server.attr("data-id")
                val serverName = server.select("span.server-name").text()
                if (serverId.isNotEmpty() && serverName.isNotEmpty()) {
                    servers.add(Video.Server(serverId, serverName))
                } else {
                    Log.w(TAG, "Invalid server data: id=$serverId, name=$serverName")
                }
            }
            
            Log.d(TAG, "Found ${servers.size} servers for video: ${video.id}")
            servers
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch servers for video: ${video.id}", e)
            emptyList()
        }
    }

    override suspend fun getVideo(server: Video.Server, video: Video): Video {
        return try {
            Log.d(TAG, "Fetching video details for server: ${server.id}, video: ${video.id}")
            if (video.id.isBlank() || server.id.isBlank()) {
                Log.w(TAG, "Invalid video or server ID")
                return video
            }

            val url = when (video.type) {
                Video.Type.Movie -> "$baseUrl/movie/${video.id}/"
                Video.Type.Episode -> "$baseUrl/episode/${video.id}/"
                else -> {
                    Log.w(TAG, "Unsupported video type: ${video.type}")
                    return video
                }
            }
            
            val doc = getDocument(url)
            val serverElement = doc.select("div.server-list div.server-item[data-id=${server.id}]").first()
            
            if (serverElement != null) {
                val videoUrl = serverElement.attr("data-url")
                if (videoUrl.isNotEmpty()) {
                    Log.d(TAG, "Successfully fetched video URL for server: ${server.id}")
                    return video.copy(
                        source = videoUrl,
                        headers = headers,
                        type = "video/mp4"
                    )
                } else {
                    Log.w(TAG, "Empty video URL for server: ${server.id}")
                }
            } else {
                Log.w(TAG, "Server not found: ${server.id}")
            }
            
            video
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch video details", e)
            video
        }
    }

    private suspend fun getPosts(url: String): List<AppAdapter.Item> {
        val doc = getDocument(url)
        val items = mutableListOf<AppAdapter.Item>()
        
        doc.select("div.movie-list div.movie-item").forEach { item ->
            try {
                val link = item.select("a").attr("href")
                val title = item.select("h3.title").text()
                val poster = item.select("img").attr("src")
                val year = item.select("span.year").text().toIntOrNull() ?: 0
                val rating = item.select("span.rating").text().toFloatOrNull() ?: 0f
                
                if (link.isNotEmpty() && title.isNotEmpty()) {
                    val absoluteLink = if (link.startsWith("http")) link else "$baseUrl$link"
                    val absolutePoster = if (poster.startsWith("http")) poster else "$baseUrl$poster"
                    val id = absoluteLink.substringAfterLast("/").substringBefore("/")
                    
                    if (link.contains("/web-series/")) {
                        items.add(
                            TvShow(
                                id = id,
                                title = title,
                                poster = absolutePoster,
                                year = year,
                                rating = rating,
                                seasons = emptyList()
                            )
                        )
                    } else {
                        items.add(
                            Movie(
                                id = id,
                                title = title,
                                poster = absolutePoster,
                                year = year,
                                rating = rating,
                                duration = 0,
                                description = "",
                                genres = emptyList(),
                                cast = emptyList(),
                                director = "",
                                writer = "",
                                country = "",
                                language = "",
                                budget = "",
                                revenue = "",
                                status = "",
                                tagline = "",
                                backdrop = "",
                                trailer = ""
                            )
                        )
                    }
                } else {
                    Log.w(TAG, "Invalid item data: link=$link, title=$title")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse item", e)
            }
        }
        
        return items
    }

    private suspend fun getDocument(url: String): Document {
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount < MAX_RETRIES) {
            try {
                Log.d(TAG, "Fetching URL: $url (attempt ${retryCount + 1})")
                val request = Request.Builder()
                    .url(url)
                    .headers(headers)
                    .build()
                    
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errorMsg = "Failed to fetch URL: $url, Status: ${response.code}"
                    Log.e(TAG, errorMsg)
                    throw IOException(errorMsg)
                }
                
                val html = response.body?.string() ?: throw IOException("Empty response body")
                response.close()
                
                return Jsoup.parse(html)
            } catch (e: Exception) {
                lastException = e
                retryCount++
                when (e) {
                    is SocketTimeoutException -> Log.w(TAG, "Timeout on attempt $retryCount", e)
                    is UnknownHostException -> Log.e(TAG, "Network error on attempt $retryCount", e)
                    else -> Log.e(TAG, "Error on attempt $retryCount", e)
                }
                if (retryCount < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS)
                }
            }
        }
        
        throw lastException ?: IOException("Failed to fetch URL after $MAX_RETRIES attempts")
    }
} 
