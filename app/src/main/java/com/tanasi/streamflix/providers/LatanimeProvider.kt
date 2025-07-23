package com.tanasi.streamflix.providers

import android.util.Base64
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
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.File
import java.util.concurrent.TimeUnit

object LatanimeProvider : Provider {

    override val name = "Latanime"
    override val baseUrl = "https://latanime.org"
    override val language = "es"

    // --- NUEVA CONFIGURACIÓN DE RED PROFESIONAL ---
    private val client = getOkHttpClient()
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JsoupConverterFactory.create())
        .client(client)
        .build()
    private val service = retrofit.create(LatanimeService::class.java)

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

    private interface LatanimeService {
        @GET
        suspend fun getPage(@Url url: String): Document
    }

    override suspend fun getHome(): List<Category> {
        return try {
            coroutineScope {
                val homeDeferred = async { service.getPage(baseUrl) }
                val animes2025Deferred = async { service.getPage("$baseUrl/animes?fecha=2025") }
                val animes2024Deferred = async { service.getPage("$baseUrl/animes?fecha=2024") }
                val animes2023Deferred = async { service.getPage("$baseUrl/animes?fecha=2023") }

                val homeDocument = homeDeferred.await()
                val bannerShows = homeDocument.select("div.carousel-item").map { element ->
                    val bannerUrl = element.selectFirst("img")?.attr("data-src")
                    TvShow(
                        id = element.selectFirst("a")!!.attr("href"),
                        title = element.selectFirst("span.span-slider")!!.text(),
                        banner = bannerUrl?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                    )
                }

                val recentShows = homeDocument.select("h2:contains(Añadidos recientemente) + div.row div.col-6").map { element ->
                    val posterUrl = element.selectFirst("img")?.attr("data-src") ?: ""
                    TvShow(
                        id = element.selectFirst("a")!!.attr("href"),
                        title = element.selectFirst("h2.mt-3")!!.text().substringAfter(" - "),
                        poster = if (posterUrl.startsWith("http")) posterUrl else "$baseUrl$posterUrl"
                    )
                }

                val categories = mutableListOf(
                    Category(name = Category.FEATURED, list = bannerShows),
                    Category(name = "Añadidos Recientemente", list = recentShows)
                )

                fun parseAnimesFromPage(document: Document): List<TvShow> {
                    return document.select("div.row > div:has(a)").map {
                        val posterUrl = it.selectFirst("img")?.attr("data-src") ?: it.selectFirst("img")?.attr("src") ?: ""
                        TvShow(
                            id = it.selectFirst("a")!!.attr("href"),
                            title = it.selectFirst("div.seriedetails > h3")!!.text(),
                            poster = if (posterUrl.startsWith("http")) posterUrl else "$baseUrl$posterUrl"
                        )
                    }
                }

                try {
                    val animes2025Document = animes2025Deferred.await()
                    categories.add(Category(name = "Animes del 2025", list = parseAnimesFromPage(animes2025Document).take(12)))
                } catch (e: Exception) {
                }

                try {
                    val animes2024Document = animes2024Deferred.await()
                    categories.add(Category(name = "Animes del 2024", list = parseAnimesFromPage(animes2024Document).take(12)))
                } catch (e: Exception) {
                }

                try {
                    val animes2023Document = animes2023Deferred.await()
                    categories.add(Category(name = "Animes del 2023", list = parseAnimesFromPage(animes2023Document).take(12)))
                } catch (e: Exception) {
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
                Genre("accion", "Acción"),
                Genre("aventura", "Aventura"),
                Genre("carreras", "Carreras"),
                Genre("ciencia-ficcion", "Ciencia Ficción"),
                Genre("comedia", "Comedia"),
                Genre("deportes", "Deportes"),
                Genre("drama", "Drama"),
                Genre("escolares", "Escolares"),
                Genre("fantasia", "Fantasía"),
                Genre("harem", "Harem"),
                Genre("horror", "Horror"),
                Genre("josei", "Josei"),
                Genre("lucha", "Lucha"),
                Genre("magia", "Magia"),
                Genre("mecha", "Mecha"),
                Genre("militar", "Militar"),
                Genre("misterio", "Misterio"),
                Genre("musica", "Música"),
                Genre("psicologico", "Psicológico"),
                Genre("romance", "Romance"),
                Genre("seinen", "Seinen"),
                Genre("shojo", "Shojo"),
                Genre("shonen", "Shonen"),
                Genre("sobrenatural", "Sobrenatural"),
                Genre("vampiros", "Vampiros"),
                Genre("yaoi", "Yaoi"),
                Genre("yuri", "Yuri"),
            )
        }
        if (page > 1) {
            return emptyList()
        }
        return try {
            val document = service.getPage("$baseUrl/buscar?q=$query")
            document.select("div.row > div:has(a)").map {
                val posterUrl = it.selectFirst("img")!!.attr("src")
                TvShow(
                    id = it.selectFirst("a")!!.attr("href"),
                    title = it.selectFirst("div.seriedetails > h3")!!.text(),
                    poster = if (posterUrl.startsWith("http")) posterUrl else "$baseUrl$posterUrl"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        return try {
            val document = service.getPage("$baseUrl/animes?fecha=false&genero=false&letra=false&categoria=Película&p=$page")
            document.select("div.row > div:has(a)").map {
                val posterUrl = it.selectFirst("img")?.attr("data-src") ?: it.selectFirst("img")?.attr("src") ?: ""
                Movie(
                    id = it.selectFirst("a")!!.attr("href"),
                    title = it.selectFirst("div.seriedetails > h3")!!.text(),
                    poster = if (posterUrl.startsWith("http")) posterUrl else "$baseUrl$posterUrl"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        return try {
            val document = service.getPage("$baseUrl/animes?p=$page")
            document.select("div.row > div:has(a)").map {
                val posterUrl = it.selectFirst("img")?.attr("data-src") ?: it.selectFirst("img")?.attr("src") ?: ""
                TvShow(
                    id = it.selectFirst("a")!!.attr("href"),
                    title = it.selectFirst("div.seriedetails > h3")!!.text(),
                    poster = if (posterUrl.startsWith("http")) posterUrl else "$baseUrl$posterUrl"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getPage(id)
        val posterUrl = document.selectFirst("div.serieimgficha > img")?.attr("src")
        return Movie(
            id = id,
            title = document.selectFirst("div.row > div > h2")?.text() ?: "",
            poster = posterUrl?.let { if (it.startsWith("http")) it else "$baseUrl$it" },
            overview = document.selectFirst("div.row > div > p.my-2")?.text(),
            genres = document.select("div.row > div > a:has(div.btn)").map {
                Genre(
                    id = it.attr("href").substringAfterLast("/"),
                    name = it.text()
                )
            }
        )
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getPage(id)
        val posterUrl = document.selectFirst("div.serieimgficha > img")?.attr("src")
        return TvShow(
            id = id,
            title = document.selectFirst("div.row > div > h2")?.text() ?: "",
            poster = posterUrl?.let { if (it.startsWith("http")) it else "$baseUrl$it" },
            overview = document.selectFirst("div.row > div > p.my-2")?.text(),
            genres = document.select("div.row > div > a:has(div.btn)").map {
                Genre(
                    id = it.attr("href").substringAfterLast("/"),
                    name = it.text()
                )
            },
            seasons = listOf(
                Season(
                    id = id,
                    number = 1,
                    title = "Episodios"
                )
            )
        )
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        return try {
            val document = service.getPage(seasonId)
            document.select("div.row > div > div.row > div > a").map { element ->
                val title = element.text()
                val posterUrl = element.selectFirst("img")?.attr("data-src")
                Episode(
                    id = element.attr("href"),
                    number = title.substringAfter("Capitulo ").toIntOrNull() ?: 1,
                    title = title.replace("- ", ""),
                    poster = posterUrl?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
                )
            }.reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        return try {
            val document = service.getPage(id)
            document.select("li#play-video > a.play-video").map {
                val serverName = it.ownText().trim()
                val encodedUrl = it.attr("data-player")
                val decodedUrl = String(Base64.decode(encodedUrl, Base64.DEFAULT))
                Video.Server(
                    id = decodedUrl,
                    name = serverName
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return Extractor.extract(server.id, server)
    }

    override val logo: String get() = "https://latanime.org/public/img/logito.png"

    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = service.getPage("$baseUrl/genero/$id?p=$page")
        val shows = document.select("div.row > div:has(a)").map {
            val posterUrl = it.selectFirst("img")!!.attr("src")
            TvShow(
                id = it.selectFirst("a")!!.attr("href"),
                title = it.selectFirst("div.seriedetails > h3")!!.text(),
                poster = if (posterUrl.startsWith("http")) posterUrl else "$baseUrl$posterUrl"
            )
        }
        return Genre(
            id = id,
            name = id.replaceFirstChar { it.uppercase() },
            shows = shows
        )
    }

    override suspend fun getPeople(id: String, page: Int): People = throw Exception("No soportado")
}