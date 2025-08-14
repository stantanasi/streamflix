package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

object AnimeIDProvider : Provider {

    override val name = "AnimeID"
    override val baseUrl = "https://www.animeid.tv"
    override val language = "es"
    override val logo = "https://static.mocaverse.xyz/partner/5bfb09c5-9431-4745-852d-bb342a6d44d2-logo.png"

    private val client = getOkHttpClient()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JsoupConverterFactory.create())
        .client(client)
        .build()

    private val service = retrofit.create(AnimeIDService::class.java)
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

    private interface AnimeIDService {
        @GET
        suspend fun getPage(@Url url: String): Document
    }


    override suspend fun getHome(): List<Category> {
        return try {
            coroutineScope {
                val newestAnimesDeferred = async { service.getPage("$baseUrl/series?sort=newest&pag=1") }
                val winter2024Deferred = async { service.getPage("$baseUrl/genero/invierno-2024") }
                val fall2024Deferred = async { service.getPage("$baseUrl/genero/otono-2024") }

                val newestAnimesDocument = newestAnimesDeferred.await()
                val latestAnimes = parseTvShowsFromElements(newestAnimesDocument.select("#result article.item"))

                val bannerShows = latestAnimes.map { it.copy(banner = it.poster) }

                val categories = mutableListOf(
                    Category(Category.FEATURED, bannerShows.take(5)),
                    Category("Últimos Animes Agregados", latestAnimes)
                )

                try {
                    val winter2024Document = winter2024Deferred.await()
                    val winter2024Shows = parseTvShowsFromElements(winter2024Document.select("#result article.item"))
                    categories.add(Category("Temporada Invierno 2024", winter2024Shows))
                } catch (_: Exception) {}

                try {
                    val fall2024Document = fall2024Deferred.await()
                    val fall2024Shows = parseTvShowsFromElements(fall2024Document.select("#result article.item"))
                    categories.add(Category("Temporada Otoño 2024", fall2024Shows))
                } catch (_: Exception) {}

                categories
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isBlank()) {
            return listOf(
                Genre("accion", "Acción"), Genre("aventura", "Aventura"), Genre("carreras", "Carreras"),
                Genre("ciencia-ficcion", "Ciencia Ficción"), Genre("comedia", "Comedia"),
                Genre("deportes", "Deportes"), Genre("drama", "Drama"), Genre("escolares", "Escolares"),
                Genre("fantasia", "Fantasía"), Genre("gore", "Gore"), Genre("harem", "Harem"),
                Genre("historico", "Histórico"), Genre("horror", "Horror"), Genre("josei", "Josei"),
                Genre("juegos", "Juegos"), Genre("magia", "Magia"), Genre("mecha", "Mecha"),
                Genre("militar", "Militar"), Genre("misterio", "Misterio"), Genre("musica", "Música"),
                Genre("parodia", "Parodia"), Genre("psicologico", "Psicológico"), Genre("romance", "Romance"),
                Genre("seinen", "Seinen"), Genre("shojo", "Shōjo"), Genre("shonen", "Shōnen"),
                Genre("sobrenatural", "Sobrenatural"), Genre("terror", "Terror"), Genre("vampiros", "Vampiros"),
                Genre("yaoi", "Yaoi"), Genre("yuri", "Yuri")
            )
        }
        val document = service.getPage("$baseUrl/buscar?q=$query&pag=$page")
        return parseTvShowsFromElements(document.select("#result article.item"))
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val document = service.getPage("$baseUrl/peliculas?pag=$page")
        return parseMoviesFromElements(document.select("#result article.item"))
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = service.getPage("$baseUrl/series?sort=views&pag=$page")
        return parseTvShowsFromElements(document.select("#result article.item"))
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getPage(id)
        val title = document.selectFirst("#anime section hgroup h1")?.text() ?: ""
        val originalOverview = document.selectFirst("#anime section p.sinopsis")?.text()?.removeSurrounding("\"")

        val newOverview = """
            ${originalOverview ?: ""}

            ----------------------------------------------------
            NOTA DEL PROVEEDOR:
            Esto parece ser una colección de películas. Para ver los servidores, por favor, use la función de búsqueda (la lupa 🔍) y busque "$title". Aparecerá como una serie y podrá seleccionar cada película individualmente.
            ----------------------------------------------------
        """.trimIndent()

        return Movie(
            id = id,
            title = title,
            poster = document.selectFirst("#anime figure img.cover")?.attr("src"),
            overview = newOverview,
            genres = document.select("#anime section ul.tags li a").map {
                Genre(id = it.attr("href").substringAfterLast('/'), name = it.text())
            },
        )
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getPage(id)
        val animeAjaxId = document.selectFirst("#ord")?.attr("data-id") ?: ""

        val seasons = if (animeAjaxId.isNotEmpty()) {
            listOf(
                Season(
                    id = "$animeAjaxId|S|$id", // ID Compuesto: "ID_AJAX|S|URL_REFERER"
                    number = 1,
                    title = "Episodios"
                )
            )
        } else {
            emptyList()
        }

        return TvShow(
            id = id,
            title = document.selectFirst("#anime section hgroup h1")?.text() ?: "",
            poster = document.selectFirst("#anime figure img.cover")?.attr("src"),
            overview = document.selectFirst("#anime section p.sinopsis")?.text()?.removeSurrounding("\""),
            genres = document.select("#anime section ul.tags li a").map {
                Genre(id = it.attr("href").substringAfterLast('/'), name = it.text())
            },
            seasons = seasons,
        )
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val (animeAjaxId, refererPath) = try {
            val parts = seasonId.split("|S|")
            parts[0] to parts[1]
        } catch (e: Exception) {
            return emptyList()
        }
        return getEpisodesFromAjax(animeAjaxId, baseUrl + refererPath)
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        return try {
            val document = when (videoType) {
                is Video.Type.Movie -> {
                    val moviePage = service.getPage(id)
                    val episodeUrl = moviePage.selectFirst("#listado li a")?.attr("href") ?: return emptyList()
                    service.getPage(episodeUrl)
                }
                is Video.Type.Episode -> service.getPage(id)
            }
            parseServersFromPage(document)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return Extractor.extract(server.id, server)
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = service.getPage("$baseUrl/genero/$id?pag=$page")
        val shows = document.select("#result article.item").map { element ->
            val href = element.selectFirst("a")?.attr("href") ?: ""
            if (href.contains("/peliculas/")) {
                parseMoviesFromElements(listOf(element)).first()
            } else {
                parseTvShowsFromElements(listOf(element)).first()
            }
        }
        return Genre(
            id = id,
            name = id.replaceFirstChar { it.titlecase(Locale.ROOT) },
            shows = shows
        )
    }

    override suspend fun getPeople(id: String, page: Int): People {
        throw NotImplementedError("Este proveedor no soporta la búsqueda por persona.")
    }


    // Funciones de Ayuda (Helpers)

    private fun parseTvShowsFromElements(elements: List<org.jsoup.nodes.Element>): List<TvShow> {
        return elements.map { element ->
            TvShow(
                id = element.selectFirst("a")?.attr("href") ?: "",
                title = element.selectFirst("a header")?.text() ?: "",
                poster = element.selectFirst("a figure img")?.attr("src"),
            )
        }
    }

    private fun parseMoviesFromElements(elements: List<org.jsoup.nodes.Element>): List<Movie> {
        return elements.map { element ->
            Movie(
                id = element.selectFirst("a")?.attr("href") ?: "",
                title = element.selectFirst("a header")?.text() ?: "",
                poster = element.selectFirst("a figure img")?.attr("src"),
            )
        }
    }

    private fun getEpisodesFromAjax(animeId: String, refererUrl: String): List<Episode> {
        val episodeList = mutableListOf<Episode>()
        var page = 1
        var hasNextPage = true
        while (hasNextPage) {
            val ajaxUrl = "$baseUrl/ajax/caps?id=$animeId&ord=DESC&pag=$page"
            val request = Request.Builder().url(ajaxUrl).get()
                .addHeader("Referer", refererUrl)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val episodesJson = json.decodeFromString<JsonObject>(responseBody)["list"]?.jsonArray
                if (episodesJson.isNullOrEmpty()) {
                    hasNextPage = false
                } else {
                    episodesJson.forEach { cap ->
                        val capObject = cap.jsonObject
                        val epNum = capObject["numero"]?.jsonPrimitive?.content ?: "0"
                        episodeList.add(
                            Episode(
                                id = capObject["href"]?.jsonPrimitive?.content ?: "",
                                number = epNum.toFloatOrNull()?.toInt() ?: 0,
                                title = "Episodio $epNum"
                            )
                        )
                    }
                    page++
                }
            } else {
                hasNextPage = false
            }
        }
        return episodeList.reversed()
    }

    private fun parseServersFromPage(document: Document): List<Video.Server> {
        return document.select("#partes div.container li.subtab div.parte").mapNotNull { script ->
            val jsonString = script.attr("data")
            val serverName = script.closest(".subtab")?.attr("data-tab-id")?.let { tabId ->
                document.selectFirst("#mirrors [data-tab-id=\"$tabId\"]")?.ownText()?.trim()
            } ?: "Unknown Server"
            val jsonUnescaped = unescapeJava(jsonString).replace("\\", "")
            val url = fetchUrls(jsonUnescaped).firstOrNull()?.replace("\\\\", "\\")
            if (url != null) {
                Video.Server(id = url, name = serverName)
            } else {
                null
            }
        }
    }

    private fun unescapeJava(escaped: String): String {
        if (!escaped.contains("\\u")) return escaped
        var processed = ""
        var position = escaped.indexOf("\\u")
        var tempEscaped = escaped
        while (position != -1) {
            if (position != 0) {
                processed += tempEscaped.substring(0, position)
            }
            val token = tempEscaped.substring(position + 2, position + 6)
            tempEscaped = tempEscaped.substring(position + 6)
            processed += token.toInt(16).toChar()
            position = tempEscaped.indexOf("\\u")
        }
        processed += tempEscaped
        return processed
    }

    private fun fetchUrls(text: String): List<String> {
        val linkRegex = "(http|ftp|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])".toRegex()
        return linkRegex.findAll(text).map { it.value.trim().removeSurrounding("\"") }.toList()
    }
}