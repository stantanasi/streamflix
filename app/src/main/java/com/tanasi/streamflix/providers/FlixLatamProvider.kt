package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.*
import okhttp3.Cache
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jsoup.nodes.Element
import com.tanasi.streamflix.models.flixlatam.*
import com.tanasi.streamflix.utils.CryptoAES
import com.google.gson.Gson
import android.util.Log
import org.jsoup.Jsoup
import retrofit2.http.HeaderMap

object FlixLatamProvider : Provider {

    override val name = "FlixLatam"
    override val baseUrl = "https://flixlatam.com"
    override val language = "es"

    private val client = getOkHttpClient()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JsoupConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    private val service = retrofit.create(FlixLatamService::class.java)

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

    private interface FlixLatamService {
        @GET
        suspend fun getPage(@Url url: String): Document

        @POST("/wp-admin/admin-ajax.php")
        @Headers("x-requested-with: XMLHttpRequest")
        suspend fun getPlayerAjax(@Body body: FormBody): ResponseBody

        @POST("/wp-admin/admin-ajax.php")
        @Headers("x-requested-with: XMLHttpRequest")
        suspend fun getEpisodesAjax(@Body body: FormBody): ResponseBody

        @GET
        suspend fun getEmbedPage(
            @Url url: String,
            @HeaderMap headers: Map<String, String>
        ): Document
    }

    private fun parseShows(elements: List<Element>): List<Show> {
        return elements.mapNotNull {
            val a = it.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")

            val title = it.selectFirst("h3")?.text()
                ?: it.selectFirst(".title")?.text()
                ?: return@mapNotNull null

            // La imagen puede estar en 'src' o 'data-src'
            val poster = it.selectFirst("img")?.let { img ->
                img.attr("data-src").ifEmpty { img.attr("src") }
            }

            // El ID en DooPlay es la última parte de la URL (el "slug")
            val id = href.removeSuffix("/").substringAfterLast("/")

            when {
                href.contains("/pelicula/") -> Movie(id = id, title = title, poster = poster)
                href.contains("/serie/") -> TvShow(id = id, title = title, poster = poster)
                else -> null
            }
        }
    }

