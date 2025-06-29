package com.tanasi.streamflix.providers

import MyCookieJar
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
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object AnimeWorldProvider : Provider {

    private const val URL = "https://animeworld.so/"
    override val baseUrl = URL
    private const val USER_AGENT = "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)"

    override val name = "AnimeWorld"
    override val logo = "https://static.animeworld.so/assets/images/favicon/android-icon-192x192.png"
    override val language = "it"

    private val service = AnimeWorldService.build()


    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = Category.FEATURED,
                list = document.select("#swiper-container > div.swiper-wrapper div.swiper-slide").map {
                    val id = it.selectFirst("a")
                        ?.attr("href")?.substringAfterLast("/") ?: ""
                    val title = it.selectFirst("a")
                        ?.text() ?: ""
                    val overview = it.selectFirst("p")
                        ?.text()
                    val banner = it
                        .attr("style")
                        .substringAfter("url(").substringBefore(")")

                    TvShow(
                        id = id,
                        title = title,
                        overview = overview,
                        banner = banner,
                    )
                }.distinctBy { it.title },
            )
        )

        categories.addAll(
            document.select("#main div.widget.hotnew > div.widget-body > div:nth-child(4)").map { block ->
                Category(
                    name = "Tendenze",
                    list = block.select("div.item").map {
                        val id = it.selectFirst("a")
                            ?.attr("href")
                            ?.substringBeforeLast("/")
                            ?.substringAfterLast("/") ?: ""
                        val title = it.selectFirst("a.name")
                            ?.text() ?: ""
                        val poster = it.selectFirst("img")
                            ?.attr("src")

                        val isMovie = it.selectFirst("div.status > div.movie")
                            ?.text() == "Movie"

                        if (isMovie) {
                            Movie(
                                id = id,
                                title = title,
                                poster = poster,
                            )
                        }
                        else {
                            TvShow(
                                id = id,
                                title = title,
                                poster = poster,
                            )
                        }
                    }.distinctBy { when (it) {
                        is Movie ->  it.title
                        is TvShow -> it.title
                    } }
                )
            }
        )

        categories.addAll(
            document.select("#main div.widget.hotnew > div.widget-body > div:nth-child(3)").map { block ->
                Category(
                    name = "Ultimi episodi (Doppiati)",
                    list = block.select("div.item").map {
                        val id = it.selectFirst("a")
                            ?.attr("href")
                            ?.substringBeforeLast("/")
                            ?.substringAfterLast("/") ?: ""
                        val title = it.selectFirst("a.name")
                            ?.text() ?: ""
                        val poster = it.selectFirst("img")
                            ?.attr("src")

                        val isMovie = it.selectFirst("div.status > div.movie")
                            ?.text() == "Movie"

                        if (isMovie) {
                            Movie(
                                id = id,
                                title = title,
                                poster = poster,
                            )
                        }
                        else {
                            TvShow(
                                id = id,
                                title = title,
                                poster = poster,
                            )
                        }
                    }.distinctBy { when (it) {
                        is Movie ->  it.title
                        is TvShow -> it.title
                    } }
                )
            }
        )

        categories.addAll(
            document.select("#main div.widget.hotnew > div.widget-body > div:nth-child(2)").map { block ->
                Category(
                    name = "Ultimi episodi (Sottotitolati)",
                    list = block.select("div.item").map {
                        val id = it.selectFirst("a")
                            ?.attr("href")
                            ?.substringBeforeLast("/")
                            ?.substringAfterLast("/") ?: ""
                        val title = it.selectFirst("a.name")
                            ?.text() ?: ""
                        val poster = it.selectFirst("img")
                            ?.attr("src")

                        val isMovie = it.selectFirst("div.status > div.movie")
                            ?.text() == "Movie"

                        if (isMovie) {
                            Movie(
                                id = id,
                                title = title,
                                poster = poster,
                            )
                        }
                        else {
                            TvShow(
                                id = id,
                                title = title,
                                poster = poster,
                            )
                        }
                    }.distinctBy { when (it) {
                        is Movie ->  it.title
                        is TvShow -> it.title
                    } }
                )
            }
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getHome()

            val genres = document.select("#categories-menu .sub li a")
                .map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"), // .substringAfterLast("/"),
                        name = it.text(),
                    )
                }
                .sortedBy { it.name }

            return genres
        }

        val document = service.search(query.replace(" ", "+"), page)
        if (page > 1) {
            if (document.select("#paging-form").size == 0
                || page > (document.selectFirst("#paging-form span.total")?.text()?.toInt() ?: 0)
            ) {
                return listOf()
            }
        }

        val results = document.select("div.film-list .item").map {
            val id = it.selectFirst("a")
                ?.attr("href")?.substringAfterLast("/") ?: ""
            val title = it.selectFirst("a.name")
                ?.text() ?: ""
            val poster = it.selectFirst("img")
                ?.attr("src")

            val isMovie = it.selectFirst("div.status > div.movie")
                ?.text() == "Movie"

            if (isMovie) {
                Movie(
                    id = id,
                    title = title,
                    poster = poster,
                )
            } else {
                TvShow(
                    id = id,
                    title = title,
                    poster = poster,
                )
            }
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val document = service.getMovies(page)
        if (page > 1) {
            if (document.select("#paging-form").size == 0
                || page > (document.selectFirst("#paging-form span.total")?.text()?.toInt() ?: 0)
            ) {
                return listOf()
            }
        }

        val movies = document.select("div.film-list .item").map {
            Movie(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/") ?: "",
                title = it.selectFirst("a.name")
                    ?.text() ?: "",
                poster = it.selectFirst("img")
                    ?.attr("src"),
            )
        }

        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = service.getTvSeries(page)
        if (page > 1) {
            if (document.select("#paging-form").size == 0
                || page > (document.selectFirst("#paging-form span.total")?.text()?.toInt() ?: 0)
            ) {
                return listOf()
            }
        }

        val tvShows = document.select("div.film-list .item").map {
            TvShow(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/") ?: "",
                title = it.selectFirst("a.name")
                    ?.text() ?: "",
                poster = it.selectFirst("img")
                    ?.attr("src"),
            )
        }

        return tvShows
    }


    override suspend fun getMovie(id: String): Movie {
        val document = service.getDetails(id)

        val movie = Movie(
            id = id,
            title = document.selectFirst("#anime-title")
                ?.text() ?: "errore",
            overview = let {
                val long = document.selectFirst("#main div.widget.info div.info div.desc > div.long")?.text()
                if (long?.isNotEmpty() == true)
                    long
                else
                    document.selectFirst("#main div.widget.info div.info div.desc")?.text()
            },
            released = document.select("#main div.widget.info div.info > div.row > dl:nth-child(1) > dd:nth-child(6)")
                .text().substringAfterLast(" "),
            runtime = document.select("#main div.widget.info div.info > div.row > dl:nth-child(2) > dd:nth-child(4)")
                .text()?.let {
                    val hours = it.substringBefore("h").toIntOrNull() ?: 0
                    val minutes = it.substringBeforeLast(" min").substringAfterLast(" ").toIntOrNull() ?: 0
                    if (hours * 60 + minutes != 0)
                        hours * 60 + minutes
                    else
                        null
                },
            trailer = document.select("#controls div.trailer.control")
                .firstOrNull { it.attr("data-url").contains("youtube") }
                ?.attr("data-url")?.substringAfterLast("/")
                ?.let { "https://www.youtube.com/watch?v=${it}" },
            rating = document.select(".rating #average-vote").text().toDoubleOrNull(),
            poster = document.selectFirst("#thumbnail-watch > img")
                ?.attr("src"),

            genres = document.select("#main div.widget.info div.info > div.row > dl:nth-child(1) > dd:nth-child(12)")
                .select("a")?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            cast = document.select("#main div.widget.info div.info > div.row > dl:nth-child(1) > dd:nth-child(10) > a")
                .select("a").map {
                    People(
                        id = it.attr("href").substringAfterLast("="),
                        name = ("Studio: " + it.text())
                    )
                }.filter { it.name.isNotEmpty() },
            recommendations = document.select("#sidebar > div > div.related")
                .select("div.item")?.map {
                    val showId = it.selectFirst("a")
                        ?.attr("href")?.substringAfterLast("/") ?: ""
                    val showTitle = it.selectFirst("a")
                        ?.text() ?: ""
                    val showRuntime = it.selectFirst("p")
                        ?.text()?.substringBeforeLast("- ")?.substringBefore("min")?.toIntOrNull()
                    val showPoster = it.selectFirst("img")
                        ?.attr("src")

                    val isMovie = it.selectFirst("p")
                        ?.text()?.substringBefore(" ") == "Movie"

                    if (isMovie) {
                        Movie(
                            id = showId,
                            title = showTitle,
                            runtime = showRuntime,
                            poster = showPoster,
                        )
                    } else {
                        TvShow(
                            id = showId,
                            title = showTitle,
                            runtime = showRuntime,
                            poster = showPoster,
                        )
                    }
                } ?: listOf(),
        )

        return movie
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getDetails(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("#anime-title")
                ?.text() ?: "errore",
            overview = let {
                val long = document.selectFirst("#main div.widget.info div.info div.desc > div.long")?.text()
                if (long?.isNotEmpty() == true)
                    long
                else
                    document.selectFirst("#main div.widget.info div.info div.desc")?.text()
            },
            released = document.select("#main div.widget.info div.info > div.row > dl:nth-child(1) > dd:nth-child(6)")
                .text().substringAfterLast(" "),
            runtime = document.select("#main div.widget.info div.info > div.row > dl:nth-child(2) > dd:nth-child(4)")
                .text()?.let {
                    val hours = it.substringBefore("h").toIntOrNull() ?: 0
                    val minutes = it.substringBeforeLast(" min").substringAfterLast(" ").toIntOrNull() ?: 0
                    if (hours * 60 + minutes != 0)
                        hours * 60 + minutes
                    else
                        null
                },
            trailer = document.select("#controls div.trailer.control")
                .firstOrNull { it.attr("data-url").contains("youtube") }
                ?.attr("data-url")?.substringAfterLast("/")
                ?.let { "https://www.youtube.com/watch?v=${it}" },
            rating = document.select(".rating #average-vote").text().toDoubleOrNull(),
            poster = document.selectFirst("#thumbnail-watch > img")
                ?.attr("src"),

            genres = document.select("#main div.widget.info div.info > div.row > dl:nth-child(1) > dd:nth-child(12)")
                .select("a")?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            cast = document.select("#main div.widget.info div.info > div.row > dl:nth-child(1) > dd:nth-child(10) > a")
                .select("a").map {
                    People(
                        id = URLDecoder.decode(it.attr("href").substringAfterLast("="), "UTF-8"),
                        name = URLDecoder.decode(it.attr("href").substringAfterLast("="), "UTF-8") // ("Studio: " + it.text()) ?: ""
                    )
                }.filter { it.name.isNotEmpty() },
            recommendations = document.select("#sidebar > div > div.related")
                .select("div.item")?.map {
                    val showId = it.selectFirst("a")
                        ?.attr("href")?.substringAfterLast("/") ?: ""
                    val showTitle = it.selectFirst("a")
                        ?.text() ?: ""
                    val showRuntime = it.selectFirst("p")
                        ?.text()?.substringBeforeLast("- ")?.substringBefore("min")?.toIntOrNull()
                    val showPoster = it.selectFirst("img")
                        ?.attr("src")

                    val isMovie = it.selectFirst("p")
                        ?.text()?.substringBefore(" ") == "Movie"

                    if (isMovie) {
                        Movie(
                            id = showId,
                            title = showTitle,
                            runtime = showRuntime,
                            poster = showPoster,
                        )
                    } else {
                        TvShow(
                            id = showId,
                            title = showTitle,
                            runtime = showRuntime,
                            poster = showPoster,
                        )
                    }
                } ?: listOf(),
            // Not real seasons, just episode ranges
            seasons = document.select("#animeId div.widget-body div.server[data-id=\"9\"] div.range span").map {
                Season (
                    id = id + "/" + it.attr("data-range-id"),
                    number = it.attr("data-range-id").toInt() + 1,
                    title = "Episodi: " + it.text()
                )
            }.ifEmpty {
                listOf(Season(
                    id = "$id/",
                    number = 0,
                    title = "Episodi"
                ))
            },
        )

        return tvShow
    }

    // need to keep the show id as there's no other way I know of
    // to check for different servers. Hence the showId/range splitting for Seasons
    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val split = seasonId.split("/")
        val showId = split[0]
        val range = if (split.size > 1) split[1] else ""
        val document = service.getDetails(showId)
        var episodes: List<Episode>

        // AW Server
        var cssQuery = "#animeId > div.widget-body > div[data-id=\"9\"]"
        // range of episodes
        if (range != "")
            cssQuery += " ul.episodes.range[data-range-id=\"$range\"]"

        cssQuery += " a"

        episodes = document.select(cssQuery).map {
            Episode(
                id = showId + "/" + it.attr("data-episode-id"),
                number = it.text()
                    .substringBeforeLast("-")
                    .substringBeforeLast(".")
                    .toIntOrNull() ?: 0,
                title = it.text()
            )
        }
        if (episodes.isEmpty()) {
            // Streamtape as a backup
            cssQuery = "#animeId > div.widget-body > div[data-id=\"8\"]"
            if (range != "")
                cssQuery += " ul.episodes.range[data-range-id=\"$range\"]"

            cssQuery += " a"

            episodes = document.select(cssQuery).map {
                Episode(
                    id = showId + "/" + it.attr("data-episode-id"),
                    number = it.text()
                        .substringBeforeLast("-")
                        .substringBeforeLast(".")
                        .toIntOrNull() ?: 0, // (episodes.size + 1)
                    title = it.text()
                )
            }
        }

        return episodes
    }


    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = service.getGenre(id, page)
        if (page > 1) {
            if (document.select("#paging-form").size == 0
                || page > (document.selectFirst("#paging-form span.total")?.text()?.toInt() ?: 0)
            ) {
                return Genre(
                    id = id,
                    name = document.selectFirst("div.widget-title > span.title > h1")
                        ?.text() ?: ""
                )
            }
        }

        val genre = Genre(
            id = id,
            name = document.selectFirst("div.widget-title > span.title > h1")
                ?.text() ?: "",

            shows = document.select("div.film-list .item").map {
                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/") ?: ""
                val showTitle = it.selectFirst("a.name")
                    ?.text() ?: ""
                val showPoster = it.selectFirst("img")
                    ?.attr("src")

                val isMovie = it.selectFirst("div.status > div.movie")
                    ?.text() == "Movie"

                if (isMovie) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                } else {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                }
            }
        )

        return genre
    }


    override suspend fun getPeople(id: String, page: Int): People {
        val document = service.getPeople(id, page)
        if (page > 1) {
            if (document.select("#paging-form").size == 0
                || page > (document.selectFirst("#paging-form span.total")?.text()?.toInt() ?: 0)
            ) {
                return People(
                    id = id,
                    name = document.selectFirst("label[for=\"studio-$id\"]")
                        ?.text() ?: ""
                )
            }
        }

        return People(
            id = id,
            name = document.selectFirst("label[for=\"studio-$id\"]")
                ?.text() ?: "",

            filmography = document.select("div.film-list .item").map {
                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/") ?: ""
                val showTitle = it.selectFirst("a.name")
                    ?.text() ?: ""
                val showPoster = it.selectFirst("img")
                    ?.attr("src")

                val isMovie = it.selectFirst("div.status > div.movie")
                    ?.text() == "Movie"

                if (isMovie) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                } else {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                }
            }
        )
    }


    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val split = id.split("/")
        val showId = split[0]
        val document = service.getDetails(showId)

        // episode main id
        val episodeId = when (videoType) {
            is Video.Type.Movie -> {
                val awServer = document.select("#animeId > div.widget-body > div[data-id=\"9\"] a")
                                .firstOrNull()?.attr("data-episode-id") ?: ""

                if (awServer != "")
                    awServer
                else {
                    // streamtape as backup
                    document.select("#animeId > div.widget-body > div[data-id=\"8\"] a")
                        .firstOrNull()?.attr("data-episode-id") ?: ""
                }
            }
            is Video.Type.Episode -> if (split.size > 1) split[1] else ""
        }

        return document.select("#animeId div.widget-body div.server").map {
            Video.Server (
                id = it.selectFirst("a[data-episode-id=\"$episodeId\"]")?.attr("data-id") ?: "",
                name = when (it.attr("data-id")) {
                    "9" -> "AnimeWorld Server"
                    "8" -> "Streamtape"
                    else -> ""
                }
            )
        }.filter { it.id.isNotEmpty() }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val link = service.getLink(server.id, 0)

        if (server.name == "Streamtape")
            return Extractor.extract(link.grabber.substringBeforeLast("/"))

        return Video(
            source = link.grabber,
            subtitles = listOf()
        )
    }

    private fun getCookieValue(document: Document): String{
        val scriptContent = document.select("script").first()?.data();

        if(scriptContent != null) {
            val pattern = Pattern.compile("""document\.cookie\s*=\s*"([^";]+)""")

            val matcher = pattern.matcher(scriptContent)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }

        return "";
    }

    private interface AnimeWorldService {

        companion object {
            fun build(): AnimeWorldService {
                val client = OkHttpClient.Builder()
                    .cookieJar(MyCookieJar())
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(AnimeWorldService::class.java)
            }
        }


        @Headers(USER_AGENT)
        @GET(".")
        suspend fun getHome(): Document

        @Headers(USER_AGENT)
        @GET("search")
        suspend fun search(
            @Query("keyword", encoded = true) keyword: String,
            @Query("page") page: Int,
        ): Document

        @Headers(USER_AGENT)
        @GET("filter?type=4&sort=0")
        suspend fun getMovies(
            @Query("page") page: Int,
        ): Document

        @Headers(USER_AGENT)
        @GET("filter?type=0&sort=0")
        suspend fun getTvSeries(
            @Query("page") page: Int,
        ): Document

        @Headers(USER_AGENT)
        @GET("play/{id}")
        suspend fun getDetails(@Path("id") id: String): Document

        @Headers(USER_AGENT)
        @GET("genre/{id}")
        suspend fun getGenre(
            @Path("id") id: String,
            @Query("page") page: Int,
        ): Document


        @Headers(USER_AGENT)
        @GET("filter")
        suspend fun getPeople(
            @Query("studio") id: String,
            @Query("page") page: Int,
        ): Document


        @Headers(USER_AGENT)
        @GET("api/episode/info")
        suspend fun getLink(
            @Query("id") id: String,
            @Query("alt") alt: Number,
        ): Link


        data class Link(
            val grabber: String = ""
        )
    }
}