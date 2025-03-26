package com.tanasi.streamflix.providers

import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * UHDMoviesProvider implements the Provider interface for the UHD Movies website.
 * This provider offers high-quality movie and TV show content with various streaming options.
 *
 * Features:
 * - Latest movies and TV shows
 * - Web series collection
 * - 4K HDR content
 * - Multiple streaming servers
 * - Search functionality
 *
 * Known Limitations:
 * - Some content may require specific region access
 * - Server availability may vary
 * - Quality options depend on server selection
 */
object UHDMoviesProvider : Provider {

    override val name = "UHD Movies"
    override val logo = ""
    override val language = "en"

    /**
     * HTTP client configured with appropriate timeouts and settings
     * for reliable content fetching.
     */
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Base URL for the UHD Movies website.
     * This URL is used as the foundation for all API requests.
     */
    private val baseUrl = "https://uhdmovies.me"

    /**
     * HTTP headers required for making requests to the website.
     * These headers help mimic a regular browser request.
     */
    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.5",
        "Connection" to "keep-alive",
        "Upgrade-Insecure-Requests" to "1"
    )

    /**
     * Returns the main categories available on the website.
     * These categories are used for navigation and content organization.
     *
     * @return List of Category objects representing main sections
     */
    override suspend fun getHome(): List<Category> {
        return listOf(
            Category("Latest", emptyList()),
            Category("Web Series", emptyList()),
            Category("Movies", emptyList()),
            Category("4K HDR", emptyList())
        )
    }

    /**
     * Performs a search query on the website.
     *
     * @param query The search term to look for
     * @param page The page number for paginated results
     * @return List of search results as AppAdapter.Item objects
     */
    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        val url = if (page == 1) {
            "$baseUrl/search/$query/"
        } else {
            "$baseUrl/search/$query/page/$page/"
        }
        return getPosts(url).map { post ->
            if (post.link.contains("/movie/")) {
                Movie(
                    id = post.link.substringAfterLast("/"),
                    title = post.title,
                    poster = post.image,
                    overview = "",
                    released = null,
                    runtime = null,
                    trailer = null,
                    quality = null,
                    rating = null,
                    banner = null
                )
            } else {
                TvShow(
                    id = post.link.substringAfterLast("/"),
                    title = post.title,
                    poster = post.image,
                    overview = "",
                    released = null,
                    runtime = null,
                    trailer = null,
                    quality = null,
                    rating = null,
                    banner = null,
                    seasons = emptyList()
                )
            }
        }
    }

    /**
     * Retrieves a list of movies from the website.
     *
     * @param page The page number for paginated results
     * @return List of Movie objects
     */
    override suspend fun getMovies(page: Int): List<Movie> {
        val url = if (page == 1) {
            "$baseUrl/movies/"
        } else {
            "$baseUrl/movies/page/$page/"
        }
        return getPosts(url).map { post ->
            Movie(
                id = post.link.substringAfterLast("/"),
                title = post.title,
                poster = post.image,
                overview = "",
                released = null,
                runtime = null,
                trailer = null,
                quality = null,
                rating = null,
                banner = null
            )
        }
    }

    /**
     * Retrieves a list of TV shows from the website.
     *
     * @param page The page number for paginated results
     * @return List of TvShow objects
     */
    override suspend fun getTvShows(page: Int): List<TvShow> {
        val url = if (page == 1) {
            "$baseUrl/web-series/"
        } else {
            "$baseUrl/web-series/page/$page/"
        }
        return getPosts(url).map { post ->
            TvShow(
                id = post.link.substringAfterLast("/"),
                title = post.title,
                poster = post.image,
                overview = "",
                released = null,
                runtime = null,
                trailer = null,
                quality = null,
                rating = null,
                banner = null,
                seasons = emptyList()
            )
        }
    }

    /**
     * Retrieves detailed information about a specific movie.
     *
     * @param id The unique identifier of the movie
     * @return Movie object with detailed information
     */
    override suspend fun getMovie(id: String): Movie {
        val url = "$baseUrl/movie/$id/"
        val doc = getDocument(url)
        return Movie(
            id = id,
            title = doc.select("h1.entry-title").text(),
            poster = doc.select("img.attachment-post-thumbnail").attr("src"),
            overview = doc.select("div.entry-content").text(),
            released = null,
            runtime = null,
            trailer = null,
            quality = null,
            rating = null,
            banner = null
        )
    }

    /**
     * Retrieves detailed information about a specific TV show.
     *
     * @param id The unique identifier of the TV show
     * @return TvShow object with detailed information
     */
    override suspend fun getTvShow(id: String): TvShow {
        val url = "$baseUrl/web-series/$id/"
        val doc = getDocument(url)
        return TvShow(
            id = id,
            title = doc.select("h1.entry-title").text(),
            poster = doc.select("img.attachment-post-thumbnail").attr("src"),
            overview = doc.select("div.entry-content").text(),
            released = null,
            runtime = null,
            trailer = null,
            quality = null,
            rating = null,
            banner = null,
            seasons = emptyList()
        )
    }

    /**
     * Retrieves episodes for a specific season of a TV show.
     *
     * @param seasonId The unique identifier of the season
     * @return List of Episode objects
     */
    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val doc = getDocument(seasonId)
        return doc.select("div.episode-item").map { element ->
            Episode(
                id = element.attr("data-id"),
                number = element.select("span.episode-number").text().toIntOrNull() ?: 0,
                title = element.select("h3").text(),
                released = null,
                poster = element.select("img").attr("src"),
                tvShow = null,
                season = null
            )
        }
    }

    /**
     * Retrieves content for a specific genre.
     *
     * @param id The unique identifier of the genre
     * @param page The page number for paginated results
     * @return Genre object with associated content
     */
    override suspend fun getGenre(id: String, page: Int): Genre {
        val url = if (page == 1) {
            "$baseUrl/genre/$id/"
        } else {
            "$baseUrl/genre/$id/page/$page/"
        }
        val doc = getDocument(url)
        return Genre(
            id = id,
            name = doc.select("h1.entry-title").text(),
            shows = emptyList()
        )
    }

    /**
     * Retrieves information about a specific person (actor, director, etc.).
     * Note: This feature is not supported by the UHD Movies website.
     *
     * @param id The unique identifier of the person
     * @param page The page number for paginated results
     * @return People object with associated content
     */
    override suspend fun getPeople(id: String, page: Int): People {
        val url = if (page == 1) {
            "$baseUrl/people/$id/"
        } else {
            "$baseUrl/people/$id/page/$page/"
        }
        val doc = getDocument(url)
        return People(
            id = id,
            name = doc.select("h1.entry-title").text(),
            image = doc.select("img.attachment-post-thumbnail").attr("src"),
            biography = doc.select("div.entry-content").text(),
            placeOfBirth = null,
            birthday = null,
            deathday = null,
            filmography = emptyList()
        )
    }

    /**
     * Retrieves available streaming servers for a video.
     *
     * @param id The unique identifier of the video
     * @param videoType The type of video (movie or TV show)
     * @return List of Video.Server objects
     */
    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val url = when (videoType) {
            is Video.Type.Movie -> "$baseUrl/movie/$id/"
            is Video.Type.Episode -> "$baseUrl/web-series/${videoType.tvShow.id}/episode/${videoType.number}/"
        }
        val doc = getDocument(url)
        return doc.select("div.server-item").map { element ->
            Video.Server(
                id = element.attr("data-id"),
                name = element.select("span.server-name").text(),
                src = element.select("iframe").attr("src")
            )
        }
    }

    /**
     * Retrieves video information from a specific server.
     *
     * @param server The server to get video information from
     * @return Video object with streaming information
     */
    override suspend fun getVideo(server: Video.Server): Video {
        return Video(
            source = server.src,
            headers = headers,
            type = "video/mp4"
        )
    }

    /**
     * Helper function to get a Jsoup Document from a URL.
     *
     * @param url The URL to fetch the document from
     * @return Document object
     */
    private suspend fun getDocument(url: String): Document {
        val request = Request.Builder()
            .url(url)
            .headers(okhttp3.Headers.Builder().apply {
                headers.forEach { (key, value) -> add(key, value) }
            }.build())
            .build()

        val response = client.newCall(request).execute()
        return Jsoup.parse(response.body?.string() ?: "")
    }

    /**
     * Helper function to get posts from a URL.
     *
     * @param url The URL to fetch posts from
     * @return List of Post objects
     */
    private suspend fun getPosts(url: String): List<Post> {
        val doc = getDocument(url)
        return doc.select("article.post").map { element ->
            Post(
                title = element.select("h2.entry-title a").text(),
                link = element.select("h2.entry-title a").attr("href"),
                image = element.select("img.attachment-post-thumbnail").attr("src")
            )
        }
    }

    /**
     * Data class representing a post from the website.
     */
    private data class Post(
        val title: String,
        val link: String,
        val image: String
    )
} 