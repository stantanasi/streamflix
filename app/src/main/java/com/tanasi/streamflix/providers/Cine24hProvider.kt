package com.tanasi.streamflix.providers

import android.util.Base64
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
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object Cine24hProvider : Provider {

    override val name = "Cine24h"
    override val baseUrl = "https://cine24h.online"
    override val language = "es"
    override val logo = "https://i.ibb.co/kgjcsFmj/Image-1.png"

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
        .create(Cine24hService::class.java)

    private interface Cine24hService {
        @GET
        suspend fun getPage(@Url url: String): Document
    }

    override suspend fun getHome(): List<Category> = coroutineScope {
        val categories = mutableListOf<Category>()
        val bannerDeferred = async { service.getPage("$baseUrl/release/2025/") }
        val moviesDeferred = async { service.getPage("$baseUrl/estrenos/?type=movies") }
        val tvShowsDeferred = async { service.getPage("$baseUrl/estrenos/?type=series") }

        try {
            val bannerShows = parseShows(bannerDeferred.await()).mapNotNull { show ->
                when (show) {
                    is Movie -> show.copy(poster = null, banner = show.poster)
                    is TvShow -> show.copy(poster = null, banner = show.poster)
                    else -> null
                }
            }
            if (bannerShows.isNotEmpty()) {
                categories.add(Category(Category.FEATURED, bannerShows.take(10)))
            }
        } catch (e: Exception) { /* Do nothing */ }

        try {
            val latestMovies = parseShows(moviesDeferred.await()).filterIsInstance<Movie>()
            if (latestMovies.isNotEmpty()) {
                categories.add(Category("Estrenos de Películas", latestMovies))
            }
        } catch (e: Exception) { /* Do nothing */ }

        try {
            val latestTvShows = parseShows(tvShowsDeferred.await()).filterIsInstance<TvShow>()
            if (latestTvShows.isNotEmpty()) {
                categories.add(Category("Estrenos de Series", latestTvShows))
            }
        } catch (e: Exception) { /* Do nothing */ }

        categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isBlank()) {
            return listOf(
                Genre(id = "category/accion/", name = "Acción"),
                Genre(id = "category/animacion/", name = "Animación"),
                Genre(id = "category/anime/", name = "Anime"),
                Genre(id = "category/aventura/", name = "Aventura"),
                Genre(id = "category/belica/", name = "Bélica"),
                Genre(id = "category/ciencia-ficcion/", name = "Ciencia ficción"),
                Genre(id = "category/comedia/", name = "Comedia"),
                Genre(id = "category/crimen/", name = "Crimen"),
                Genre(id = "category/documental/", name = "Documental"),
                Genre(id = "category/drama/", name = "Drama"),
                Genre(id = "category/familia/", name = "Familia"),
                Genre(id = "category/fantasia/", name = "Fantasía"),
                Genre(id = "category/historia/", name = "Historia"),
                Genre(id = "category/misterio/", name = "Misterio"),
                Genre(id = "category/musica/", name = "Música"),
                Genre(id = "category/romance/", name = "Romance"),
                Genre(id = "category/suspense/", name = "Suspenso"),
                Genre(id = "category/terror/", name = "Terror"),
                Genre(id = "category/western/", name = "Western"),
            )
        }
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val document = service.getPage("$baseUrl/?s=$encodedQuery&paged=$page")
        return parseShows(document)
    }

    private fun parseShows(document: Document): List<AppAdapter.Item> {
        val elements = document.select("main article.TPost, li.TPostMv article.TPost")
        return elements.mapNotNull { article ->
            val linkElement = article.selectFirst("a") ?: return@mapNotNull null
            val url = linkElement.attr("href")
            var title = linkElement.selectFirst("h2, .Title, .text-md")?.text()?.trim()
            if (title.isNullOrEmpty()) return@mapNotNull null
            val imageElement = linkElement.selectFirst(".Image img, figure img")
            val posterUrl = imageElement?.attr("abs:src")?.ifEmpty { imageElement.attr("abs:data-src") } ?: ""

            val lang = linkElement.selectFirst(".language-box .lang-item span")?.text()?.trim()
            if (!lang.isNullOrEmpty()) {
                title += " [$lang]"
            }

            when {
                url.contains("/peliculas/") -> Movie(
                    id = url.substringAfter("/peliculas/").removeSuffix("/"),
                    title = title,
                    poster = posterUrl.replace("/w185/", "/w300/").replace("/w92/", "/w300/")
                )
                url.contains("/series/") -> TvShow(
                    id = url.substringAfter("/series/").removeSuffix("/"),
                    title = title,
                    poster = posterUrl.replace("/w185/", "/w300/").replace("/w92/", "/w300/")
                )
                else -> null
            }
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val document = service.getPage("$baseUrl/peliculas/page/$page")
        return parseShows(document).filterIsInstance<Movie>()
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = service.getPage("$baseUrl/series/page/$page")
        return parseShows(document).filterIsInstance<TvShow>()
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = service.getPage("$baseUrl/${id}page/$page")
        val shows = parseShows(document).filterIsInstance<Show>()
        val genreName = id.removePrefix("category/").removeSuffix("/").replaceFirstChar { it.uppercase() }
        return Genre(id = id, name = genreName, shows = shows)
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getPage("$baseUrl/peliculas/$id")
        val info = document.selectFirst(".TPost footer .Info")
        return Movie(
            id = id,
            title = document.selectFirst(".TPost header .Title")?.text() ?: "",
            overview = document.selectFirst(".TPost .Description")?.text(),
            poster = document.selectFirst(".TPost .Image img")?.attr("abs:src")?.replace("/w185/", "/w500/"),
            rating = info?.selectFirst(".Rank")?.text()?.toDoubleOrNull(),
            released = info?.selectFirst(".Date")?.text(),
            runtime = info?.selectFirst(".Time")?.text()
                ?.replace("h", "")
                ?.replace("m", "")
                ?.trim()
                ?.split(" ")
                ?.let { (it.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (it.getOrNull(1)?.toIntOrNull() ?: 0) },
            genres = document.select(".TPost .Description .Genre a")?.map {
                Genre(id = it.attr("href"), name = it.text())
            } ?: emptyList()
        )
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getPage("$baseUrl/series/$id")
        val info = document.selectFirst(".TPost footer .Info")
        return TvShow(
            id = id,
            title = document.selectFirst(".TPost header .Title")?.text() ?: "",
            overview = document.selectFirst(".TPost .Description")?.text(), // Corrected Selector
            poster = document.selectFirst(".TPost .Image img")?.attr("abs:src")?.replace("/w185/", "/w500/"),
            rating = info?.selectFirst(".Rank")?.text()?.toDoubleOrNull(),
            released = info?.selectFirst(".Date")?.text(),
            genres = document.select(".TPost .Description .Genre a")?.map {
                Genre(id = it.attr("href"), name = it.text())
            } ?: emptyList(),
            seasons = document.select(".AABox").mapNotNull {
                val seasonTitle = it.selectFirst(".Title")?.text() ?: return@mapNotNull null
                val seasonNumber = Regex("""\d+$""").find(seasonTitle)?.value?.toIntOrNull() ?: return@mapNotNull null
                Season(
                    id = "$id/$seasonNumber",
                    number = seasonNumber,
                    title = seasonTitle
                )
            }.sortedByDescending { it.number }
        )
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val (showId, seasonNumber) = seasonId.split("/")
        val document = service.getPage("$baseUrl/series/$showId")

        val seasonBox = document.select(".AABox").find {
            (it.selectFirst(".Title")?.text() ?: "").trim().endsWith(seasonNumber)
        } ?: return emptyList()

        return seasonBox.select(".TPTblCn tr").mapNotNull { row ->
            val titleElement = row.selectFirst(".MvTbTtl a")
            val episodeTitle = titleElement?.text()?.trim()
            val episodeUrl = titleElement?.attr("abs:href")

            if (episodeTitle.isNullOrEmpty() || episodeUrl.isNullOrEmpty()) {
                return@mapNotNull null
            }

            Episode(
                id = episodeUrl,
                number = row.selectFirst(".Num")?.text()?.toIntOrNull() ?: 0,
                title = episodeTitle,
                poster = row.selectFirst(".MvTbImg img")?.attr("abs:src")?.replace("/w154/", "/w300/"),
                released = row.selectFirst(".MvTbTtl span")?.text()
            )
        }.sortedBy { it.number }
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val document = service.getPage(id)

        return document.select("ul.optnslst li[data-src]").mapNotNull {
            val serverInfo = it.selectFirst("button")?.text()?.replace(it.selectFirst(".nmopt")?.text() ?: "", "")?.trim() ?: ""
            val encodedUrl = it.attr("data-src")
            if (encodedUrl.isBlank()) return@mapNotNull null

            val decodedUrl = try {
                String(Base64.decode(encodedUrl, Base64.DEFAULT))
            } catch (e: Exception) {
                return@mapNotNull null
            }

            try {
                val embedDocument = service.getPage(decodedUrl)
                val finalUrl = embedDocument.selectFirst("iframe")?.attr("abs:src") ?: return@mapNotNull null

                val serverName = finalUrl.toHttpUrl().host
                    .replaceFirst("www.", "")
                    .substringBefore(".")
                    .replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }

                Video.Server(
                    id = finalUrl,
                    name = "$serverName ($serverInfo)",
                    src = finalUrl
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return Extractor.extract(server.src, server)
    }

    override suspend fun getPeople(id: String, page: Int): People {
        throw Exception("Not yet implemented")
    }
}