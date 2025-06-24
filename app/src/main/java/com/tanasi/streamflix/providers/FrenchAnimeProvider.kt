package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

object FrenchAnimeProvider : Provider {

    private const val URL = "https://french-anime.com/"
    override val baseUrl = URL
    override val name = "FrenchAnime"
    override val logo = "$URL/templates/franime/images/favicon3.png"
    override val language = "fr"

    private val service = FrenchAnimeService.build()

    // Flag to track if more search results are available. Set to false when API returns fewer items than requested.
    // This prevents querying non-existent pages that could return random/incorrect results.
    private var hasMore = true
    private const val SEARCH_PAGE_SIZE = 10
    private val URL_DOMAIN_REGEX = Regex("""(?:https?:)?//(?:www\.)?([^.]+)\.""")

    override suspend fun getHome(): List<Category> {
        val document = service.getHome()
        val categories = mutableListOf<Category>()

        document.select(".owl-carousel .item").map { item ->
            val a = item.selectFirst("a") ?: return@map null
            val link = a.attr("href")

            TvShow(
                id = link.substringAfterLast("/").substringBefore(".html"),
                title = a.selectFirst(".title1")?.text() ?: "",
                overview = a.selectFirst(".title0")?.text(),
                banner = a.selectFirst("img")?.attr("src")?.toUrl()
            )
        }.filterNotNull().takeIf { it.isNotEmpty() }?.let { featuredItems ->
            categories.add(Category(name = Category.FEATURED, list = featuredItems))
        }

        document.select(".block-main").forEach { block ->
            val categoryName = block.selectFirst(".block-title .left-ma")?.text() ?: ""
            block.select(".mov").mapNotNull { mov ->
                val a = mov.selectFirst("a.mov-t") ?: return@mapNotNull null
                val link = a.attr("href")
                val id = link.substringAfterLast("/").substringBefore(".html")
                val isFrench = (mov.selectFirst(".block-sai")?.text()
                    ?: mov.selectFirst(".nbloc1")?.text()).isFrench()
                val title = if (categoryName.contains("FILMS", ignoreCase = true))
                    a.text().toTitle(isFrench) else a.text()
                val poster = mov.selectFirst(".mov-i img")?.attr("src")?.toUrl() ?: ""

                val isTvShow = mov.selectFirst(".block-ep") != null
                if (isTvShow) {
                    val seasonText = mov.selectFirst(".block-sai")?.text() ?: ""
                    val seasonNumber = seasonText.split(" ").getOrNull(1)?.toIntOrNull() ?: 0
                    val episodeText = mov.selectFirst(".block-ep")?.text() ?: ""
                    // Handle cases like "Episode 13 et 14 Final" by taking the last number
                    val episodeNumberText = episodeText.split(" ")
                        .lastOrNull { it.toIntOrNull() != null }
                    val episodeNumber = episodeNumberText?.toIntOrNull() ?: 0

                    TvShow(
                        id = id,
                        title = title,
                        poster = poster,
                        seasons = if (seasonNumber > 0 && episodeNumber > 0) {
                            listOf(
                                Season(
                                    id = "",
                                    number = seasonNumber,
                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = episodeNumber
                                        )
                                    )
                                )
                            )
                        } else {
                            emptyList()
                        }
                    )
                } else {
                    Movie(
                        id = id,
                        title = title,
                        poster = poster
                    )
                }
            }.takeIf { it.isNotEmpty() }?.let { items ->
                categories.add(Category(name = categoryName, list = items))
            }
        }

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getHome()

            val genres = document.select("div.side-b nav.side-c ul.flex-row li a").map {
                Genre(
                    id = it.attr("href").substringBeforeLast("/").substringAfterLast("/"),
                    name = it.text()
                )
            }

            return genres
        }

        if (page > 1 && !hasMore) return emptyList()

        val document = service.search(
            query = query,
            searchStart = page,
            resultFrom = if (page > 1) (page - 1) * SEARCH_PAGE_SIZE + 1 else -1
        )

        document.selectFirst("div.berrors")?.let(::checkHasMore)

        val results = document.select("div.mov").mapNotNull { mov ->
            val a = mov.selectFirst("a.mov-t") ?: return@mapNotNull null
            val link = a.attr("href")
            val id = link.substringAfterLast("/").substringBefore(".html")
            val isFrench = (mov.selectFirst(".block-sai")?.text() ?: mov.selectFirst(".nbloc1")
                ?.text()).isFrench()
            val title = a.text().toTitle(isFrench)
            val poster = mov.selectFirst(".mov-i img")?.attr("src")?.toUrl()

            val isTvShow = mov.selectFirst(".block-ep") != null
            if (isTvShow) {
                TvShow(
                    id = id,
                    title = title,
                    poster = poster,
                )
            } else {
                Movie(
                    id = id,
                    title = title,
                    poster = poster
                )
            }
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val document = try {
            service.getMovies(page)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> return emptyList()
                else -> throw e
            }
        }

        val movies = document.select("div.mov.clearfix").mapNotNull { mov ->
            val a = mov.selectFirst("a.mov-t") ?: return@mapNotNull null
            val link = a.attr("href")
            val isFrench = mov.selectFirst(".nbloc1")?.text().isFrench()

            Movie(
                id = link.substringAfterLast("/").substringBefore(".html"),
                title = a.text().toTitle(isFrench),
                poster = mov.selectFirst(".mov-i img")?.attr("src")?.toUrl()
            )
        }

        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> = withContext(Dispatchers.IO) {
        val vfDeferred = async { fetchTvShows("animes-vf/page/$page/") }
        val vostfrDeferred = async { fetchTvShows("animes-vostfr/page/$page/") }

        val vfTvShows = vfDeferred.await()
        val vostfrTvShows = vostfrDeferred.await()

        (vfTvShows + vostfrTvShows).distinctBy { it.id }
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getMovie(id)

        val isFrench = document.selectFirst("ul.mov-list li:contains(Version) .mov-desc span")
            ?.text().isFrench()

        val movie = Movie(
            id = id,
            title = document.selectFirst("header.full-title h1")?.text()?.toTitle(isFrench) ?: "",
            overview = document.selectFirst("span[itemprop='description']")?.text() ?: "",
            released = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text() == "Date de sortie:" }
                ?.selectFirst("div.mov-desc")?.text()?.substringBefore(" to"),
            runtime = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text() == "Durée:" }
                ?.selectFirst("div.mov-desc")?.text()?.extractRuntime(),
            poster = document.selectFirst("div.mov-img img[itemprop='thumbnailUrl']")
                ?.attr("src")?.toUrl(),
            genres = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text() == "GENRE:" }
                ?.select("span[itemprop='genre'] a")?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/").substringBefore("/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            directors = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text() == "RÉALISATEUR:" }
                ?.selectFirst("div.mov-desc span[itemprop='name']")?.text()
                .toPeople(),
            cast = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text() == "ACTEURS:" }
                ?.selectFirst("div.mov-desc span[itemprop='name']")?.text()
                .toPeople()
        )

        return movie
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTvShow(id)

        val isFrench = document.selectFirst("ul.mov-list li:contains(Version) .mov-desc span")
            ?.text().isFrench()

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("h1[itemprop=name]")?.text()?.toTitle(isFrench) ?: "",
            overview = document.selectFirst("span[itemprop=description]")?.text(),
            released = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text() == "Date de sortie:" }
                ?.selectFirst("div.mov-desc")?.text()?.substringBefore(" to"),
            runtime = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text() == "Durée:" }
                ?.selectFirst("div.mov-desc")?.text()?.extractRuntime(),
            poster = document.selectFirst("div.mov-img img[itemprop=thumbnailUrl]")?.attr("src")
                ?.toUrl(),
            seasons = listOf(
                Season(
                    id = id,
                    number = 0,
                    title = "Episodes",
                )
            ),
            genres = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text() == "GENRE:" }
                ?.select("span[itemprop='genre'] a")?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/").substringBefore("/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            directors = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text() == "RÉALISATEUR:" }
                ?.selectFirst("div.mov-desc span[itemprop='name']")?.text()
                .toPeople(),
            cast = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text() == "ACTEURS:" }
                ?.selectFirst("div.mov-desc span[itemprop='name']")?.text()
                .toPeople()
        )

        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val document = service.getTvShow(seasonId)

        val epsText = document.selectFirst("div.eps")?.text() ?: return emptyList()
        val episodeLines = epsText.split(" ")
        val episodes = episodeLines.mapNotNull { line ->
            val parts = line.split("!")
            if (parts.size == 2) {
                val episodeNumber = parts[0].toIntOrNull() ?: return@mapNotNull null
                Episode(
                    id = "${seasonId}_${episodeNumber}",
                    number = episodeNumber
                )
            } else {
                null
            }
        }

        return episodes
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = try {
            service.getGenre(id, page)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> return Genre(id, "")
                else -> throw e
            }
        }

        val genreName = document.title().substringBefore(" - French Anime").substringBefore(" »")

        val shows = document.select("div.mov").mapNotNull { mov ->
            val a = mov.selectFirst("a.mov-t") ?: return@mapNotNull null
            val link = a.attr("href")
            val itemId = link.substringAfterLast("/").substringBefore(".html")
            val isFrench = (mov.selectFirst(".block-sai")?.text() ?: mov.selectFirst(".nbloc1")
                ?.text()).isFrench()
            val title = a.text().toTitle(isFrench)
            val poster = mov.selectFirst(".mov-i img")?.attr("src")?.toUrl()

            val isTvShow = mov.selectFirst(".block-ep") != null
            if (isTvShow) {
                TvShow(
                    id = itemId,
                    title = title,
                    poster = poster
                )
            } else {
                Movie(
                    id = itemId,
                    title = title,
                    poster = poster
                )
            }
        }

        return Genre(id = id, name = genreName, shows = shows)
    }

    override suspend fun getPeople(id: String, page: Int): People {
        if (page > 1 && !hasMore) return People(id = id, name = id)

        val document = service.getPeople(
            query = id,
            searchStart = page,
            resultFrom = if (page > 1) (page - 1) * SEARCH_PAGE_SIZE + 1 else -1
        )

        document.selectFirst("div.berrors")?.let(::checkHasMore)

        val filmography = document.select("div.mov").mapNotNull { mov ->
            val a = mov.selectFirst("a.mov-t") ?: return@mapNotNull null
            val link = a.attr("href")
            val itemId = link.substringAfterLast("/").substringBefore(".html")
            val isFrench = (mov.selectFirst(".block-sai")?.text() ?: mov.selectFirst(".nbloc1")
                ?.text()).isFrench()
            val title = a.text().toTitle(isFrench)
            val poster = mov.selectFirst(".mov-i img")?.attr("src")?.toUrl()

            val isTvShow = mov.selectFirst(".block-ep") != null
            if (isTvShow) {
                TvShow(
                    id = itemId,
                    title = title,
                    poster = poster,
                )
            } else {
                Movie(
                    id = itemId,
                    title = title,
                    poster = poster
                )
            }
        }

        return People(id = id, name = id, filmography = filmography)
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val episodeId = when (videoType) {
            is Video.Type.Movie -> id
            is Video.Type.Episode -> id.substringBeforeLast("_")
        }

        val document = service.getTvShow(episodeId)

        val epsText = document.selectFirst("div.eps")?.text() ?: return emptyList()
        val episodeLines = epsText.split(" ")
        val idSuffix = id.substringAfterLast("_")
        val servers = episodeLines.firstNotNullOfOrNull { line ->
            val parts = line.split("!")
            if (parts.size == 2 && (parts[0] == idSuffix || videoType is Video.Type.Movie)) {
                parts[1].split(",").filter { it.isNotEmpty() && it.startsWith("http") }
                    .mapIndexed { index, source ->
                        Video.Server(
                            id = index.toString(),
                            name = source.extractUrlDomain(),
                            src = source
                        )
                    }
            } else {
                null
            }
        } ?: emptyList()

        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val video = Extractor.extract(server.src)

        return video
    }

    private suspend fun fetchTvShows(path: String): List<TvShow> {
        return try {
            service.getTvSeries(path).let { document ->
                document.select("div.mov.clearfix").mapNotNull { extractTvShows(it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractTvShows(mov: Element): TvShow? {
        val a = mov.selectFirst("a.mov-t") ?: return null
        val link = a.attr("href")
        val id = link.substringAfterLast("/").substringBefore(".html")
        val isFrench = (mov.selectFirst(".block-sai")?.text() ?: mov.selectFirst(".nbloc1")
            ?.text()).isFrench()
        val title = a.text().toTitle(isFrench)
        val poster = mov.selectFirst(".mov-i img")?.attr("src")?.toUrl() ?: ""
        val episodeText = mov.selectFirst(".block-ep")?.text() ?: return null
        val episodeNumberText = episodeText.split(" ").lastOrNull { it.toIntOrNull() != null }
        val episodeNumber = episodeNumberText?.toIntOrNull() ?: 0
        val seasonText = mov.selectFirst(".block-sai")?.text() ?: ""
        val seasonNumber = seasonText.split(" ").getOrNull(1)?.toIntOrNull() ?: 0

        return TvShow(
            id = id,
            title = title,
            poster = poster,
            seasons = if (seasonNumber > 0 && episodeNumber > 0) {
                listOf(
                    Season(
                        id = "",
                        number = seasonNumber,
                        episodes = listOf(
                            Episode(
                                id = "",
                                number = episodeNumber
                            )
                        )
                    )
                )
            } else {
                emptyList()
            }
        )
    }

    private fun checkHasMore(berrorsDiv: Element) {
        val resultText = berrorsDiv.text()
        val totalResults = resultText.substringAfter("Trouvé ")
            .substringBefore(" réponses").toIntOrNull() ?: 0
        val currentRange = resultText.substringAfter("Résultats de la requête ")
            .substringBefore(")").split(" - ")
        val receivedItems = currentRange.getOrNull(1)?.toIntOrNull() ?: 0

        hasMore = receivedItems < totalResults
    }

    private fun String.toUrl(): String = if (startsWith("/")) URL.dropLast(1).plus(this) else this

    private fun String?.isFrench(): Boolean = this?.contains("FRENCH", ignoreCase = true) ?: false

    private fun String.toTitle(isFrench: Boolean): String = if (isFrench) {
        "(FR) $this".replace(" FRENCH", "")
    } else {
        this.replace(" VOSTFR", "")
    }

    private fun String?.toPeople(): List<People> {
        return this?.split(", ")
            ?.map { it.replace(("[^\\p{L}\\d ]").toRegex(), " ").trim() }
            ?.filter { it.isNotEmpty() }
            ?.map {
                People(
                    id = it,
                    name = it,
                    image = "",
                )
            } ?: listOf()
    }

    private fun String.extractUrlDomain(): String {
        return URL_DOMAIN_REGEX.find(this)?.groupValues?.get(1)?.replaceFirstChar { it.uppercase() }
            ?: this
    }

    private fun String.extractRuntime() = when {
        contains("h") -> {
            val hours = substringBefore("h").toIntOrNull() ?: 0
            val minutes = substringAfter("h ").substringBefore("min").toIntOrNull() ?: 0
            hours * 60 + minutes
        }
        contains("min") -> substringBefore(" min").toIntOrNull()
        contains("mn") -> substringBefore(" mn").toIntOrNull()
        else -> null
    }

    private interface FrenchAnimeService {
        companion object {
            fun build(): FrenchAnimeService {
                val client = OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(FrenchAnimeService::class.java)
            }
        }

        @GET(".")
        suspend fun getHome(): Document

        @POST(".")
        @FormUrlEncoded
        suspend fun search(
            @Field("do") doAction: String = "search",
            @Field("subaction") subAction: String = "search",
            @Field("story") query: String,
            @Field("search_start") searchStart: Int = -1,
            @Field("result_from") resultFrom: Int = -1,
            @Field("full_search") fullSearch: Int = 0,
//            @Field("titleonly") titleOnly: Int = 3, // Narrow to title search
        ): Document

        @GET("films-vf-vostfr/page/{page}")
        suspend fun getMovies(@Path("page") page: Int): Document

        @GET("{path}")
        suspend fun getTvSeries(@Path("path") path: String): Document

        @GET("films-vf-vostfr/{id}.html")
        suspend fun getMovie(@Path("id") id: String): Document

        @GET("{id}.html")
        suspend fun getTvShow(@Path("id") id: String): Document

        @GET("genre/{genre}/page/{page}")
        suspend fun getGenre(
            @Path("genre") genre: String,
            @Path("page") page: Int
        ): Document

        @POST(".")
        @FormUrlEncoded
        suspend fun getPeople(
            @Field("do") doAction: String = "search",
            @Field("subaction") subAction: String = "search",
            @Field("story") query: String,
            @Field("search_start") searchStart: Int = -1,
            @Field("result_from") resultFrom: Int = -1,
            @Field("full_search") fullSearch: Int = 0
        ): Document
    }
}