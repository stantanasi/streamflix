package com.tanasi.streamflix.providers

import com.tanasi.streamflix.models.Video
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.Ignore

@Ignore("Integration tests require network access and may be slow")
class UHDMoviesProviderIntegrationTest {

    private lateinit var provider: UHDMoviesProvider

    @Before
    fun setup() {
        provider = UHDMoviesProvider
    }

    @Test
    fun `test getHome returns valid categories`() = runBlocking {
        val categories = provider.getHome()
        
        assertNotNull(categories)
        assertTrue(categories.isNotEmpty())
        assertTrue(categories.any { it.name == "Latest" })
        assertTrue(categories.any { it.name == "Movies" })
    }

    @Test
    fun `test search returns results`() = runBlocking {
        val results = provider.search("test", 1)
        
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `test getMovies returns valid movies`() = runBlocking {
        val movies = provider.getMovies(1)
        
        assertNotNull(movies)
        assertTrue(movies.isNotEmpty())
        
        val firstMovie = movies.first()
        assertNotNull(firstMovie.id)
        assertNotNull(firstMovie.title)
        assertNotNull(firstMovie.poster)
    }

    @Test
    fun `test getTvShows returns valid shows`() = runBlocking {
        val shows = provider.getTvShows(1)
        
        assertNotNull(shows)
        assertTrue(shows.isNotEmpty())
        
        val firstShow = shows.first()
        assertNotNull(firstShow.id)
        assertNotNull(firstShow.title)
        assertNotNull(firstShow.poster)
    }

    @Test
    fun `test getMovie returns valid movie details`() = runBlocking {
        // Get a movie ID from the movies list
        val movies = provider.getMovies(1)
        assertTrue(movies.isNotEmpty())
        
        val movieId = movies.first().id
        val movie = provider.getMovie(movieId)
        
        assertNotNull(movie)
        assertEquals(movieId, movie.id)
        assertNotNull(movie.title)
        assertNotNull(movie.poster)
    }

    @Test
    fun `test getServers returns valid servers`() = runBlocking {
        // Get a movie ID from the movies list
        val movies = provider.getMovies(1)
        assertTrue(movies.isNotEmpty())
        
        val movieId = movies.first().id
        val movieType = Video.Type.Movie(
            id = movieId,
            title = movies.first().title,
            releaseDate = "",
            poster = movies.first().poster ?: ""
        )
        val servers = provider.getServers(movieId, movieType)
        
        assertNotNull(servers)
        assertTrue(servers.isNotEmpty())
        
        val firstServer = servers.first()
        assertNotNull(firstServer.name)
        assertNotNull(firstServer.src)
    }

    @Test
    fun `test getVideo returns valid video`() = runBlocking {
        // Get a movie ID and server from the movies list
        val movies = provider.getMovies(1)
        assertTrue(movies.isNotEmpty())
        
        val movieId = movies.first().id
        val movieType = Video.Type.Movie(
            id = movieId,
            title = movies.first().title,
            releaseDate = "",
            poster = movies.first().poster ?: ""
        )
        val servers = provider.getServers(movieId, movieType)
        assertTrue(servers.isNotEmpty())
        
        val server = servers.first()
        val video = provider.getVideo(server)
        
        assertNotNull(video)
        assertNotNull(video.source)
        assertEquals("video/mp4", video.type)
    }

    @Test
    fun `test getGenre returns valid genre content`() = runBlocking {
        val genre = provider.getGenre("/movies/english-movies", 1)
        
        assertNotNull(genre)
        assertEquals("/movies/english-movies", genre.id)
        assertNotNull(genre.name)
        assertNotNull(genre.shows)
        assertTrue(genre.shows.isNotEmpty())
    }

    @Test
    fun `test error handling for invalid movie ID`() = runBlocking {
        try {
            provider.getMovie("invalid-id")
            fail("Expected an exception for invalid movie ID")
        } catch (e: Exception) {
            // Expected exception
            assertTrue(e.message?.contains("404") == true || e.message?.contains("not found") == true)
        }
    }

    @Test
    fun `test error handling for invalid server URL`() = runBlocking {
        val invalidServer = Video.Server("invalid-id", "Invalid Server", "invalid-url")
        try {
            provider.getVideo(invalidServer)
            fail("Expected an exception for invalid server URL")
        } catch (e: Exception) {
            // Expected exception
            assertTrue(e.message?.contains("failed") == true || e.message?.contains("error") == true)
        }
    }
} 