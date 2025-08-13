package com.tanasi.streamflix.providers

import android.util.Base64
import android.util.Log
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.Show
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

object PelisplustoProvider : Provider {

    override val name = "Pelisplusto"
    override val baseUrl = "https://pelisplus.to"
    override val language = "es"
    override val logo = "https://pelisplus.to/images/logo2.png"
    private const val TAG = "PelisplustoProvider"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                .build()
            chain.proceed(request)
        }
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private val service = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JsoupConverterFactory.create())
        .client(client)
        .build()
        .create(PelisplustoService::class.java)

    private interface PelisplustoService {
        @GET
        suspend fun getPage(@Url url: String): Document
    }

    override suspend fun getHome(): List<Category> = coroutineScope {
        val categories = mutableListOf<Category>()

        val mainPageDeferred = async { service.getPage(baseUrl) }
        val moviesDeferred = async { service.getPage("$baseUrl/peliculas") }
        val seriesDeferred = async { service.getPage("$baseUrl/series") }
        val animesDeferred = async { service.getPage("$baseUrl/animes") }

        try {
            val mainDocument = mainPageDeferred.await()

            val bannerShows = mainDocument.select("div.home__slider_index div.swiper-slide article").mapNotNull {
                val url = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null

                // La imagen del banner está en el 'div.bg' dentro de cada 'article'
                val banner = it.selectFirst("div.bg")?.attr("style")
                    ?.substringAfter("url(")?.substringBefore(")")
                    ?.removeSurrounding("'")?.removeSurrounding("\"") ?: return@mapNotNull null

                val title = it.selectFirst("h2")?.text()?.substringBefore(" (") ?: return@mapNotNull null
                val id = url.substringAfterLast('/').removeSuffix("/")

                when {
                    url.contains("/pelicula/") -> Movie(id = id, title = title, banner = getAbsoluteUrl(banner))
                    url.contains("/serie/") -> TvShow(id = id, title = title, banner = getAbsoluteUrl(banner))
                    url.contains("/anime/") -> TvShow(id = "anime/$id", title = title, banner = getAbsoluteUrl(banner))
                    else -> null
                }
            }
            if (bannerShows.isNotEmpty()) {
                categories.add(Category(Category.FEATURED, bannerShows))
            }
        } catch (e: Exception) { Log.e(TAG, "getHome (banners): ${e.message}") }

        try {
            val movies = parseShows(moviesDeferred.await()).filterIsInstance<Movie>()
            if (movies.isNotEmpty()) categories.add(Category("Películas", movies))
        } catch (e: Exception) { Log.e(TAG, "getHome (movies): ${e.message}") }

        try {
            val series = parseShows(seriesDeferred.await()).filterIsInstance<TvShow>()
            if (series.isNotEmpty()) categories.add(Category("Series", series))
        } catch (e: Exception) { Log.e(TAG, "getHome (series): ${e.message}") }

        try {
            val animes = parseShows(animesDeferred.await()).filterIsInstance<TvShow>()
            if (animes.isNotEmpty()) categories.add(Category("Animes", animes))
        } catch (e: Exception) { Log.e(TAG, "getHome (animes): ${e.message}") }

        categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isBlank()) {
            return listOf(
                Genre("genero/accion", "Acción"),
                Genre("genero/animacion", "Animación"),
                Genre("genero/anime", "Anime"),
                Genre("genero/aventura", "Aventura"),
                Genre("genero/belica", "Bélica"),
                Genre("genero/ciencia-ficcion", "Ciencia ficción"),
                Genre("genero/comedia", "Comedia"),
                Genre("genero/crimen", "Crimen"),
                Genre("genero/documental", "Documental"),
                Genre("genero/drama", "Drama"),
                Genre("genero/familia", "Familia"),
                Genre("genero/fantasia", "Fantasía"),
                Genre("genero/guerra", "Guerra"),
                Genre("genero/historia", "Historia"),
                Genre("genero/misterio", "Misterio"),
                Genre("genero/musica", "Música"),
                Genre("genero/romance", "Romance"),
                Genre("genero/suspense", "Suspenso"),
                Genre("genero/terror", "Terror")
            )
        }

        if (page > 1) {
            return emptyList()
        }

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search/$encodedQuery"
        val document = service.getPage(url)
        return parseShows(document)
    }

    private fun parseShows(document: Document): List<AppAdapter.Item> {
        val elements = document.select("article.item.liste.relative a.itemA")

        return elements.mapNotNull {
            val url = it.attr("href")
            val posterUrl = it.selectFirst("img")?.attr("data-src") ?: ""
            val title = it.selectFirst("h2")?.text()?.substringBefore(" (") ?: return@mapNotNull null

            when {
                url.contains("/pelicula/") -> Movie(
                    id = url.substringAfter("/pelicula/").removeSuffix("/"),
                    title = title,
                    poster = posterUrl
                )
                url.contains("/serie/") -> TvShow(
                    id = url.substringAfter("/serie/").removeSuffix("/"),
                    title = title,
                    poster = posterUrl
                )
                url.contains("/anime/") -> TvShow(
                    id = "anime/${url.substringAfter("/anime/").removeSuffix("/")}",
                    title = title,
                    poster = posterUrl
                )
                else -> null
            }
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val url = if (page == 1) "$baseUrl/peliculas" else "$baseUrl/peliculas/$page"
        val document = service.getPage(url)
        return parseShows(document).filterIsInstance<Movie>()
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val url = if (page == 1) "$baseUrl/series" else "$baseUrl/series/$page"
        val document = service.getPage(url)
        return parseShows(document).filterIsInstance<TvShow>()
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val url = if (page == 1) "$baseUrl/$id" else "$baseUrl/$id/page/$page"
        val document = service.getPage(url)
        val shows = parseShows(document).filterIsInstance<Show>()
        val genreName = id.substringAfter("genero/").replaceFirstChar { it.uppercase() }
        return Genre(id = id, name = genreName, shows = shows)
    }

    private fun getAbsoluteUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        val cleanUrl = url.removePrefix("background-image: url(\"").removeSuffix("\");")
        return if (cleanUrl.startsWith("http")) {
            cleanUrl
        } else {
            "$baseUrl$cleanUrl"
        }
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getPage("$baseUrl/pelicula/$id")
        val info = document.selectFirst("div.genres.rating")
        val posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content")
        val bannerUrl = document.selectFirst("div.bg")?.attr("style")
            ?.substringAfter("url(")?.substringBefore(")")
            ?.removeSurrounding("'")?.removeSurrounding("\"")

        return Movie(
            id = id,
            title = document.selectFirst("h1.slugh1")?.text()?.substringBefore(" (") ?: "",
            overview = document.selectFirst("div.description p")?.text(),
            poster = getAbsoluteUrl(posterUrl),
            banner = getAbsoluteUrl(bannerUrl),
            rating = info?.select("span")?.find { it.text().contains("Rating:") }?.text()?.substringAfter(":")?.trim()?.toDoubleOrNull(),
            released = info?.selectFirst("a")?.text(),
            genres = document.select("div.genres")
                .find { it.selectFirst("span b")?.text() == "Generos" }
                ?.select("a")?.map {
                    Genre(id = it.attr("href"), name = it.text())
                } ?: emptyList()
        )
    }

    override suspend fun getTvShow(id: String): TvShow {
        val url = if (id.startsWith("anime/")) "$baseUrl/$id" else "$baseUrl/serie/$id"
        val document = service.getPage(url)
        val info = document.selectFirst("div.genres.rating")

        val posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content")
        val bannerUrl = document.selectFirst("div.bg")?.attr("style")
            ?.substringAfter("url(")?.substringBefore(")")
            ?.removeSurrounding("'")?.removeSurrounding("\"")

        val script = document.select("script").find { it.data().contains("seasonsJson") }?.data() ?: ""
        val json = script.substringAfter("const seasonsJson = ").substringBefore(";")
        val seasonsData = JSONObject(json)

        return TvShow(
            id = id,
            title = document.selectFirst("h1.slugh1")?.text()?.substringBefore(" (") ?: "",
            overview = document.selectFirst("div.description p")?.text(),
            poster = getAbsoluteUrl(posterUrl),
            banner = getAbsoluteUrl(bannerUrl),
            rating = info?.select("span")?.find { it.text().contains("Rating:") }?.text()?.substringAfter(":")?.trim()?.toDoubleOrNull(),
            released = info?.selectFirst("a")?.text(),
            genres = document.select("div.genres")
                .find { it.selectFirst("span b")?.text() == "Generos" }
                ?.select("a")?.map {
                    Genre(id = it.attr("href"), name = it.text())
                } ?: emptyList(),
            seasons = seasonsData.keys().asSequence().map {
                val seasonNumber = it.toIntOrNull() ?: 0
                Season(
                    id = "$id/$seasonNumber",
                    number = seasonNumber,
                    title = "Temporada $seasonNumber"
                )
            }.sortedByDescending { it.number }.toList()
        )
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val lastSlashIndex = seasonId.lastIndexOf('/')
        if (lastSlashIndex == -1) return emptyList()

        val showId = seasonId.substring(0, lastSlashIndex)
        val seasonNumber = seasonId.substring(lastSlashIndex + 1)

        val url = if (showId.startsWith("anime/")) "$baseUrl/$showId" else "$baseUrl/serie/$showId"

        try {
            val document = service.getPage(url)
            val script = document.select("script").find { it.data().contains("seasonsJson") }?.data() ?: ""
            val json = script.substringAfter("const seasonsJson = ").substringBefore(";")
            val seasonsData = JSONObject(json)
            val episodesData = seasonsData.getJSONArray(seasonNumber)

            return List(episodesData.length()) { i ->
                val episodeData = episodesData.getJSONObject(i)
                Episode(
                    id = "$seasonId/${episodeData.getInt("episode")}",
                    number = episodeData.getInt("episode"),
                    title = episodeData.getString("title"),
                    poster = "https://image.tmdb.org/t/p/w300${episodeData.getString("image")}"
                )
            }.sortedBy { it.number }
        } catch (e: Exception) {
            Log.e(TAG, "getEpisodesBySeason falló: ${e.message}")
            return emptyList()
        }
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val url = when (videoType) {
            is Video.Type.Movie -> "$baseUrl/pelicula/${videoType.id}"
            is Video.Type.Episode -> {
                val showId = videoType.tvShow.id
                val season = videoType.season.number
                val episode = videoType.number
                val basePath = if (showId.startsWith("anime/")) "$baseUrl/$showId" else "$baseUrl/serie/$showId"
                "$basePath/season/$season/episode/$episode"
            }
        }

        try {
            val document = service.getPage(url)
            val serverElements = document.select(".bg-tabs ul li")

            val servers = mutableListOf<Video.Server>()
            for (li in serverElements) {
                try {
                    val serverName = li.text().replace(" Reproducir", "")
                    val dataServer = li.attr("data-server")
                    if (dataServer.isEmpty()) continue

                    val decodedUrl = String(Base64.decode(dataServer, Base64.DEFAULT))

                    val finalUrl = if (!decodedUrl.contains("https://")) {
                        val reEncoded = String(Base64.encode(dataServer.toByteArray(), Base64.DEFAULT)).trim()
                        val playerUrl = "$baseUrl/player/$reEncoded"
                        val playerDoc = service.getPage(playerUrl)

                        playerDoc.selectFirst("script:containsData(window.onload)")?.data()
                            ?.let { Regex("""(https?://[^\s'"]+)""").find(it)?.groupValues?.get(1) }
                    } else {
                        decodedUrl
                    }

                    if (!finalUrl.isNullOrEmpty()) {
                        servers.add(Video.Server(id = finalUrl, name = serverName, src = finalUrl))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fallo procesando un servidor individual: ${e.message}")
                }
                delay(1500L)
            }
            return servers

        } catch (e: Exception) {
            Log.e(TAG, "Fallo crítico en getServers: ${e.message}")
            return emptyList()
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return Extractor.extract(server.src, server)
    }

    override suspend fun getPeople(id: String, page: Int): People {
        throw Exception("Not yet implemented")
    }
}