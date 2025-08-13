package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.File
import java.util.concurrent.TimeUnit

object AnimeBumProvider : Provider {

    override val name = "AnimeBum"
    override val baseUrl = "https://www.animebum.net"
    override val language = "es"
    override val logo = "$baseUrl/images/logo.png"

    private val client = getOkHttpClient()

    private val service = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JsoupConverterFactory.create())
        .client(client)
        .build()
        .create(AnimeBumService::class.java)

    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024))
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .dns(
                DnsOverHttps.Builder().client(OkHttpClient())
                    .url("https://1.1.1.1/dns-query".toHttpUrl())
                    .build()
            )
            .build()
    }

    private interface AnimeBumService {
        @GET
        suspend fun getPage(@Url url: String): Document
    }

    private fun parseShowsFromPage(document: Document): List<TvShow> {
        return document.select("article.serie").mapNotNull {
            val titleElement = it.selectFirst("div.title h3 a") ?: return@mapNotNull null
            TvShow(
                id = titleElement.attr("href"),
                title = titleElement.attr("title"),
                poster = it.selectFirst("figure.image img")?.attr("src")
            )
        }
    }

    private fun parseSearchShows(elements: List<Element>): List<AppAdapter.Item> {
        return elements.mapNotNull { element ->
            val urlElement = element.selectFirst("div.search-results__left a") ?: return@mapNotNull null
            val title = element.selectFirst("div.search-results__left a h2")?.text() ?: ""
            val poster = element.selectFirst("div.search-results__img a img")?.attr("src")
            val overview = element.selectFirst("div.search-results__left div.description")?.text()
            val type = element.selectFirst("div.search-results__left .result-type")?.text()

            if (type == "Película") {
                Movie(
                    id = urlElement.attr("href"),
                    title = title,
                    poster = poster,
                    overview = overview
                )
            } else {
                TvShow(
                    id = urlElement.attr("href"),
                    title = title,
                    poster = poster,
                    overview = overview
                )
            }
        }
    }

    override suspend fun getHome(): List<Category> {
        return try {
            coroutineScope {
                val emisionDeferred = async { service.getPage("$baseUrl/emision") }
                val latinoDeferred = async { service.getPage("$baseUrl/genero/audio-latino") }
                val recientesDeferred = async { service.getPage("$baseUrl/series") }

                val categories = mutableListOf<Category>()

                val emisionShows = parseShowsFromPage(emisionDeferred.await())
                if (emisionShows.isNotEmpty()) {
                    val bannerShows = emisionShows.take(10).map { it.copy(banner = it.poster) }
                    categories.add(Category(Category.FEATURED, bannerShows))
                    categories.add(Category("En Emisión", emisionShows))
                }

                val latinoShows = parseShowsFromPage(latinoDeferred.await())
                if (latinoShows.isNotEmpty()) {
                    categories.add(Category("Audio Latino", latinoShows))
                }

                val recentShows = parseShowsFromPage(recientesDeferred.await())
                if (recentShows.isNotEmpty()) {
                    categories.add(Category("Series Recientes", recentShows))
                }

                categories
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isBlank()) {
            return listOf(
                Genre("genero/accion", "Acción"), Genre("genero/aventura", "Aventura"),
                Genre("genero/ciencia-ficcion", "Ciencia Ficción"), Genre("genero/comedia", "Comedia"),
                Genre("genero/deportes", "Deportes"), Genre("genero/demonios", "Demonios"),
                Genre("genero/drama", "Drama"), Genre("genero/ecchi", "Ecchi"),
                Genre("genero/escolares", "Escolares"), Genre("genero/fantasia", "Fantasía"),
                Genre("genero/harem", "Harem"), Genre("genero/historico", "Histórico"),
                Genre("genero/juegos", "Juegos"), Genre("genero/latino", "Latino"),
                Genre("genero/lucha", "Lucha"), Genre("genero/magia", "Magia"),
                Genre("genero/mecha", "Mecha"), Genre("genero/militar", "Militar"),
                Genre("genero/misterio", "Misterio"), Genre("genero/musica", "Música"),
                Genre("genero/parodia", "Parodia"), Genre("genero/policia", "Policía"),
                Genre("genero/psicologico", "Psicológico"), Genre("genero/recuentos-de-la-vida", "Recuentos de la Vida"),
                Genre("genero/romance", "Romance"), Genre("genero/samurai", "Samurái"),
                Genre("genero/seinen", "Seinen"), Genre("genero/shoujo", "Shoujo"),
                Genre("genero/shounen", "Shounen"), Genre("genero/sobrenatural", "Sobrenatural"),
                Genre("genero/super-poderes", "Superpoderes"), Genre("genero/suspense", "Suspenso"),
                Genre("genero/terror", "Terror"), Genre("genero/vampiros", "Vampiros"),
                Genre("genero/yaoi", "Yaoi")
            )
        }
        return try {
            val document = service.getPage("$baseUrl/search?s=$query&page=$page")
            parseSearchShows(document.select("div.search-results__item"))
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        return try {
            val document = service.getPage("$baseUrl/peliculas?page=$page")
            document.select("article.serie").mapNotNull {
                val titleElement = it.selectFirst("div.title h3 a") ?: return@mapNotNull null
                Movie(
                    id = titleElement.attr("href"),
                    title = titleElement.attr("title"),
                    poster = it.selectFirst("figure.image img")?.attr("src")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        return try {
            val document = service.getPage("$baseUrl/series?page=$page")
            parseShowsFromPage(document)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        return try {
            val document = service.getPage("$baseUrl/$id?page=$page")
            val shows = parseShowsFromPage(document)
            val genreName = document.selectFirst("h1.main-title")?.text() ?: id.substringAfterLast("/")
            Genre(id = id, name = genreName, shows = shows)
        } catch (e: Exception) {
            Genre(id = id, name = "Error", shows = emptyList())
        }
    }

    override suspend fun getMovie(id: String): Movie {
        return try {
            val document = service.getPage(id)
            val ratingPercent = document.selectFirst("div.Prct #A-circle")?.attr("data-percent")?.toDoubleOrNull()
            Movie(
                id = id,
                title = document.selectFirst("h1.title-h1-serie")?.text()?.replace(" Online", "") ?: "",
                poster = document.selectFirst("div.poster-serie img.poster-serie__img")?.attr("src"),
                overview = document.selectFirst("div.description p")?.text(),
                genres = document.select("div.boom-categories a").map {
                    Genre(id = it.attr("href"), name = it.text())
                },
                released = document.selectFirst("p.datos-serie strong:contains(Año)")?.parent()?.text()?.substringAfter("Año:")?.trim(),
                rating = ratingPercent?.let { it / 20.0 }
            )
        } catch (e: Exception) {
            Movie(id = id, title = "Error al cargar")
        }
    }

    override suspend fun getTvShow(id: String): TvShow {
        return try {
            val document = service.getPage(id)
            val ratingPercent = document.selectFirst("div.Prct #A-circle")?.attr("data-percent")?.toDoubleOrNull()
            val episodes = document.select("ul.list-episodies li").mapNotNull { element ->
                val a = element.selectFirst("a") ?: return@mapNotNull null
                val episodeTitle = a.ownText().trim()
                val episodeNumber = Regex("""Episodio (\d+)""").find(episodeTitle)?.groupValues?.get(1)?.toIntOrNull()
                Episode(
                    id = a.attr("href"),
                    number = episodeNumber ?: 0,
                    title = episodeTitle
                )
            }

            TvShow(
                id = id,
                title = document.selectFirst("h1.title-h1-serie")?.text()?.replace(" Online", "") ?: "",
                poster = document.selectFirst("div.poster-serie img.poster-serie__img")?.attr("src"),
                overview = document.selectFirst("div.description p")?.text(),
                genres = document.select("div.boom-categories a").map {
                    Genre(id = it.attr("href"), name = it.text())
                },
                released = document.selectFirst("p.datos-serie strong:contains(Año)")?.parent()?.text()?.substringAfter("Año:")?.trim(),
                rating = ratingPercent?.let { it / 20.0 },
                seasons = listOf(Season(id = id, number = 1, title = "Episodios", episodes = episodes))
            )
        } catch (e: Exception) {
            TvShow(id = id, title = "Error al cargar")
        }
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        return try {
            getTvShow(seasonId).seasons.firstOrNull()?.episodes ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        return try {
            val url = when (videoType) {
                is Video.Type.Movie -> {
                    val moviePage = service.getPage(id)
                    moviePage.selectFirst("ul.list-episodies li a")?.attr("href") ?: id
                }
                is Video.Type.Episode -> id
            }

            val document = service.getPage(url)
            val scriptContent = document.selectFirst("script:containsData(var video = [])")?.data() ?: return emptyList()

            val iframeRegex = """video\[\d+\]\s*=\s*['"]<iframe[^>]+src=["']([^"']+)["']""".toRegex()
            iframeRegex.findAll(scriptContent).mapNotNull { matchResult ->
                var videoUrl = matchResult.groupValues[1]
                if (videoUrl.startsWith("//")) {
                    videoUrl = "https:$videoUrl"
                }

                val serverName = try {
                    videoUrl.toHttpUrl().host
                        .replaceFirst("www.", "")
                        .substringBefore(".")
                        .replaceFirstChar { it.titlecase() }
                } catch (e: Exception) {
                    "Servidor"
                }

                Video.Server(id = videoUrl, name = serverName)
            }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return Extractor.extract(server.id, server)
    }

    override suspend fun getPeople(id: String, page: Int): People {
        throw Exception("Esta función no está disponible en AnimeBum")
    }
}