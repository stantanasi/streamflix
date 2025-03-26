package com.tanasi.streamflix.providers

import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File

class UHDMoviesProviderTest {

    private lateinit var provider: TestUHDMoviesProvider
    private lateinit var mockDocument: Document

    @Before
    fun setup() {
        // Load mock HTML from test resources
        val html = File("src/test/resources/uhd_movies.html").readText()
        mockDocument = Jsoup.parse(html)
        provider = TestUHDMoviesProvider(mockDocument)
    }

    @Test
    fun `test provider name and language`() {
        assertEquals("UHD Movies", provider.name)
        assertEquals("en", provider.language)
    }

    @Test
    fun `test getHome returns correct categories`() = runTest {
        val categories = provider.getHome()
        
        assertEquals(4, categories.size)
        
        val expectedCategories = listOf(
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
        
        assertTrue(categories.containsAll(expectedCategories))
        assertTrue(expectedCategories.containsAll(categories))
    }

    @Test
    fun `test getMovies returns correct format`() = runBlocking {
        val movies = provider.getMovies(1)
        
        assertNotNull(movies)
        assertTrue(movies.isNotEmpty())
        
        val firstMovie = movies.first()
        assertNotNull(firstMovie.id)
        assertNotNull(firstMovie.title)
        assertNotNull(firstMovie.poster)
        assertEquals("", firstMovie.overview)
        assertNull(firstMovie.released)
        assertNull(firstMovie.runtime)
        assertNull(firstMovie.trailer)
        assertNull(firstMovie.quality)
        assertNull(firstMovie.rating)
        assertNull(firstMovie.banner)
    }

    @Test
    fun `test getTvShows returns correct format`() = runBlocking {
        val shows = provider.getTvShows(1)
        
        assertNotNull(shows)
        assertTrue(shows.isNotEmpty())
        
        val firstShow = shows.first()
        assertNotNull(firstShow.id)
        assertNotNull(firstShow.title)
        assertNotNull(firstShow.poster)
        assertEquals("", firstShow.overview)
        assertNull(firstShow.released)
        assertNull(firstShow.runtime)
        assertNull(firstShow.trailer)
        assertNull(firstShow.quality)
        assertNull(firstShow.rating)
        assertNull(firstShow.banner)
        assertTrue(firstShow.seasons.isEmpty())
    }

    @Test
    fun `test search returns correct format`() = runBlocking {
        val results = provider.search("test", 1)
        
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        
        val firstResult = results.first()
        assertNotNull(firstResult)
    }

    @Test
    fun `test getServers returns correct format`() = runBlocking {
        val movieType = Video.Type.Movie(
            id = "test-id",
            title = "Test Movie",
            releaseDate = "",
            poster = ""
        )
        val servers = provider.getServers("test-id", movieType)
        
        assertNotNull(servers)
        assertTrue(servers.isNotEmpty())
        
        val firstServer = servers.first()
        assertNotNull(firstServer.name)
        assertNotNull(firstServer.src)
    }

    @Test
    fun `test getVideo returns correct format`() = runBlocking {
        val server = Video.Server("test-id", "Test Server", "http://test.com")
        val video = provider.getVideo(server)
        
        assertNotNull(video)
        assertNotNull(video.source)
        assertEquals("video/mp4", video.type)
        assertNotNull(video.headers)
    }

    @Test
    fun `test getGenre returns correct format`() = runBlocking {
        val genre = provider.getGenre("/movies/english-movies", 1)
        
        assertNotNull(genre)
        assertEquals("/movies/english-movies", genre.id)
        assertNotNull(genre.name)
        assertNotNull(genre.shows)
    }

    @Test
    fun `test getPeople returns empty result`() = runBlocking {
        val people = provider.getPeople("test-id", 1)
        
        assertNotNull(people)
        assertEquals("test-id", people.id)
        assertEquals("", people.name)
        assertEquals("", people.image)
        assertEquals("", people.biography)
        assertNull(people.placeOfBirth)
        assertNull(people.birthday)
        assertNull(people.deathday)
        assertTrue(people.filmography.isEmpty())
    }
} 