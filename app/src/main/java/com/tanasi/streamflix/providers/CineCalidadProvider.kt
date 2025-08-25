package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.*
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object CineCalidadProvider : Provider {

    override val name = "CineCalidad"
    override val baseUrl = "https://www.cinecalidad.ec"
    override val language = "es"
    override val logo = "https://www.cinecalidad.ec/wp-content/themes/Cinecalidad/assets/img/logo.png"

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

    private interface CineCalidadService {
        @GET
        suspend fun getPage(@Url url: String): Document
    }

    private val client = getOkHttpClient()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JsoupConverterFactory.create())
        .client(client)
        .build()

    private val service = retrofit.create(CineCalidadService::class.java)

    private fun parseShows(elements: List<org.jsoup.nodes.Element>): List<Show> {
        return elements.mapNotNull { element ->
            val a = element.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")

            val img = element.selectFirst("div.poster img") ?: return@mapNotNull null
            val title = img.attr("alt")

            val posterUrl = img.attr("data-src").ifEmpty { img.attr("src") }

            if (href.contains("/ver-pelicula/")) {
                Movie(
                    id = href,
                    title = title,
                    poster = posterUrl
                )
            } else if (href.contains("/ver-serie/")) {
                TvShow(
                    id = href,
                    title = title,
                    poster = posterUrl
                )
            } else {
                null
            }
        }
    }

    override suspend fun getHome(): List<Category> {
        return try {
            coroutineScope {
                val mainPageDeferred = async { service.getPage(baseUrl) }
                val actionPageDeferred = async { service.getPage("$baseUrl/genero-de-la-pelicula/accion/") }
                val comedyPageDeferred = async { service.getPage("$baseUrl/genero-de-la-pelicula/comedia/") }

                val mainDocument = mainPageDeferred.await()
                val actionDocument = actionPageDeferred.await()
                val comedyDocument = comedyPageDeferred.await()

                val categories = mutableListOf<Category>()

                val featuredShows = mainDocument.select("aside#dtw_content_featured-3 li").mapNotNull {
                    val a = it.selectFirst("a") ?: return@mapNotNull null
                    val href = a.attr("href")
                    val title = a.attr("title")
                    val imageUrl = a.selectFirst("img")?.attr("data-src")

                    if (href.contains("/ver-pelicula/")) {
                        Movie(id = href, title = title, banner = imageUrl)
                    } else if (href.contains("/ver-serie/")) {
                        TvShow(id = href, title = title, banner = imageUrl)
                    } else {
                        null
                    }
                }
                if (featuredShows.isNotEmpty()) {
                    categories.add(Category(Category.FEATURED, featuredShows))
                }

                val latestReleases = parseShows(mainDocument.select("article.item[id^=post-]"))
                if (latestReleases.isNotEmpty()) {
                    categories.add(Category("Últimos Estrenos", latestReleases))
                }

                val actionShows = parseShows(actionDocument.select("article.item[id^=post-]"))
                if (actionShows.isNotEmpty()) {
                    categories.add(Category("Acción", actionShows))
                }

                val comedyShows = parseShows(comedyDocument.select("article.item[id^=post-]"))
                if (comedyShows.isNotEmpty()) {
                    categories.add(Category("Comedia", comedyShows))
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
                Genre(id = "genero-de-la-pelicula/accion", name = "Acción"),
                Genre(id = "genero-de-la-pelicula/animacion", name = "Animación"),
                Genre(id = "genero-de-la-pelicula/anime", name = "Anime"),
                Genre(id = "genero-de-la-pelicula/aventura", name = "Aventura"),
                Genre(id = "genero-de-la-pelicula/belica", name = "Bélico"),
                Genre(id = "genero-de-la-pelicula/ciencia-ficcion", name = "Ciencia ficción"),
                Genre(id = "genero-de-la-pelicula/crimen", name = "Crimen"),
                Genre(id = "genero-de-la-pelicula/comedia", name = "Comedia"),
                Genre(id = "genero-de-la-pelicula/documental", name = "Documental"),
                Genre(id = "genero-de-la-pelicula/drama", name = "Drama"),
                Genre(id = "genero-de-la-pelicula/familia", name = "Familiar"),
                Genre(id = "genero-de-la-pelicula/fantasia", name = "Fantasía"),
                Genre(id = "genero-de-la-pelicula/historia", name = "Historia"),
                Genre(id = "genero-de-la-pelicula/musica", name = "Música"),
                Genre(id = "genero-de-la-pelicula/misterio", name = "Misterio"),
                Genre(id = "genero-de-la-pelicula/terror", name = "Terror"),
                Genre(id = "genero-de-la-pelicula/suspense", name = "Suspenso"),
                Genre(id = "genero-de-la-pelicula/romance", name = "Romance"),
                Genre(id = "genero-de-la-pelicula/peliculas-de-dc-comics-online-cinecalidad", name = "Dc Comics"),
                Genre(id = "genero-de-la-pelicula/universo-marvel", name = "Marvel")
            )
        }

        return try {
            val document = service.getPage("$baseUrl/page/$page?s=$query")
            val searchResults = parseShows(document.select("article.item[id^=post-]"))
            searchResults
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        return try {
            val document = service.getPage("$baseUrl/page/$page")
            parseShows(document.select("article.item[id^=post-]"))
                .filterIsInstance<Movie>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        return try {
            val document = service.getPage("$baseUrl/ver-serie/page/$page")
            parseShows(document.select("article.item[id^=post-]"))
                .filterIsInstance<TvShow>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getPage(id)

        val title = document.selectFirst("h1")?.text() ?: ""
        val poster = document.selectFirst(".single_left table img")?.attr("data-src")

        val detailsTd = document.selectFirst(".single_left td[style*=justify]")
        val overview = detailsTd?.selectFirst("p:not(:has(span))")?.text()?.trim()

        val rating = document.selectFirst("span:contains(TMDB) b")?.text()?.trim()?.toDoubleOrNull()

        var genres: List<Genre> = emptyList()
        var cast: List<People> = emptyList()

        val detailsParagraph = detailsTd?.selectFirst("p:has(span:contains(Género))")

        detailsParagraph?.children()?.forEach { span ->
            val text = span.text()
            if (text.startsWith("Género:")) {
                genres = span.select("a").map { a -> Genre(id = a.attr("href"), name = a.text()) }
            }
            if (text.startsWith("Elenco:")) {
                cast = span.select("a").map { a -> People(id = a.attr("href"), name = a.text()) }
            }
        }

        return Movie(
            id = id,
            title = title,
            poster = poster,
            overview = overview,
            rating = rating,
            genres = genres,
            cast = cast
        )
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getPage(id)

        val title = document.selectFirst("h1")?.text() ?: ""
        val poster = document.selectFirst(".single_left table img")?.attr("data-src")

        val detailsTd = document.selectFirst(".single_left td[style*=justify]")

        val overview = detailsTd?.select("p")?.find { p ->
            p.hasText() && p.selectFirst("span") == null
        }?.text()?.trim()

        val rating = document.selectFirst("span:contains(TMDB) b")?.text()?.trim()?.toDoubleOrNull()

        var genres: List<Genre> = emptyList()
        var cast: List<People> = emptyList()

        val detailsParagraph = detailsTd?.selectFirst("p:has(span:contains(Género))")
        detailsParagraph?.children()?.forEach { span ->
            val text = span.text()
            if (text.startsWith("Género:")) {
                genres = span.select("a").map { a -> Genre(id = a.attr("href"), name = a.text()) }
            }
            if (text.startsWith("Elenco:")) {
                cast = span.select("a").map { a -> People(id = a.attr("href"), name = a.text()) }
            }
        }

        val seasons = document.select(".mark-1 .numerando").mapNotNull {
            it.text().substringAfter("S").substringBefore("-").toIntOrNull()
        }.distinct().sorted().map { seasonNumber ->
            Season(
                id = "$id|$seasonNumber",
                number = seasonNumber,
                title = "Temporada $seasonNumber"
            )
        }

        return TvShow(
            id = id,
            title = title,
            poster = poster,
            overview = overview,
            rating = rating,
            genres = genres,
            cast = cast,
            seasons = seasons
        )
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val (showId, seasonNumberStr) = seasonId.split('|')
        val seasonNumber = seasonNumberStr.toInt()
        val document = service.getPage(showId)

        return document.select(".mark-1").filter {
            it.selectFirst(".numerando")?.text()?.startsWith("S$seasonNumber-") == true
        }.map { element ->
            val a = element.selectFirst(".episodiotitle a")
            val numerando = element.selectFirst(".numerando")?.text() ?: ""
            val episodeNumber = numerando.substringAfter("-E").toIntOrNull() ?: 0

            Episode(
                id = a?.attr("href") ?: "",
                number = episodeNumber,
                title = a?.text() ?: "Episodio $episodeNumber",
                poster = element.selectFirst("div.imagen img")?.attr("data-src")
            )
        }
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        return try {
            val document = service.getPage(id)

            document.select("#playeroptionsul li[data-option]")
                .map { element ->
                    val serverUrl = element.attr("data-option")
                    val serverName = element.text()

                    Video.Server(
                        id = serverUrl,
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

    override suspend fun getGenre(id: String, page: Int): Genre {
        return try {
            val document = service.getPage("$baseUrl/$id/page/$page")

            val shows = parseShows(document.select("article.item[id^=post-]"))
            val genreName = id.substringAfterLast("/")
                .replace("-", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            Genre(
                id = id,
                name = genreName,
                shows = shows
            )
        } catch (e: Exception) {
            Genre(id = id, name = "Error", shows = emptyList())
        }
    }

    override suspend fun getPeople(id: String, page: Int): People {
        throw Exception("getPeople not implemented")
    }
}