    override suspend fun getHome(): List<Category> {
        return try {
            val document = service.getPage(baseUrl)
            val categories = mutableListOf<Category>()

            // 1. Banner / Destacados (FEATURED)
            val featuredShows = document.select("#slider-movies-tvshows .item").mapNotNull {
                val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val banner = it.selectFirst("img")?.attr("src")
                val title = it.selectFirst(".data h3")?.text()
                val id = href.removeSuffix("/").substringAfterLast("/")
                val type = it.selectFirst("span.item_type")?.text()

                if (type == "TV" || href.contains("/serie/")) {
                    TvShow(id = id, title = title ?: "", banner = banner)
                } else {
                    Movie(id = id, title = title ?: "", banner = banner)
                }
            }
            if (featuredShows.isNotEmpty()) {
                categories.add(Category(Category.FEATURED, featuredShows))
            }

            // 2. TOP Películas (de la barra lateral)
            val topMovies = document.select(".top-imdb-list.tleft .top-imdb-item").mapNotNull {
                val a = it.selectFirst("a") ?: return@mapNotNull null
                val href = a.attr("href")
                val title = it.selectFirst(".title a")?.text() ?: return@mapNotNull null
                val poster = it.selectFirst("img")?.attr("src")
                val id = href.removeSuffix("/").substringAfterLast("/")
                Movie(id = id, title = title, poster = poster)
            }
            if (topMovies.isNotEmpty()) {
                categories.add(Category("TOP Películas", topMovies))
            }

            // 3. TOP Series (de la barra lateral)
            val topTvShows = document.select(".top-imdb-list.tright .top-imdb-item").mapNotNull {
                val a = it.selectFirst("a") ?: return@mapNotNull null
                val href = a.attr("href")
                val title = it.selectFirst(".title a")?.text() ?: return@mapNotNull null
                val poster = it.selectFirst("img")?.attr("src")
                val id = href.removeSuffix("/").substringAfterLast("/")
                TvShow(id = id, title = title, poster = poster)
            }
            if (topTvShows.isNotEmpty()) {
                categories.add(Category("TOP Series", topTvShows))
            }

            categories
        } catch (e: Exception) {
            Log.e("FLIXLATAM_ERROR", "Error en getHome", e)
            emptyList()
        }
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isBlank()) {
            return listOf(
                Genre(id = "accion", name = "Acción"),
                Genre(id = "action-adventure", name = "Action & Adventure"),
                Genre(id = "adolescencia", name = "Adolescencia"),
                Genre(id = "animacion", name = "Animación"),
                Genre(id = "anime", name = "Anime"),
                Genre(id = "artes-marciales", name = "Artes Marciales"),
                Genre(id = "aventura", name = "Aventura"),
                Genre(id = "aventuras", name = "Aventuras"),
                Genre(id = "belica", name = "Bélica"),
                Genre(id = "belico", name = "Bélico"),
                Genre(id = "ciencia-ficcion", name = "Ciencia Ficción"),
                Genre(id = "comedia", name = "Comedia"),
                Genre(id = "comedia-de-situacion", name = "Comedia de Situación"),
                Genre(id = "crimen", name = "Crimen"),
                Genre(id = "deporte", name = "Deporte"),
                Genre(id = "dibujo-animado", name = "Dibujo Animado"),
                Genre(id = "documental", name = "Documental"),
                Genre(id = "drama", name = "Drama"),
                Genre(id = "drama-adolescente", name = "Drama Adolescente"),
                Genre(id = "drama-medico", name = "Drama Médico"),
                Genre(id = "familia", name = "Familia"),
                Genre(id = "fantasia", name = "Fantasía"),
                Genre(id = "ficcion-historica", name = "Ficción Histórica"),
                Genre(id = "historia", name = "Historia"),
                Genre(id = "kids", name = "Kids"),
                Genre(id = "misterio", name = "Misterio"),
                Genre(id = "musica", name = "Música"),
                Genre(id = "pelicula-de-tv", name = "Película de TV"),
                Genre(id = "reality", name = "Reality"),
                Genre(id = "romance", name = "Romance"),
                Genre(id = "sci-fi-fantasy", name = "Sci-Fi & Fantasy"),
                Genre(id = "soap", name = "Soap"),
                Genre(id = "talk", name = "Talk"),
                Genre(id = "telenovela", name = "Telenovela"),
                Genre(id = "terror", name = "Terror"),
                Genre(id = "war-politics", name = "War & Politics"),
                Genre(id = "western", name = "Western"),
            )
        }
        return try {
            val document = service.getPage("$baseUrl/page/$page?s=$query")
            // Usamos un selector genérico para los resultados de búsqueda de DooPlay
            parseShows(document.select("div.search-page article, div.items article"))
        } catch (e: Exception) {
            Log.e("FLIXLATAM_ERROR", "Error in search", e)
            emptyList()
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        return try {
            val document = service.getPage("$baseUrl/pelicula/page/$page")
            parseShows(document.select("div.items article"))
                .filterIsInstance<Movie>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        return try {
            val document = service.getPage("$baseUrl/series/page/$page")
            parseShows(document.select("div.items article"))
                .filterIsInstance<TvShow>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        return try {
            val document = service.getPage("$baseUrl/genero/$id/page/$page")
            val shows = parseShows(document.select("div.items article"))
            val genreName = document.selectFirst("h1.Title")?.text() ?: id.replaceFirstChar { it.uppercase() }
            Genre(
                id = id,
                name = genreName,
                shows = shows
            )
        } catch (e: Exception) {
            Genre(id = id, name = id.replaceFirstChar { it.uppercase() })
        }
    }

    override suspend fun getPeople(id: String, page: Int): People {
        throw Exception("Esta función no está disponible en FlixLatam")
    }

    override suspend fun getMovie(id: String): Movie {
        return try {
            val document = service.getPage("$baseUrl/pelicula/$id/")

            val title = document.selectFirst(".sheader .data h1")?.text() ?: ""
            val poster = document.selectFirst(".sheader .poster img")?.attr("src")
            val banner = document.selectFirst("style:containsData(background-image)")
                ?.data()
                ?.substringAfter("url(")
                ?.substringBefore(")")

            val overview = document.selectFirst("#info .wp-content p")?.text()
            val rating = document.selectFirst(".dt_rating_data .dt_rating_vgs")?.text()?.toDoubleOrNull()
            val released = document.selectFirst(".sheader .extra span.date")?.text()

            val genres = document.select(".sgeneros a").map {
                Genre(
                    id = it.attr("href").removeSuffix("/").substringAfterLast("/"),
                    name = it.text()
                )
            }

            val cast = document.select("#cast .persons .person").map {
                People(
                    id = it.selectFirst("a")?.attr("href")?.removeSuffix("/")?.substringAfterLast("/") ?: "",
                    name = it.selectFirst(".name a")?.text() ?: "",
                    image = it.selectFirst(".img img")?.attr("src")
                )
            }

            val recommendations = parseShows(document.select("#single_relacionados article"))

            Movie(
                id = id,
                title = title,
                poster = poster,
                banner = banner,
                overview = overview,
                rating = rating,
                released = released,
                genres = genres,
                cast = cast,
                recommendations = recommendations
            )
        } catch (e: Exception) {
            Log.e("FLIXLATAM_ERROR", "Error in getMovie", e)
            Movie(id = id, title = "")
        }
    }

    override suspend fun getTvShow(id: String): TvShow {
        return try {
            val document = service.getPage("$baseUrl/serie/$id/")

            // ... (toda la extracción de title, poster, banner, etc., se queda igual)
            val title = document.selectFirst(".sheader .data h1")?.text() ?: ""
            val poster = document.selectFirst(".sheader .poster img")?.attr("src")
            val banner = document.selectFirst("style:containsData(background-image)")
                ?.data()
                ?.substringAfter("url(")
                ?.substringBefore(")")

            val overview = document.selectFirst("#info .wp-content p")?.text()
            val rating = document.selectFirst(".dt_rating_data .dt_rating_vgs")?.text()?.toDoubleOrNull()
            val released = document.selectFirst(".sheader .extra span.date")?.text()

            val genres = document.select(".sgeneros a").map {
                Genre(
                    id = it.attr("href").removeSuffix("/").substringAfterLast("/"),
                    name = it.text()
                )
            }

            val cast = document.select("#cast .persons .person").map {
                People(
                    id = it.selectFirst("a")?.attr("href")?.removeSuffix("/")?.substringAfterLast("/") ?: "",
                    name = it.selectFirst(".name a")?.text() ?: "",
                    image = it.selectFirst(".img img")?.attr("src")
                )
            }

            val recommendations = parseShows(document.select("#single_relacionados article"))

            val seasons = document.select("#seasons .se-c").mapNotNull { seasonElement ->
                val seasonNumber = seasonElement.selectFirst(".se-q span.se-t")?.text()?.toIntOrNull() ?: return@mapNotNull null

                val episodes = seasonElement.select(".se-a ul.episodios li").mapNotNull { episodeElement ->
                    val a = episodeElement.selectFirst("a") ?: return@mapNotNull null
                    val href = a.attr("href")
                    val posterUrl = episodeElement.selectFirst(".imagen img")?.attr("src")
                    val title = episodeElement.selectFirst(".episodiotitle a")?.text() ?: ""
                    val numberStr = episodeElement.selectFirst(".numerando")?.text()
                        ?.trim()?.split("-")?.getOrNull(1)?.trim()
                    val number = numberStr?.toIntOrNull() ?: 0

                    Episode(
                        id = href.removeSuffix("/").substringAfterLast("/"),
                        title = title,
                        number = number,
                        poster = posterUrl
                    )
                }

                Season(
                    id = "$id|$seasonNumber",
                    number = seasonNumber,
                    title = "Temporada $seasonNumber",
                    episodes = episodes // <-- ¡CORREGIDO! Ya no se invierte la lista.
                )
            }

            TvShow(
                id = id,
                title = title,
                poster = poster,
                banner = banner,
                overview = overview,
                rating = rating,
                released = released,
                genres = genres,
                cast = cast,
                recommendations = recommendations,
                seasons = seasons
            )
        } catch (e: Exception) {
            Log.e("FLIXLATAM_ERROR", "Error in getTvShow", e)
            TvShow(id = id, title = "")
        }
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        return try {
            // seasonId ahora es "dragon-ball-daima|1"
            val showId = seasonId.substringBefore("|")
            val seasonNumber = seasonId.substringAfter("|").toInt()

            // Re-obtenemos la información de la serie, que ya contiene todos los episodios.
            val show = getTvShow(showId)

            // Buscamos la temporada correcta y devolvemos sus episodios.
            show.seasons.find { it.number == seasonNumber }?.episodes ?: emptyList()
        } catch (e: Exception) {
            Log.e("FLIXLATAM_ERROR", "Error in getEpisodesBySeason", e)
            emptyList()
        }
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        return try {
            // Para las series, el 'id' del episodio no nos sirve. Necesitamos el de la serie.
            // Afortunadamente, lo tenemos disponible en el objeto videoType.
            val pageUrl = when (videoType) {
                is Video.Type.Movie -> "$baseUrl/pelicula/$id/"
                is Video.Type.Episode -> "$baseUrl/serie/${videoType.tvShow.id}/"
            }
            val document = service.getPage(pageUrl)

            // Buscamos el ID del post, que es el que necesitamos para la llamada AJAX
            val postId = document.body().className().substringAfter("postid-").substringBefore(" ")

            // Para un episodio, necesitamos el 'data-nume' específico de ese episodio
            val nume = when (videoType) {
                is Video.Type.Movie -> "1" // Las películas suelen tener el nume "1"
                is Video.Type.Episode -> {
                    // Buscamos el episodio correcto en el HTML de la página de la serie
                    document.selectFirst("#seasons .se-c .se-a li:contains(E${videoType.number})")
                        ?.attr("data-id") ?: ""
                }
            }

            if (postId.isBlank() || nume.isBlank()) {
                return emptyList()
            }

            val body = FormBody.Builder()
                .add("action", "doo_player_ajax")
                .add("post", postId)
                .add("nume", nume)
                .add("type", if (videoType is Video.Type.Movie) "movie" else "tv")
                .build()

            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val playerResponseJson = service.getPlayerAjax(body).string()
            val playerResponse = json.decodeFromString<PlayerResponse>(playerResponseJson)
            val embedUrl = playerResponse.embed_url.replace("\\", "")

            if (embedUrl.isBlank() || !embedUrl.contains("embed69")) return emptyList()

            val embedHeaders = mapOf(
                "Referer" to baseUrl,
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
            )
            val embedDocument = service.getEmbedPage(embedUrl, embedHeaders)

            val dataLinkScript = embedDocument.selectFirst("script:containsData(const dataLink =)")?.data()
            val dataLinkJsonString = Regex("""const dataLink\s*=\s*(\[.*?\]);""").find(dataLinkScript ?: "")?.groupValues?.get(1)
                ?: return emptyList()

            val dataLinkItems = json.decodeFromString<List<DataLinkItem>>(dataLinkJsonString)
            dataLinkItems.flatMap { item ->
                val language = item.video_language
                item.sortedEmbeds.mapNotNull { embed ->
                    val decryptedLink = CryptoAES.decrypt(embed.link, "Ak7qrvvH4WKYxV2OgaeHAEg2a5eh16vE")
                    if (decryptedLink.isNotBlank()) {
                        Video.Server(
                            id = decryptedLink,
                            name = "${embed.servername.replaceFirstChar { it.titlecase() }} [$language]"
                        )
                    } else {
                        null
                    }
                }
            }.distinctBy { it.id }
        } catch (e: Exception) {
            Log.e("FLIXLATAM_ERROR", "Error in getServers", e)
            emptyList()
        }
    }

    override suspend fun getVideo(server: Video.Server): Video = Extractor.extract(server.id)

    override val logo: String = "$baseUrl/wp-content/uploads/2022/04/cropped-Series-Latinoamerica.jpg"
}