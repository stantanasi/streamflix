package com.tanasi.streamflix.providers

import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class VegaMoviesProviderIntegrationTest {
    private lateinit var provider: VegaMoviesProvider

    @Before
    fun setup() {
        provider = VegaMoviesProvider()
    }

    @Test
    fun `test getHome fetches real data`() = runBlocking {
        val categories = provider.getHome()
        
        assertTrue(categories.isNotEmpty())
        assertTrue(categories.all { it.name.isNotEmpty() })
        assertTrue(categories.all { it.list.isNotEmpty() })
        assertTrue(categories.all { it.itemType == AppAdapter.Type.CATEGORY })
    }

    @Test
    fun `test search fetches real data`() = runBlocking {
        val query = "avengers"
        val results = provider.search(query, 1)
        
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it is Movie || it is TvShow })
        assertTrue(results.all { it.title.contains(query, ignoreCase = true) })
    }

    @Test
    fun `test getMovies fetches real data`() = runBlocking {
        val movies = provider.getMovies(1)
        
        assertTrue(movies.isNotEmpty())
        assertTrue(movies.all { it is Movie })
        assertTrue(movies.all { it.title.isNotEmpty() })
        assertTrue(movies.all { it.poster.isNotEmpty() })
        assertTrue(movies.all { it.year > 0 })
        assertTrue(movies.all { it.rating >= 0f })
    }

    @Test
    fun `test getTvShows fetches real data`() = runBlocking {
        val shows = provider.getTvShows(1)
        
        assertTrue(shows.isNotEmpty())
        assertTrue(shows.all { it is TvShow })
        assertTrue(shows.all { it.title.isNotEmpty() })
        assertTrue(shows.all { it.poster.isNotEmpty() })
        assertTrue(shows.all { it.year > 0 })
        assertTrue(shows.all { it.rating >= 0f })
    }

    @Test
    fun `test getServers fetches real data`() = runBlocking {
        val movie = Video(
            id = "avengers-endgame",
            title = "Avengers: Endgame",
            poster = "https://example.com/poster.jpg",
            year = 2019,
            rating = 8.4f,
            duration = 181,
            description = "After the devastating events of Avengers: Infinity War, the universe is in ruins.",
            genres = listOf("Action", "Adventure", "Drama"),
            cast = listOf("Robert Downey Jr.", "Chris Evans", "Mark Ruffalo"),
            director = "Anthony Russo",
            writer = "Christopher Markus",
            country = "United States",
            language = "English",
            budget = "$356 million",
            revenue = "$2.798 billion",
            status = "Released",
            tagline = "Whatever it takes.",
            backdrop = "https://example.com/backdrop.jpg",
            trailer = "https://example.com/trailer.mp4",
            source = "",
            headers = null,
            type = "video/mp4"
        )
        
        val servers = provider.getServers(movie)
        assertTrue(servers.isNotEmpty())
        assertTrue(servers.all { it.id.isNotEmpty() })
        assertTrue(servers.all { it.name.isNotEmpty() })
    }

    @Test
    fun `test getVideo fetches real data`() = runBlocking {
        val video = Video(
            id = "avengers-endgame",
            title = "Avengers: Endgame",
            poster = "https://example.com/poster.jpg",
            year = 2019,
            rating = 8.4f,
            duration = 181,
            description = "After the devastating events of Avengers: Infinity War, the universe is in ruins.",
            genres = listOf("Action", "Adventure", "Drama"),
            cast = listOf("Robert Downey Jr.", "Chris Evans", "Mark Ruffalo"),
            director = "Anthony Russo",
            writer = "Christopher Markus",
            country = "United States",
            language = "English",
            budget = "$356 million",
            revenue = "$2.798 billion",
            status = "Released",
            tagline = "Whatever it takes.",
            backdrop = "https://example.com/backdrop.jpg",
            trailer = "https://example.com/trailer.mp4",
            source = "",
            headers = null,
            type = "video/mp4"
        )
        
        val server = Video.Server("server1", "Server 1")
        val updatedVideo = provider.getVideo(server, video)
        
        assertTrue(updatedVideo.source.isNotEmpty())
        assertTrue(updatedVideo.headers != null)
        assertTrue(updatedVideo.type == "video/mp4")
    }
} 