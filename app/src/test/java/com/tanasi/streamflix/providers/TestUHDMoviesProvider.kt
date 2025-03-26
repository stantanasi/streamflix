package com.tanasi.streamflix.providers

import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import org.jsoup.nodes.Document

class TestUHDMoviesProvider(private val mockDocument: Document) : Provider {

    override val name = "UHD Movies"
    override val logo = ""
    override val language = "en"

    override suspend fun getHome(): List<Category> {
        return listOf(
            Category("Latest", emptyList()).apply {
                selectedIndex = 0
                itemSpacing = 0
                itemType = AppAdapter.Type.CATEGORY_MOBILE_ITEM
            },
            Category("Web Series", emptyList()).apply {
                selectedIndex = 0
                itemSpacing = 0
                itemType = AppAdapter.Type.CATEGORY_MOBILE_ITEM
            },
            Category("Movies", emptyList()).apply {
                selectedIndex = 0
                itemSpacing = 0
                itemType = AppAdapter.Type.CATEGORY_MOBILE_ITEM
            },
            Category("4K HDR", emptyList()).apply {
                selectedIndex = 0
                itemSpacing = 0
                itemType = AppAdapter.Type.CATEGORY_MOBILE_ITEM
            }
        )
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        return getPosts().map { post ->
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

    override suspend fun getMovies(page: Int): List<Movie> {
        return getPosts().map { post ->
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

    override suspend fun getTvShows(page: Int): List<TvShow> {
        return getPosts().map { post ->
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

    override suspend fun getMovie(id: String): Movie {
        return Movie(
            id = id,
            title = mockDocument.select("h1.entry-title").text(),
            poster = mockDocument.select("img.attachment-post-thumbnail").attr("src"),
            overview = mockDocument.select("div.entry-content").text(),
            released = null,
            runtime = null,
            trailer = null,
            quality = null,
            rating = null,
            banner = null
        )
    }

    override suspend fun getTvShow(id: String): TvShow {
        return TvShow(
            id = id,
            title = mockDocument.select("h1.entry-title").text(),
            poster = mockDocument.select("img.attachment-post-thumbnail").attr("src"),
            overview = mockDocument.select("div.entry-content").text(),
            released = null,
            runtime = null,
            trailer = null,
            quality = null,
            rating = null,
            banner = null,
            seasons = emptyList()
        )
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        return mockDocument.select("div.episode-list a").map { element ->
            Episode(
                id = element.attr("href").substringAfterLast("/"),
                number = 0,
                title = element.text(),
                released = null,
                poster = "",
                tvShow = null,
                season = null
            )
        }
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        return Genre(
            id = id,
            name = mockDocument.select("h1.entry-title").text(),
            shows = getPosts().map { post ->
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
        )
    }

    override suspend fun getPeople(id: String, page: Int): People {
        return People(
            id = id,
            name = "",
            image = "",
            biography = "",
            placeOfBirth = null,
            birthday = null,
            deathday = null,
            filmography = emptyList()
        )
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        return mockDocument.select("div.download-links a").map { element ->
            Video.Server(
                id = element.attr("href").substringAfterLast("/"),
                name = element.text(),
                src = element.attr("href")
            )
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return Video(
            source = server.src,
            headers = emptyMap(),
            type = "video/mp4"
        )
    }

    private suspend fun getPosts(): List<Post> {
        return mockDocument.select(".gridlove-posts .layout-masonry").map { element ->
            Post(
                title = element.select("a").attr("title").replace("Download", "").trim(),
                link = element.select("a").attr("href"),
                image = element.select("img").attr("src")
            )
        }
    }

    private data class Post(
        val title: String,
        val link: String,
        val image: String
    )
} 