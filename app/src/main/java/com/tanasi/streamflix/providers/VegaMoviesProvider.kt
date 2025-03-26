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
import java.util.concurrent.TimeUnit

class VegaMoviesProvider : Provider {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://vegamovies.xyz"
    private val headers = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        .add("Accept-Language", "en-US,en;q=0.5")
        .add("Connection", "keep-alive")
        .build()

    override suspend fun getHome(): List<Category> {
        return listOf(
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
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        val url = if (page == 1) {
            "$baseUrl/search/$query/"
        } else {
            "$baseUrl/search/$query/page/$page/"
        }
        return getPosts(url)
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val url = if (page == 1) {
            "$baseUrl/movies/"
        } else {
            "$baseUrl/movies/page/$page/"
        }
        return getPosts(url).filterIsInstance<Movie>()
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val url = if (page == 1) {
            "$baseUrl/web-series/"
        } else {
            "$baseUrl/web-series/page/$page/"
        }
        return getPosts(url).filterIsInstance<TvShow>()
    }

    override suspend fun getPeople(page: Int): List<People> {
        return emptyList() // VegaMovies doesn't have people/actors section
    }

    override suspend fun getServers(video: Video): List<Video.Server> {
        val url = when (video.type) {
            is Video.Type.Movie -> "$baseUrl/movie/${video.id}/"
            is Video.Type.Episode -> "$baseUrl/episode/${video.id}/"
            else -> return emptyList()
        }
        
        val doc = getDocument(url)
        val servers = mutableListOf<Video.Server>()
        
        doc.select("div.server-list div.server-item").forEach { server ->
            val serverId = server.attr("data-id")
            val serverName = server.select("span.server-name").text()
            servers.add(Video.Server(serverId, serverName))
        }
        
        return servers
    }

    override suspend fun getVideo(server: Video.Server, video: Video): Video {
        val url = when (video.type) {
            is Video.Type.Movie -> "$baseUrl/movie/${video.id}/"
            is Video.Type.Episode -> "$baseUrl/episode/${video.id}/"
            else -> return video
        }
        
        val doc = getDocument(url)
        val serverElement = doc.select("div.server-list div.server-item[data-id=${server.id}]").first()
        
        if (serverElement != null) {
            val videoUrl = serverElement.attr("data-url")
            return video.copy(
                source = videoUrl,
                headers = headers,
                type = "video/mp4"
            )
        }
        
        return video
    }

    private suspend fun getPosts(url: String): List<AppAdapter.Item> {
        val doc = getDocument(url)
        val items = mutableListOf<AppAdapter.Item>()
        
        doc.select("div.movie-list div.movie-item").forEach { item ->
            val link = item.select("a").attr("href")
            val title = item.select("h3.title").text()
            val poster = item.select("img").attr("src")
            val year = item.select("span.year").text().toIntOrNull() ?: 0
            val rating = item.select("span.rating").text().toFloatOrNull() ?: 0f
            
            if (link.contains("/web-series/")) {
                items.add(
                    TvShow(
                        id = link.substringAfterLast("/").substringBefore("/"),
                        title = title,
                        poster = poster,
                        year = year,
                        rating = rating,
                        seasons = emptyList()
                    )
                )
            } else {
                items.add(
                    Movie(
                        id = link.substringAfterLast("/").substringBefore("/"),
                        title = title,
                        poster = poster,
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
        }
        
        return items
    }

    private suspend fun getDocument(url: String): Document {
        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .build()
            
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: throw Exception("Failed to fetch URL: $url")
        
        return Jsoup.parse(html)
    }
} 