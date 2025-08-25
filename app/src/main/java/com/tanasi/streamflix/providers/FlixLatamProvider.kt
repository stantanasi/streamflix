package com.tanasi.streamflix.providers

import android.util.Log
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.*
import com.tanasi.streamflix.models.flixlatam.DataLinkItem
import com.tanasi.streamflix.models.flixlatam.PlayerResponse
import com.tanasi.streamflix.utils.CryptoAES
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.Locale

object FlixLatamProvider : Provider {

    override val name = "FlixLatam"
    override val baseUrl = "https://flixlatam.com"
    override val language = "es"
    override val logo = "$baseUrl/wp-content/uploads/2022/04/cropped-Series-Latinoamerica.jpg"

    private val service = FlixLatamService.build(baseUrl)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getHome(): List<Category> = coroutineScope {
        try {
            val document = service.getPage(baseUrl, baseUrl)
            val categories = mutableListOf<Category>()

            val featuredShows = document.select("#slider-movies-tvshows .item").mapNotNull {
                val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val banner = it.selectFirst("img")?.attr("src")
                val title = it.selectFirst(".data h3")?.text() ?: ""
                val id = href.getId()
                val type = it.selectFirst("span.item_type")?.text()

                when {
                    type == "TV" || href.contains("/serie/") -> TvShow(id = id, title = title, banner = banner)
                    else -> Movie(id = id, title = title, banner = banner)
                }
            }
            if (featuredShows.isNotEmpty()) {
                categories.add(Category(Category.FEATURED, featuredShows))
            }

            val sections = document.select("div.module")
            sections.forEach { section ->
                val title = section.selectFirst("header > h2")?.text() ?: return@forEach
                val shows = parseShows(section.select(".items article"))
                if (shows.isNotEmpty()) {
                    categories.add(Category(title, shows))
                }
            }

            categories
        } catch (e: Exception) {
            Log.e("FlixLatamProvider", "Error en getHome: ${e.message}")
            emptyList()
        }
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isBlank()) {
            return listOf(
                Genre(id = "accion", name = "Acción"), Genre(id = "animacion", name = "Animación"),
                Genre(id = "aventura", name = "Aventura"), Genre(id = "ciencia-ficcion", name = "Ciencia Ficción"),
                Genre(id = "comedia", name = "Comedia"), Genre(id = "crimen", name = "Crimen"),
                Genre(id = "documental", name = "Documental"), Genre(id = "drama", name = "Drama"),
                Genre(id = "familia", name = "Familia"), Genre(id = "fantasia", name = "Fantasía"),
                Genre(id = "historia", name = "Historia"), Genre(id = "kids", name = "Kids"),
                Genre(id = "misterio", name = "Misterio"), Genre(id = "musica", name = "Música"),
                Genre(id = "romance", name = "Romance"), Genre(id = "terror", name = "Terror"),
                Genre(id = "western", name = "Western")
            )
        }
        return try {
            val url = "$baseUrl/page/$page/?s=$query"
            val document = service.getPage(url, baseUrl)
            parseShows(document.select("div.search-page article, div.items article"))
        } catch (e: Exception) {
            Log.e("FlixLatamProvider", "Error en search: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        return try {
            val url = if (page == 1) "$baseUrl/pelicula/" else "$baseUrl/pelicula/page/$page/"
            val document = service.getPage(url, baseUrl)
            parseShows(document.select("div.items article")).filterIsInstance<Movie>()
        } catch (e: Exception) {
            Log.e("FlixLatamProvider", "Error en getMovies: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        return try {
            val url = if (page == 1) "$baseUrl/series/" else "$baseUrl/series/page/$page/"
            val document = service.getPage(url, baseUrl)
            parseShows(document.select("#archive-content article.item")).filterIsInstance<TvShow>()
        } catch (e: Exception) {
            Log.e("FlixLatamProvider", "Error en getTvShows: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        return try {
            val url = if (page == 1) "$baseUrl/genero/$id/" else "$baseUrl/genero/$id/page/$page/"
            val document = service.getPage(url, baseUrl)
            val shows = parseShows(document.select("div.items article"))
            val genreName = document.selectFirst("h1.Title")?.text() ?: id.replaceFirstChar { it.uppercase() }
            Genre(id = id, name = genreName, shows = shows)
        } catch (e: Exception) {
            Genre(id = id, name = id.replaceFirstChar { it.uppercase() })
        }
    }

    override suspend fun getMovie(id: String): Movie {
        return try {
            val url = "$baseUrl/pelicula/$id/"
            val document = service.getPage(url, baseUrl)
            val details = parseShowDetails(document)
            Movie(
                id = id,
                title = document.selectFirst(".sheader .data h1")?.text() ?: "",
                poster = document.selectFirst(".sheader .poster img")?.attr("src"),
                banner = document.selectFirst("style:containsData(background-image)")?.data()?.getBackgroundImage(),
                overview = details.overview,
                rating = details.rating,
                released = details.released,
                genres = details.genres,
                cast = details.cast,
                recommendations = parseShows(document.select("#single_relacionados article"))
            )
        } catch (e: Exception) {
            Movie(id = id, title = "Error al cargar")
        }
    }

    override suspend fun getTvShow(id: String): TvShow {
        return try {
            val url = "$baseUrl/serie/$id/"
            val document = service.getPage(url, baseUrl)
            val details = parseShowDetails(document)

            val seasons = document.select("#seasons .se-c").mapNotNull { seasonElement ->
                val seasonNumber = seasonElement.selectFirst(".se-q span.se-t")?.text()?.toIntOrNull() ?: return@mapNotNull null
                Season(id = "$id|$seasonNumber", number = seasonNumber, title = "Temporada $seasonNumber")
            }.reversed()

            TvShow(
                id = id,
                title = document.selectFirst(".sheader .data h1")?.text() ?: "",
                poster = document.selectFirst(".sheader .poster img")?.attr("src"),
                banner = document.selectFirst("style:containsData(background-image)")?.data()?.getBackgroundImage(),
                overview = details.overview,
                rating = details.rating,
                released = details.released,
                genres = details.genres,
                cast = details.cast,
                recommendations = parseShows(document.select("#single_relacionados article")),
                seasons = seasons
            )
        } catch (e: Exception) {
            TvShow(id = id, title = "Error al cargar")
        }
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        return try {
            val (showSlug, seasonNumberStr) = seasonId.split('|')
            val document = service.getPage("$baseUrl/series/$showSlug/", baseUrl)

            val seasonElement = document.select("#seasons .se-c").find {
                it.selectFirst(".se-q span.se-t")?.text() == seasonNumberStr
            } ?: return emptyList()

            seasonElement.select(".se-a ul.episodios li").mapNotNull { episodeElement ->
                val a = episodeElement.selectFirst(".episodiotitle a") ?: return@mapNotNull null
                val href = a.attr("href")
                val posterUrl = episodeElement.selectFirst(".imagen img")?.attr("src")
                val title = a.text()
                val numberStr = episodeElement.selectFirst(".numerando")?.text()?.trim()?.split("-")?.getOrNull(1)?.trim()

                Episode(
                    id = href.getId(),
                    title = title,
                    number = numberStr?.toIntOrNull() ?: 0,
                    poster = posterUrl
                )
            }
        } catch (e: Exception) {
            Log.e("FlixLatamProvider", "Error en getEpisodesBySeason: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        try {
            val (postId, nume, type) = when (videoType) {
                is Video.Type.Movie -> {
                    val moviePage = service.getPage("$baseUrl/pelicula/$id/", baseUrl)
                    val numericId = moviePage.body().className().substringAfter("postid-").substringBefore(" ").trim()
                    Triple(numericId, "1", "movie")
                }
                is Video.Type.Episode -> {
                    val episodeUrl = "$baseUrl/episodio/$id/"
                    val episodePage = service.getPage(episodeUrl, baseUrl)
                    val playerOption = episodePage.selectFirst("#playeroptionsul li[data-post][data-nume]")
                    val pId = playerOption?.attr("data-post") ?: ""
                    val num = playerOption?.attr("data-nume") ?: ""
                    val typ = playerOption?.attr("data-type") ?: "tv"
                    Triple(pId, num, typ)
                }
            }
            return fetchAndProcessServers(postId, nume, type)
        } catch (e: Exception) {
            Log.e("FlixLatamProvider", "Error en getServers: ${e.message}", e)
            return emptyList()
        }
    }

    private suspend fun fetchAndProcessServers(postId: String, nume: String, type: String): List<Video.Server> {
        if (postId.isBlank() || nume.isBlank() || type.isBlank()) return emptyList()

        val formBody = FormBody.Builder()
            .add("action", "doo_player_ajax")
            .add("post", postId)
            .add("nume", nume)
            .add("type", type)
            .build()

        val responseJson = service.getPlayerAjax(formBody).string()
        val embedUrl = json.decodeFromString<PlayerResponse>(responseJson).embed_url.replace("\\", "")

        if (!embedUrl.contains("embed")) return emptyList()

        val embedHeaders = mapOf("Referer" to baseUrl)
        val embedDocument = service.getEmbedPage(embedUrl, embedHeaders)

        val scriptData = embedDocument.selectFirst("script:containsData(const dataLink =)")?.data() ?: ""
        val dataLinkJsonString = Regex("""const dataLink\s*=\s*(\[.*?\]);""").find(scriptData)?.groupValues?.get(1) ?: return emptyList()

        return json.decodeFromString<List<DataLinkItem>>(dataLinkJsonString).flatMap { item ->
            item.sortedEmbeds.mapNotNull { embed ->
                CryptoAES.decrypt(embed.link, "Ak7qrvvH4WKYxV2OgaeHAEg2a5eh16vE")?.let { decryptedLink ->
                    Video.Server(
                        id = decryptedLink,
                        name = "${embed.servername.replaceFirstChar { it.titlecase(Locale.ROOT) }} [${item.video_language}]"
                    )
                }
            }
        }.distinctBy { it.id }
    }


    override suspend fun getVideo(server: Video.Server): Video = Extractor.extract(server.id)

    override suspend fun getPeople(id: String, page: Int): People {
        throw Exception("Esta función no está disponible en FlixLatam")
    }

    private fun String.getId(): String = this.removeSuffix("/").substringAfterLast("/")
    private fun String.getBackgroundImage(): String? = this.substringAfter("url(").substringBefore(")")

    private data class ShowDetails(
        val overview: String?, val rating: Double?, val released: String?,
        val genres: List<Genre>, val cast: List<People>
    )

    private fun parseShowDetails(document: Document): ShowDetails {
        val overview = document.selectFirst("#info .wp-content p")?.text()
        val rating = document.selectFirst(".dt_rating_data .dt_rating_vgs")?.text()?.toDoubleOrNull()
        val released = document.selectFirst(".sheader .extra span.date")?.text()

        val genres = document.select(".sgeneros a").map {
            Genre(id = it.attr("href").getId(), name = it.text())
        }
        val cast = document.select("#cast .persons .person").map {
            People(
                id = it.selectFirst("a")?.attr("href")?.getId() ?: "",
                name = it.selectFirst(".name a")?.text() ?: "",
                image = it.selectFirst(".img img")?.attr("src")
            )
        }
        return ShowDetails(overview, rating, released, genres, cast)
    }

    private fun parseShows(elements: List<Element>): List<Show> {
        return elements.mapNotNull {
            val a = it.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            val title = it.selectFirst("h3")?.text() ?: it.selectFirst(".title")?.text() ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.let { img -> img.attr("data-src").ifEmpty { img.attr("src") } }
            val id = href.getId()

            when {
                href.contains("/pelicula/") -> Movie(id = id, title = title, poster = poster)
                href.contains("/serie/") || href.contains("/series/") -> TvShow(id = id, title = title, poster = poster)
                else -> null
            }
        }
    }

    private interface FlixLatamService {
        companion object {
            fun build(baseUrl: String): FlixLatamService {
                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                            .build()
                        chain.proceed(request)
                    }
                    .cache(Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024))
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .apply {
                        val dns = DnsOverHttps.Builder().client(build())
                            .url("https://1.1.1.1/dns-query".toHttpUrl())
                            .build()
                        dns(dns)
                    }
                    .build()

                return Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build()
                    .create(FlixLatamService::class.java)
            }
        }

        @GET
        suspend fun getPage(@Url url: String, @Header("Referer") referer: String): Document

        @POST("/wp-admin/admin-ajax.php")
        @retrofit2.http.Headers("x-requested-with: XMLHttpRequest")
        suspend fun getPlayerAjax(@Body body: FormBody): ResponseBody

        @GET
        suspend fun getEmbedPage(@Url url: String, @HeaderMap headers: Map<String, String>): Document
    }
}