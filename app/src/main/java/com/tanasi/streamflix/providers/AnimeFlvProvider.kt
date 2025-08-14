package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.*
import com.tanasi.streamflix.models.animeflv.ServerModel
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
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.File
import java.util.concurrent.TimeUnit

object AnimeFlvProvider : Provider {

    override val name = "AnimeFLV"
    override val baseUrl = "https://www3.animeflv.net"
    override val language = "es"
    override val logo = "https://www3.animeflv.net/assets/animeflv/img/logo.png"

    private val client = getOkHttpClient()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JsoupConverterFactory.create())
        .client(client)
        .build()

    private val service = retrofit.create(AnimeFlvService::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

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

    private interface AnimeFlvService {
        @GET
        suspend fun getPage(@Url url: String): Document

        @GET("browse")
        suspend fun search(@Query("q") query: String, @Query("page") page: Int): Document

        @GET("browse")
        suspend fun getTvShows(@Query("order") order: String = "rating", @Query("page") page: Int): Document

        @GET("browse")
        suspend fun getMovies(@Query("type[]") type: String = "movie", @Query("page") page: Int): Document

        @GET("browse")
        suspend fun getGenre(@Query("genre[]") genre: String, @Query("page") page: Int): Document

        @GET("anime/{id}")
        suspend fun getShowDetails(@Path("id") id: String): Document
    }

    override suspend fun getHome(): List<Category> {
        return try {
            coroutineScope {
                val homeDeferred = async { service.getPage(baseUrl) }
                val addedDeferred = async { service.getPage("$baseUrl/browse?order=added&page=1") }
                val airingDeferred = async { service.getPage("$baseUrl/browse?status[]=1&page=1") }

                val categories = mutableListOf<Category>()

                try {
                    val addedDocument = addedDeferred.await()
                    val bannerShows = addedDocument.select("ul.ListAnimes li article").mapNotNull { element ->
                        val url = element.selectFirst("div.Description a.Button")?.attr("href") ?: return@mapNotNull null
                        val posterUrl = element.selectFirst("a div.Image figure img")?.attr("src")
                        val finalPoster = if (posterUrl?.startsWith("http") == true) posterUrl else posterUrl?.let { "$baseUrl$it" }

                        TvShow(
                            id = url.substringAfterLast("/"),
                            title = element.selectFirst("a h3")?.text() ?: "",
                            banner = finalPoster
                        )
                    }
                    if (bannerShows.isNotEmpty()) {
                        categories.add(Category(Category.FEATURED, bannerShows))
                    }
                } catch (e: Exception) { /* No-op */ }

                try {
                    val homeDocument = homeDeferred.await()
                    val latestEpisodes = homeDocument.select("ul.ListEpisodios li")
                        .mapNotNull { element ->
                            val linkElement = element.selectFirst("a") ?: return@mapNotNull null
                            val showUrl = linkElement.attr("href")
                                .replace("/ver/", "/anime/")
                                .substringBeforeLast("-")

                            if (showUrl.isBlank()) return@mapNotNull null
                            val imageUrl = element.selectFirst("span.Image img")?.attr("src")
                            TvShow(
                                id = showUrl.substringAfterLast("/"),
                                title = element.selectFirst("strong.Title")?.text() ?: "",
                                poster = imageUrl?.let { "$baseUrl$it" }?.replace("thumbs", "covers")
                            )
                        }
                        .distinctBy { it.id }
                    if (latestEpisodes.isNotEmpty()) {
                        categories.add(Category("Últimos Episodios", latestEpisodes))
                    }
                } catch (e: Exception) { /* No-op */ }

                try {
                    val airingDocument = airingDeferred.await()
                    val airingShows = airingDocument.select("ul.ListAnimes li article").mapNotNull { element ->
                        val url = element.selectFirst("div.Description a.Button")?.attr("href") ?: return@mapNotNull null
                        val posterUrl = element.selectFirst("a div.Image figure img")?.attr("src")
                        val finalPoster = if (posterUrl?.startsWith("http") == true) posterUrl else posterUrl?.let { "$baseUrl$it" }

                        TvShow(
                            id = url.substringAfterLast("/"),
                            title = element.selectFirst("a h3")?.text() ?: "",
                            poster = finalPoster
                        )
                    }
                    if (airingShows.isNotEmpty()) {
                        categories.add(Category("Animes en Emisión", airingShows))
                    }
                } catch (e: Exception) { /* No-op */ }

                return@coroutineScope categories
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isBlank()) {
            return listOf(
                Genre("accion", "Acción"),
                Genre("artes-marciales", "Artes Marciales"),
                Genre("aventura", "Aventuras"),
                Genre("carreras", "Carreras"),
                Genre("ciencia-ficcion", "Ciencia Ficción"),
                Genre("comedia", "Comedia"),
                Genre("demencia", "Demencia"),
                Genre("demonios", "Demonios"),
                Genre("deportes", "Deportes"),
                Genre("drama", "Drama"),
                Genre("ecchi", "Ecchi"),
                Genre("escolares", "Escolares"),
                Genre("espacial", "Espacial"),
                Genre("fantasia", "Fantasía"),
                Genre("harem", "Harem"),
                Genre("historico", "Histórico"),
                Genre("infantil", "Infantil"),
                Genre("josei", "Josei"),
                Genre("juegos", "Juegos"),
                Genre("magia", "Magia"),
                Genre("mecha", "Mecha"),
                Genre("militar", "Militar"),
                Genre("misterio", "Misterio"),
                Genre("musica", "Música"),
                Genre("parodia", "Parodia"),
                Genre("policia", "Policía"),
                Genre("psicologico", "Psicológico"),
                Genre("recuentos-de-la-vida", "Recuentos de la vida"),
                Genre("romance", "Romance"),
                Genre("samurai", "Samurai"),
                Genre("seinen", "Seinen"),
                Genre("shoujo", "Shojo"),
                Genre("shounen", "Shounen"),
                Genre("sobrenatural", "Sobrenatural"),
                Genre("superpoderes", "Superpoderes"),
                Genre("suspenso", "Suspenso"),
                Genre("terror", "Terror"),
                Genre("vampiros", "Vampiros"),
                Genre("yaoi", "Yaoi"),
                Genre("yuri", "Yuri")
            )
        }

        return try {
            if (page > 1) return emptyList()

            val document = service.search(query, page)

            document.select("ul.ListAnimes li article").mapNotNull { element ->
                val url = element.selectFirst("div.Description a.Button")?.attr("href") ?: return@mapNotNull null
                val id = url.substringAfterLast("/")
                val title = element.selectFirst("a h3")?.text() ?: ""
                val posterUrl = element.selectFirst("a div.Image figure img")?.attr("src")
                val type = element.selectFirst("span.Type")?.text()

                val finalPoster = if (posterUrl?.startsWith("http") == true) {
                    posterUrl
                } else {
                    posterUrl?.let { "$baseUrl$it" }
                }

                if (type == "Película") {
                    Movie(id = id, title = title, poster = finalPoster)
                } else {
                    TvShow(id = id, title = title, poster = finalPoster)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        return try {
            val document = service.getTvShows(page = page)
            document.select("ul.ListAnimes li article").mapNotNull { element ->
                val url = element.selectFirst("div.Description a.Button")?.attr("href") ?: return@mapNotNull null
                val posterUrl = element.selectFirst("a div.Image figure img")?.attr("src")
                val finalPoster = if (posterUrl?.startsWith("http") == true) posterUrl else posterUrl?.let { "$baseUrl$it" }

                TvShow(
                    id = url.substringAfterLast("/"),
                    title = element.selectFirst("a h3")?.text() ?: "",
                    poster = finalPoster
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        return try {
            val document = service.getMovies(page = page)
            document.select("ul.ListAnimes li article").mapNotNull { element ->
                val url = element.selectFirst("div.Description a.Button")?.attr("href") ?: return@mapNotNull null
                val posterUrl = element.selectFirst("a div.Image figure img")?.attr("src")
                val finalPoster = if (posterUrl?.startsWith("http") == true) posterUrl else posterUrl?.let { "$baseUrl$it" }

                Movie(
                    id = url.substringAfterLast("/"),
                    title = element.selectFirst("a h3")?.text() ?: "",
                    poster = finalPoster
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTvShow(id: String): TvShow {
        return try {
            val document = service.getShowDetails(id)

            val title = document.selectFirst("div.Ficha.fchlt div.Container .Title")?.text() ?: ""
            val overview = document.selectFirst("div.Description")?.text()?.removeSurrounding("\"")
            val poster = document.selectFirst("div.AnimeCover div.Image figure img")?.attr("src")
            val genres = document.select("nav.Nvgnrs a").map {
                Genre(id = it.attr("href").substringAfterLast("/"), name = it.text())
            }

            val script = document.select("script")
                .firstOrNull { it.data().contains("var episodes =") }
                ?.data() ?: ""

            val episodesData = script.substringAfter("var episodes = [").substringBefore("];")
            val animeUri = script.substringAfter("var anime_info = [").substringBefore("];")
                .split(",")[2].replace("\"", "")

            val episodes = episodesData.split("],[").mapNotNull {
                val episodeNum = it.replace("[", "").replace("]", "").split(",")[0]
                if (episodeNum.isNotBlank()) {
                    Episode(
                        id = "ver/$animeUri-$episodeNum",
                        number = episodeNum.toIntOrNull() ?: 0,
                        title = "Episodio $episodeNum"
                    )
                } else {
                    null
                }
            }.reversed()

            val seasons = listOf(
                Season(
                    id = id,
                    number = 1,
                    title = "Episodios",
                    episodes = episodes
                )
            )

            TvShow(
                id = id,
                title = title,
                overview = overview,
                poster = poster?.let { "$baseUrl$it" },
                genres = genres,
                seasons = seasons
            )
        } catch (e: Exception) {
            TvShow(id = id, title = "Error al cargar")
        }
    }

    override suspend fun getMovie(id: String): Movie {
        val show = getTvShow(id)
        return Movie(
            id = show.id,
            title = show.title,
            overview = show.overview,
            poster = show.poster,
            genres = show.genres,
            cast = emptyList(),
            recommendations = emptyList()
        )
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val show = getTvShow(seasonId)
        return show.seasons.firstOrNull()?.episodes ?: emptyList()
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val genreName = id.replace("-", " ").replaceFirstChar { it.uppercase() }
        return try {
            val document = service.getGenre(genre = id, page = page)

            val shows = document.select("ul.ListAnimes li article").mapNotNull { element ->
                val url = element.selectFirst("div.Description a.Button")?.attr("href") ?: return@mapNotNull null
                val showId = url.substringAfterLast("/")
                val title = element.selectFirst("a h3")?.text() ?: ""
                val posterUrl = element.selectFirst("a div.Image figure img")?.attr("src")
                val type = element.selectFirst("span.Type")?.text()

                val finalPoster = if (posterUrl?.startsWith("http") == true) posterUrl else posterUrl?.let { "$baseUrl$it" }

                if (type == "Película") {
                    Movie(id = showId, title = title, poster = finalPoster)
                } else {
                    TvShow(id = showId, title = title, poster = finalPoster)
                }
            }

            Genre(id = id, name = genreName, shows = shows)
        } catch (e: Exception) {
            Genre(id = id, name = genreName, shows = emptyList())
        }
    }

    override suspend fun getPeople(id: String, page: Int): People {
        throw Exception("Not implemented for this provider")
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        return try {
            val url = when (videoType) {
                is Video.Type.Movie -> {
                    val movieDetailsPage = service.getPage("$baseUrl/anime/$id")
                    val script = movieDetailsPage.selectFirst("script:containsData(var episodes =)")?.data() ?: return emptyList()
                    val animeUri = script.substringAfter("var anime_info = [").substringBefore("];").split(",")[2].replace("\"", "")
                    "$baseUrl/ver/$animeUri-1"
                }
                is Video.Type.Episode -> "$baseUrl/$id"
            }

            val document = service.getPage(url)
            val script = document.selectFirst("script:containsData(var videos = {)")?.data() ?: return emptyList()
            val jsonString = script.substringAfter("var videos =").substringBefore(";").trim()
            val serverModel = json.decodeFromString<ServerModel>(jsonString)

            serverModel.sub.mapNotNull { sub ->
                if (sub.code.isNotBlank()) {
                    Video.Server(
                        id = sub.code,
                        name = sub.title ?: "Servidor Desconocido"
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return Extractor.extract(server.id, server)
    }
}