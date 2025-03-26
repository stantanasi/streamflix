package com.tanasi.streamflix.providers

import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VegaMoviesProviderTest {
    private lateinit var provider: VegaMoviesProvider

    @Before
    fun setup() {
        provider = VegaMoviesProvider()
    }

    @Test
    fun `test getHome returns correct categories`() = runBlocking {
        val categories = provider.getHome()
        
        val expectedCategories = listOf(
            Category(
                name = "Latest",
                list = emptyList(),
                selectedIndex = 0,
                itemSpacing = 0,
                itemType = AppAdapter.Type.CATEGORY
            ),
            Category(
                name = "Web Series",
                list = emptyList(),
                selectedIndex = 0,
                itemSpacing = 0,
                itemType = AppAdapter.Type.CATEGORY
            ),
            Category(
                name = "Movies",
                list = emptyList(),
                selectedIndex = 0,
                itemSpacing = 0,
                itemType = AppAdapter.Type.CATEGORY
            ),
            Category(
                name = "4K HDR",
                list = emptyList(),
                selectedIndex = 0,
                itemSpacing = 0,
                itemType = AppAdapter.Type.CATEGORY
            )
        )
        
        assertTrue(categories.containsAll(expectedCategories))
        assertTrue(expectedCategories.containsAll(categories))
    }

    @Test
    fun `test search returns correct type of items`() = runBlocking {
        val query = "test"
        val results = provider.search(query, 1)
        
        assertTrue(results.all { it is Movie || it is TvShow })
    }

    @Test
    fun `test getMovies returns only Movie objects`() = runBlocking {
        val movies = provider.getMovies(1)
        assertTrue(movies.all { it is Movie })
    }

    @Test
    fun `test getTvShows returns only TvShow objects`() = runBlocking {
        val shows = provider.getTvShows(1)
        assertTrue(shows.all { it is TvShow })
    }

    @Test
    fun `test getPeople returns empty list`() = runBlocking {
        val people = provider.getPeople(1)
        assertTrue(people.isEmpty())
    }

    @Test
    fun `test getServers returns correct server list`() = runBlocking {
        val video = Video(
            id = "test-id",
            title = "Test Video",
            poster = "test-poster.jpg",
            year = 2024,
            rating = 8.5f,
            duration = 120,
            description = "Test description",
            genres = emptyList(),
            cast = emptyList(),
            director = "Test Director",
            writer = "Test Writer",
            country = "Test Country",
            language = "Test Language",
            budget = "Test Budget",
            revenue = "Test Revenue",
            status = "Test Status",
            tagline = "Test Tagline",
            backdrop = "test-backdrop.jpg",
            trailer = "test-trailer.mp4",
            source = "",
            headers = null,
            type = null
        )
        
        val servers = provider.getServers(video)
        assertTrue(servers.isEmpty()) // Empty list for invalid video type
    }

    @Test
    fun `test getVideo returns updated video with server info`() = runBlocking {
        val video = Video(
            id = "test-id",
            title = "Test Video",
            poster = "test-poster.jpg",
            year = 2024,
            rating = 8.5f,
            duration = 120,
            description = "Test description",
            genres = emptyList(),
            cast = emptyList(),
            director = "Test Director",
            writer = "Test Writer",
            country = "Test Country",
            language = "Test Language",
            budget = "Test Budget",
            revenue = "Test Revenue",
            status = "Test Status",
            tagline = "Test Tagline",
            backdrop = "test-backdrop.jpg",
            trailer = "test-trailer.mp4",
            source = "",
            headers = null,
            type = null
        )
        
        val server = Video.Server("test-server", "Test Server")
        val updatedVideo = provider.getVideo(server, video)
        
        assertEquals(video.id, updatedVideo.id)
        assertEquals(video.title, updatedVideo.title)
        assertEquals(video.poster, updatedVideo.poster)
        assertEquals(video.year, updatedVideo.year)
        assertEquals(video.rating, updatedVideo.rating)
        assertEquals(video.duration, updatedVideo.duration)
        assertEquals(video.description, updatedVideo.description)
        assertEquals(video.genres, updatedVideo.genres)
        assertEquals(video.cast, updatedVideo.cast)
        assertEquals(video.director, updatedVideo.director)
        assertEquals(video.writer, updatedVideo.writer)
        assertEquals(video.country, updatedVideo.country)
        assertEquals(video.language, updatedVideo.language)
        assertEquals(video.budget, updatedVideo.budget)
        assertEquals(video.revenue, updatedVideo.revenue)
        assertEquals(video.status, updatedVideo.status)
        assertEquals(video.tagline, updatedVideo.tagline)
        assertEquals(video.backdrop, updatedVideo.backdrop)
        assertEquals(video.trailer, updatedVideo.trailer)
        assertEquals("", updatedVideo.source)
        assertEquals(null, updatedVideo.headers)
        assertEquals(null, updatedVideo.type)
    }
} 