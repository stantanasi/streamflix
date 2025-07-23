package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.*
import com.tanasi.streamflix.models.cuevanaeu.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.File
import java.util.concurrent.TimeUnit

object CuevanaEuProvider : Provider {

    override val name = "Cuevana 3"
    override val baseUrl = "https://www.cuevana3.eu"
    override val language = "es"

    private val client = getOkHttpClient()
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JsoupConverterFactory.create())
        .client(client)
        .build()
    private val service = retrofit.create(CuevanaEuService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private fun getOkHttpClient(): OkHttpClient {
        val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
        val clientBuilder = OkHttpClient.Builder()
            .cache(appCache)
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
        val dns = DnsOverHttps.Builder().client(clientBuilder.build())
            .url("https://1.1.1.1/dns-query".toHttpUrl())
            .build()
        return clientBuilder.dns(dns).build()
    }

    private interface CuevanaEuService {
        @GET
        suspend fun getPage(@Url url: String): Document
    }

    override suspend fun getHome(): List<Category> {
        return try {
            coroutineScope {
                val mainPageDeferred = async { service.getPage("$baseUrl/peliculas/estrenos/page/1") }
                val moviesWeeklyDeferred = async { service.getPage("$baseUrl/peliculas/tendencias/semana") }
                val seriesWeeklyDeferred = async { service.getPage("$baseUrl/series/tendencias/semana") }
                val seriesDailyDeferred = async { service.getPage("$baseUrl/series/tendencias/dia") }

                val mainDocument = mainPageDeferred.await()
                val mainScript = mainDocument.selectFirst("script#__NEXT_DATA__")?.data()
                val mainResponseJson = mainScript?.let { json.decodeFromString<ApiResponse>(it) }

                val bannerShows = mainResponseJson?.props?.pageProps?.movies?.map { movieData ->
                    TvShow(
                        id = "ver-pelicula/${movieData.slug?.name}",
                        title = movieData.titles?.name ?: "Sin Título",
                        banner = movieData.images?.backdrop?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                    )
                }?.filter { it.id != "ver-pelicula/null" } ?: emptyList()

                val homeMovies = mainResponseJson?.props?.pageProps?.movies?.map { movieData ->
                    Movie(
                        id = "ver-pelicula/${movieData.slug?.name}",
                        title = movieData.titles?.name ?: "",
                        poster = movieData.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                    )
                }?.filter { it.id != "ver-pelicula/null" } ?: emptyList()

                val categories = mutableListOf(
                    Category(name = Category.FEATURED, list = bannerShows),
                    Category(name = "Estrenos", list = homeMovies)
                )

                try {
                    val moviesWeeklyDocument = moviesWeeklyDeferred.await()
                    val moviesWeeklyScript = moviesWeeklyDocument.selectFirst("script#__NEXT_DATA__")?.data()
                    val moviesWeeklyJson = moviesWeeklyScript?.let { json.decodeFromString<ApiResponse>(it) }
                    val trendingMovies = moviesWeeklyJson?.props?.pageProps?.movies?.map { movieData ->
                        Movie(
                            id = "ver-pelicula/${movieData.slug?.name}",
                            title = movieData.titles?.name ?: "",
                            poster = movieData.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                        )
                    }?.filter { it.id != "ver-pelicula/null" } ?: emptyList()
                    categories.add(Category(name = "Películas - Tendencias de la Semana", list = trendingMovies))
                } catch (e: Exception) {}

                try {
                    val seriesWeeklyDocument = seriesWeeklyDeferred.await()
                    val seriesWeeklyScript = seriesWeeklyDocument.selectFirst("script#__NEXT_DATA__")?.data()
                    val seriesWeeklyJson = seriesWeeklyScript?.let { json.decodeFromString<ApiResponse>(it) }
                    val trendingSeriesWeekly = seriesWeeklyJson?.props?.pageProps?.movies?.map { seriesData ->
                        TvShow(
                            id = "ver-serie/${seriesData.slug?.name}",
                            title = seriesData.titles?.name ?: "",
                            poster = seriesData.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                        )
                    }?.filter { it.id != "ver-serie/null" } ?: emptyList()
                    categories.add(Category(name = "Series - Tendencias de la Semana", list = trendingSeriesWeekly))
                } catch (e: Exception) {}

                try {
                    val seriesDailyDocument = seriesDailyDeferred.await()
                    val seriesDailyScript = seriesDailyDocument.selectFirst("script#__NEXT_DATA__")?.data()
                    val seriesDailyJson = seriesDailyScript?.let { json.decodeFromString<ApiResponse>(it) }
                    val trendingSeriesDaily = seriesDailyJson?.props?.pageProps?.movies?.map { seriesData ->
                        TvShow(
                            id = "ver-serie/${seriesData.slug?.name}",
                            title = seriesData.titles?.name ?: "",
                            poster = seriesData.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                        )
                    }?.filter { it.id != "ver-serie/null" } ?: emptyList()
                    categories.add(Category(name = "Series - Tendencias del Día", list = trendingSeriesDaily))
                } catch (e: Exception) {}

                categories
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isBlank()) {
            return listOf(
                Genre("accion", "Acción"),
                Genre("aventura", "Aventura"),
                Genre("animacion", "Animación"),
                Genre("ciencia-ficcion", "Ciencia Ficción"),
                Genre("comedia", "Comedia"),
                Genre("crimen", "Crimen"),
                Genre("documental", "Documental"),
                Genre("drama", "Drama"),
                Genre("familia", "Familia"),
                Genre("fantasia", "Fantasía"),
                Genre("misterio", "Misterio"),
                Genre("romance", "Romance"),
                Genre("suspense", "Suspenso"),
                Genre("terror", "Terror"),
            )
        }
        if (page > 1) {
            return emptyList()
        }
        return try {
            val document = service.getPage("$baseUrl/search?q=$query")
            val script = document.selectFirst("script#__NEXT_DATA__")?.data() ?: return emptyList()
            val responseJson = json.decodeFromString<ApiResponse>(script)
            val results = responseJson.props?.pageProps?.movies?.mapNotNull { itemData ->
                val slugName = itemData.slug?.name
                if (slugName != null) {
                    if (itemData.url?.slug?.contains("pelicula") == true || itemData.url?.slug?.contains("movies") == true) {
                        Movie(
                            id = "ver-pelicula/$slugName",
                            title = itemData.titles?.name ?: "",
                            poster = itemData.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                        )
                    } else {
                        TvShow(
                            id = "ver-serie/$slugName",
                            title = itemData.titles?.name ?: "",
                            poster = itemData.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                        )
                    }
                } else {
                    null
                }
            } ?: emptyList()
            results.distinctBy { if (it is Movie) it.id else (it as TvShow).id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        return try {
            val document = service.getPage("$baseUrl/peliculas/estrenos/page/$page")
            val script = document.selectFirst("script#__NEXT_DATA__")?.data() ?: return emptyList()
            val responseJson = json.decodeFromString<ApiResponse>(script)
            responseJson.props?.pageProps?.movies?.map { movieData ->
                Movie(
                    id = "ver-pelicula/${movieData.slug?.name}",
                    title = movieData.titles?.name ?: "",
                    poster = movieData.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                )
            }?.filter { it.id != "ver-pelicula/null" } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        return try {
            val document = service.getPage("$baseUrl/series/estrenos/page/$page")
            val script = document.selectFirst("script#__NEXT_DATA__")?.data() ?: return emptyList()
            val responseJson = json.decodeFromString<ApiResponse>(script)
            responseJson.props?.pageProps?.movies?.map { seriesData ->
                TvShow(
                    id = "ver-serie/${seriesData.slug?.name}",
                    title = seriesData.titles?.name ?: "",
                    poster = seriesData.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                )
            }?.filter { it.id != "ver-serie/null" } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getPage("$baseUrl/$id")
        val script = document.selectFirst("script#__NEXT_DATA__")?.data()
            ?: throw Exception("No se encontró el script #__NEXT_DATA__")
        val responseJson = json.decodeFromString<ApiResponse>(script)
        val data = responseJson.props?.pageProps?.thisMovie
            ?: throw Exception("No se encontraron datos de la película en el JSON.")
        return Movie(
            id = id,
            title = data.titles?.name ?: "",
            overview = data.overview ?: "",
            released = data.releaseDate?.substringBefore("-"),
            poster = data.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" },
            banner = data.images?.backdrop?.let { if (it.startsWith("http")) it else "$baseUrl$it" },
            genres = data.genres?.map { genreData ->
                Genre(id = genreData.slug ?: "", name = genreData.name ?: "")
            } ?: emptyList(),
            cast = data.cast?.acting?.mapNotNull { actorData ->
                actorData.name?.let { People(id = "", name = it) }
            } ?: emptyList()
        )
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getPage("$baseUrl/$id")
        val script = document.selectFirst("script#__NEXT_DATA__")?.data()
            ?: throw Exception("No se encontró el script #__NEXT_DATA__")
        val responseJson = json.decodeFromString<ApiResponse>(script)
        val data = responseJson.props?.pageProps?.thisSerie
            ?: throw Exception("No se encontraron datos de la serie en el JSON.")
        val seasons = data.seasons?.map { seasonData ->
            Season(
                id = "${data.slug?.name}/${seasonData.number}",
                number = seasonData.number ?: 0,
                title = "Temporada ${seasonData.number}"
            )
        } ?: emptyList()
        return TvShow(
            id = id,
            title = data.titles?.name ?: "",
            overview = data.overview ?: "",
            released = data.releaseDate?.substringBefore("-"),
            poster = data.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" },
            banner = data.images?.backdrop?.let { if (it.startsWith("http")) it else "$baseUrl$it" },
            genres = data.genres?.map { genreData ->
                Genre(id = genreData.slug ?: "", name = genreData.name ?: "")
            } ?: emptyList(),
            cast = data.cast?.acting?.mapNotNull { actorData ->
                actorData.name?.let { People(id = "", name = it) }
            } ?: emptyList(),
            seasons = seasons
        )
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        return try {
            val showSlug = seasonId.substringBeforeLast("/")
            val seasonNumber = seasonId.substringAfterLast("/").toInt()
            val document = service.getPage("$baseUrl/ver-serie/$showSlug")
            val script = document.selectFirst("script#__NEXT_DATA__")?.data()
                ?: return emptyList()
            val responseJson = json.decodeFromString<ApiResponse>(script)
            val seasonsData = responseJson.props?.pageProps?.thisSerie?.seasons ?: return emptyList()
            val correctSeason = seasonsData.find { it.number == seasonNumber } ?: return emptyList()
            correctSeason.episodes.map { ep ->
                Episode(
                    id = "episodio/${ep.slug?.name}-temporada-${ep.slug?.season}-episodio-${ep.slug?.episode}",
                    number = ep.number ?: 0,
                    title = "Episodio ${ep.number}: ${ep.title}",
                    poster = ep.image?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        return try {
            val document = service.getPage("$baseUrl/$id")
            val script = document.selectFirst("script#__NEXT_DATA__")?.data() ?: return emptyList()
            val responseJson = json.decodeFromString<ApiResponse>(script)
            val videosJson = when (videoType) {
                is Video.Type.Movie -> responseJson.props?.pageProps?.thisMovie?.videos
                is Video.Type.Episode -> responseJson.props?.pageProps?.episode?.videos
            }
            val servers = mutableListOf<Video.Server>()
            suspend fun fetchServer(videoInfo: VideoInfo, lang: String) {
                try {
                    val embedPage = service.getPage(videoInfo.result!!)
                    val finalUrl = embedPage.select("script").firstOrNull {
                        it.data().contains("var url =")
                    }?.data()?.substringAfter("var url = '")?.substringBefore("'") ?: ""
                    if (finalUrl.isNotBlank()) {
                        servers.add(Video.Server(id = finalUrl, name = "${videoInfo.cyberlocker} [$lang]"))
                    }
                } catch (_: Exception) {}
            }
            videosJson?.latino?.forEach { fetchServer(it, "LAT") }
            videosJson?.spanish?.forEach { fetchServer(it, "CAST") }
            servers
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return Extractor.extract(server.id, server)
    }

    override val logo: String get() = "$baseUrl/favicon.ico"

    override suspend fun getGenre(id: String, page: Int): Genre {
        return try {
            val document = service.getPage("$baseUrl/genero/$id/page/$page")
            val script = document.selectFirst("script#__NEXT_DATA__")?.data()
                ?: throw Exception("No se encontró el script para el género.")
            val responseJson = json.decodeFromString<ApiResponse>(script)
            val shows = responseJson.props?.pageProps?.movies?.mapNotNull { itemData ->
                val slugName = itemData.slug?.name
                if (slugName != null) {
                    if (itemData.url?.slug?.contains("pelicula") == true || itemData.url?.slug?.contains("movies") == true) {
                        Movie(
                            id = "ver-pelicula/$slugName",
                            title = itemData.titles?.name ?: "",
                            poster = itemData.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                        )
                    } else {
                        TvShow(
                            id = "ver-serie/$slugName",
                            title = itemData.titles?.name ?: "",
                            poster = itemData.images?.poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                        )
                    }
                } else {
                    null
                }
            } ?: emptyList()
            Genre(id = id, name = id.replaceFirstChar { it.uppercaseChar() }, shows = shows)
        } catch (e: Exception) {
            Genre(id = id, name = id.replaceFirstChar { it.uppercaseChar() }, shows = emptyList())
        }
    }

    override suspend fun getPeople(id: String, page: Int): People {
        throw Exception("Esta función no está disponible en el proveedor Cuevana 3.")
    }
}