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
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

object HiAnimeProvider : Provider {

    private const val URL = "https://hianime.to/"
    override val baseUrl = URL
    override val name = "HiAnime"
    override val logo = "$URL/images/logo.png"
    override val language = "en"

    private val service = HiAnimeService.build()


    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = Category.FEATURED,
                list = document.select("div#slider div.swiper-slide").map {
                    val id = it.selectFirst("a")
                        ?.attr("href")?.substringAfterLast("/") ?: ""
                    val title = it.selectFirst("div.desi-head-title")
                        ?.text() ?: ""
                    val overview = it.selectFirst("div.desi-description")
                        ?.text()
                    val runtime = it.select("div.scd-item").firstOrNull { element ->
                        element.selectFirst("i.fa-clock") != null
                    }?.text()?.removeSuffix("m")?.toIntOrNull()
                    val quality = it.selectFirst("div.quality")
                        ?.text()
                    val banner = it.selectFirst("img.film-poster-img")
                        ?.attr("data-src")

                    val isMovie = it.select("div.scd-item").firstOrNull { element ->
                        element.selectFirst("i.fa-play-circle") != null
                    }?.text() == "Movie"

                    if (isMovie) {
                        Movie(
                            id = id,
                            title = title,
                            overview = overview,
                            runtime = runtime,
                            quality = quality,
                            banner = banner,
                        )
                    } else {
                        TvShow(
                            id = id,
                            title = title,
                            overview = overview,
                            runtime = runtime,
                            quality = quality,
                            banner = banner,

                            seasons = it.selectFirst("div.tick-sub")
                                ?.text()?.toIntOrNull()?.let { lastEpisode ->
                                    listOf(
                                        Season(
                                            id = "",
                                            number = 0,

                                            episodes = listOf(
                                                Episode(
                                                    id = "",
                                                    number = lastEpisode,
                                                )
                                            )
                                        )
                                    )
                                } ?: listOf(),
                        )
                    }
                },
            )
        )

        categories.addAll(
            document.select("div.anif-block").map { block ->
                Category(
                    name = block.selectFirst("div.anif-block-header")
                        ?.text() ?: "",
                    list = block.select("li").map {
                        val id = it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("/") ?: ""
                        val title = it.selectFirst("h3.film-name")
                            ?.text() ?: ""
                        val poster = it.selectFirst("img.film-poster-img")
                            ?.attr("data-src")

                        val isMovie = it.select("div.fd-infor span.fdi-item")
                            .lastOrNull()
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

                                seasons = it.selectFirst("div.tick-sub")
                                    ?.text()?.toIntOrNull()?.let { lastEpisode ->
                                        listOf(
                                            Season(
                                                id = "",
                                                number = 0,

                                                episodes = listOf(
                                                    Episode(
                                                        id = "",
                                                        number = lastEpisode,
                                                    )
                                                )
                                            )
                                        )
                                    } ?: listOf(),
                            )
                        }
                    }
                )
            }
        )

        categories.addAll(
            document.select("section.block_area.block_area_home").mapNotNull { block ->
                val name = block.selectFirst("h2.cat-heading")
                    ?.text() ?: ""
                if (name == "Top Upcoming") return@mapNotNull null

                Category(
                    name = name,
                    list = block.select("div.flw-item").map {
                        val id = it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("/") ?: ""
                        val title = it.selectFirst("h3.film-name")
                            ?.text() ?: ""
                        val runtime = it.selectFirst("div.fd-infor span.fdi-duration")
                            ?.text()?.removeSuffix("m")?.toIntOrNull()
                        val poster = it.selectFirst("img.film-poster-img")
                            ?.attr("data-src")

                        val isMovie = it.selectFirst("div.fd-infor span.fdi-item")
                            ?.text() == "Movie"

                        if (isMovie) {
                            Movie(
                                id = id,
                                title = title,
                                runtime = runtime,
                                poster = poster,
                            )
                        } else {
                            TvShow(
                                id = id,
                                title = title,
                                runtime = runtime,
                                poster = poster,

                                seasons = it.selectFirst("div.tick-sub")
                                    ?.text()?.toIntOrNull()?.let { lastEpisode ->
                                        listOf(
                                            Season(
                                                id = "",
                                                number = 0,

                                                episodes = listOf(
                                                    Episode(
                                                        id = "",
                                                        number = lastEpisode,
                                                    )
                                                )
                                            )
                                        )
                                    } ?: listOf(),
                            )
                        }
                    }
                )
            }
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getHome()

            val genres = document.select("div#sidebar_subs_genre a.nav-link")
                .map {
                    Genre(
                        id = it.attr("href")
                            .substringAfterLast("/"),
                        name = it.text(),
                    )
                }
                .sortedBy { it.name }

            return genres
        }

        val document = service.search(query.replace(" ", "+"), page)

        val results = document.select("div.flw-item").map {
            val id = it.selectFirst("a")
                ?.attr("href")?.substringAfterLast("/") ?: ""
            val title = it.selectFirst("h3.film-name")
                ?.text() ?: ""
            val runtime = it.selectFirst("span.fdi-duration")
                ?.text()?.removeSuffix("m")?.toIntOrNull()
            val poster = it.selectFirst("img.film-poster-img")
                ?.attr("data-src")

            val isMovie = it.selectFirst("div.fd-infor > span.fdi-item")
                ?.text() == "Movie"

            if (isMovie) {
                Movie(
                    id = id,
                    title = title,
                    runtime = runtime,
                    poster = poster,
                )
            } else {
                TvShow(
                    id = id,
                    title = title,
                    runtime = runtime,
                    poster = poster,
                )
            }
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val document = service.getMovies(page)

        val movies = document.select("div.flw-item").map {
            Movie(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/") ?: "",
                title = it.selectFirst("h3.film-name")
                    ?.text() ?: "",
                overview = it.selectFirst("div.description")
                    ?.text(),
                runtime = it.selectFirst("span.fdi-duration")
                    ?.text()?.removeSuffix("m")?.toIntOrNull(),
                poster = it.selectFirst("img.film-poster-img")
                    ?.attr("data-src"),
            )
        }

        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = service.getTvSeries(page)

        val tvShows = document.select("div.flw-item").map {
            TvShow(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/") ?: "",
                title = it.selectFirst("h3.film-name")
                    ?.text() ?: "",
                overview = it.selectFirst("div.description")
                    ?.text(),
                runtime = it.selectFirst("div.fd-infor span.fdi-duration")
                    ?.text()?.removeSuffix("m")?.toIntOrNull(),
                poster = it.selectFirst("img.film-poster-img")
                    ?.attr("data-src"),

                seasons = it.selectFirst("div.tick-sub")
                    ?.text()?.toIntOrNull()?.let { lastEpisode ->
                        listOf(
                            Season(
                                id = "",
                                number = 0,

                                episodes = listOf(
                                    Episode(
                                        id = "",
                                        number = lastEpisode,
                                    )
                                )
                            )
                        )
                    } ?: listOf(),
            )
        }

        return tvShows
    }


    override suspend fun getMovie(id: String): Movie {
        val document = service.getMovie(id)

        val movie = Movie(
            id = id,
            title = document.selectFirst("div.anisc-detail h2.film-name")
                ?.text() ?: "",
            overview = document.selectFirst("div.anisc-detail div.film-description  > .text")
                ?.text(),
            released = document.select("div.anisc-info div.item")
                .find { it.selectFirst("span.item-head")?.text() == "Aired:" }
                ?.selectFirst("span.name")?.text()?.substringBefore(" to"),
            runtime = document.select("div.anisc-info div.item")
                .find { it.selectFirst("span.item-head")?.text() == "Duration:" }
                ?.selectFirst("span.name")?.text()?.let {
                    val hours = it.substringBefore("h").toIntOrNull() ?: 0
                    val minutes = it.substringAfter("h ").substringBefore("m").toIntOrNull() ?: 0
                    hours * 60 + minutes
                },
            trailer = document.select("section.block_area-promotions div.item")
                .firstOrNull { it.attr("data-src").contains("youtube") }
                ?.attr("data-src")?.substringAfterLast("/")
                ?.let { "https://www.youtube.com/watch?v=${it}" },
            rating = document.select("div.anisc-info div.item")
                .find { it.selectFirst("span.item-head")?.text() == "MAL Score:" }
                ?.selectFirst("span.name")?.text()?.toDoubleOrNull(),
            poster = document.selectFirst("div.anisc-poster img")
                ?.attr("src"),

            genres = document.select("div.anisc-info div.item")
                .find { it.selectFirst("span.item-head")?.text() == "Genres:" }
                ?.select("a")?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            cast = document.select("div.block-actors-content div.bac-item").map {
                People(
                    id = it.selectFirst("div.rtl a")
                        ?.attr("href")?.substringAfterLast("/") ?: "",
                    name = it.selectFirst("div.rtl h4.pi-name")
                        ?.text() ?: "",
                    image = it.selectFirst("div.rtl img")
                        ?.attr("data-src"),
                )
            }.filter { it.name.isNotEmpty() },
            recommendations = document.select("section.block_area_category")
                .find { it.selectFirst("h2.cat-heading")?.text() == "Recommended for you" }
                ?.select("div.flw-item")?.map {
                    val showId = it.selectFirst("a")
                        ?.attr("href")?.substringAfterLast("/") ?: ""
                    val showTitle = it.selectFirst("h3.film-name")
                        ?.text() ?: ""
                    val showRuntime = it.selectFirst("div.fd-infor span.fdi-duration")
                        ?.text()?.substringBefore("m")?.toIntOrNull()
                    val showPoster = it.selectFirst("img")
                        ?.attr("data-src")

                    val isMovie = it.selectFirst("div.fd-infor > span.fdi-item")
                        ?.text() == "Movie"

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
        val document = service.getTvShow(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("div.anisc-detail h2.film-name")
                ?.text() ?: "",
            overview = document.selectFirst("div.anisc-detail div.film-description  > .text")
                ?.text(),
            released = document.select("div.anisc-info div.item")
                .find { it.selectFirst("span.item-head")?.text() == "Aired:" }
                ?.selectFirst("span.name")?.text()?.substringBefore(" to"),
            runtime = document.select("div.anisc-info div.item")
                .find { it.selectFirst("span.item-head")?.text() == "Duration:" }
                ?.selectFirst("span.name")?.text()?.let {
                    val hours = it.substringBefore("h").toIntOrNull() ?: 0
                    val minutes = it.substringAfter("h ").substringBefore("m").toIntOrNull() ?: 0
                    hours * 60 + minutes
                },
            trailer = document.select("section.block_area-promotions div.item")
                .firstOrNull { it.attr("data-src").contains("youtube") }
                ?.attr("data-src")?.substringAfterLast("/")
                ?.let { "https://www.youtube.com/watch?v=${it}" },
            rating = document.select("div.anisc-info div.item")
                .find { it.selectFirst("span.item-head")?.text() == "MAL Score:" }
                ?.selectFirst("span.name")?.text()?.toDoubleOrNull(),
            poster = document.selectFirst("div.anisc-poster img")
                ?.attr("src"),

            seasons = listOf(
                Season(
                    id = id.substringAfterLast("-"),
                    number = 0,
                    title = "Episodes",
                )
            ),
            genres = document.select("div.anisc-info div.item")
                .find { it.selectFirst("span.item-head")?.text() == "Genres:" }
                ?.select("a")?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            cast = document.select("div.block-actors-content div.bac-item").map {
                People(
                    id = it.selectFirst("div.rtl a")
                        ?.attr("href")?.substringAfterLast("/") ?: "",
                    name = it.selectFirst("div.rtl h4.pi-name")
                        ?.text() ?: "",
                    image = it.selectFirst("div.rtl img")
                        ?.attr("data-src"),
                )
            }.filter { it.name.isNotEmpty() },
            recommendations = document.select("section.block_area_category")
                .find { it.selectFirst("h2.cat-heading")?.text() == "Recommended for you" }
                ?.select("div.flw-item")?.map {
                    val showId = it.selectFirst("a")
                        ?.attr("href")?.substringAfterLast("/") ?: ""
                    val showTitle = it.selectFirst("h3.film-name")
                        ?.text() ?: ""
                    val showRuntime = it.selectFirst("div.fd-infor span.fdi-duration")
                        ?.text()?.substringBefore("m")?.toIntOrNull()
                    val showPoster = it.selectFirst("img")
                        ?.attr("data-src")

                    val isMovie = it.selectFirst("div.fd-infor > span.fdi-item")
                        ?.text() == "Movie"

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

        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val response = service.getTvShowEpisodes(tvShowId = seasonId)

        val episodes = Jsoup.parse(response.html).select("div.ss-list > a[href].ssl-item.ep-item").map {
            Episode(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("=") ?: "",
                number = it.selectFirst("div.ssli-order")
                    ?.text()?.toIntOrNull() ?: 0,
                title = it.selectFirst("div.ep-name")
                    ?.text(),
            )
        }

        return episodes
    }


    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = service.getGenre(id, page)

        val genre = Genre(
            id = id,
            name = document.selectFirst("h2.cat-heading")
                ?.text() ?: "",

            shows = document.select("div.flw-item").map {
                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/") ?: ""
                val showTitle = it.selectFirst("h3.film-name")
                    ?.text() ?: ""
                val showOverview = it.selectFirst("div.description")
                    ?.text()
                val showRuntime = it.selectFirst("div.fd-infor span.fdi-duration")
                    ?.text()?.substringBefore("m")?.toIntOrNull()
                val showPoster = it.selectFirst("img")
                    ?.attr("data-src")

                val isMovie = it.selectFirst("div.fd-infor > span.fdi-item")
                    ?.text() == "Movie"

                if (isMovie) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        overview = showOverview,
                        runtime = showRuntime,
                        poster = showPoster,
                    )
                } else {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        overview = showOverview,
                        runtime = showRuntime,
                        poster = showPoster,
                    )
                }
            }
        )

        return genre
    }


    override suspend fun getPeople(id: String, page: Int): People {
        val document = service.getPeople(id)

        if (page > 1) {
            return People(
                id = id,
                name = document.selectFirst("h4.name")
                    ?.text() ?: "",
                image = document.selectFirst("div.avatar img")
                    ?.attr("src"),
            )
        }

        val people = People(
            id = id,
            name = document.selectFirst("h4.name")
                ?.text() ?: "",
            image = document.selectFirst("div.avatar img")
                ?.attr("src"),

            filmography = document.select("div.bac-item").map {
                val showId = it.selectFirst("div.anime-info a")
                    ?.attr("href")?.substringAfterLast("/") ?: ""
                val showTitle = it.selectFirst("div.anime-info h4.pi-name")
                    ?.text() ?: ""
                val showReleased = it.selectFirst("div.anime-info div.pi-detail span.pi-cast")
                    ?.text()?.substringAfterLast(", ")
                val showPoster = it.selectFirst("div.anime-info img")
                    ?.attr("src")

                val isMovie = it.selectFirst("div.anime-info div.pi-detail span.pi-cast")
                    ?.text()?.substringBefore(", ") == "Movie"

                if (isMovie) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        released = showReleased,
                        poster = showPoster,
                    )
                } else {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        released = showReleased,
                        poster = showPoster,
                    )
                }
            }.distinctBy {
                when (it) {
                    is Movie -> it.id
                    is TvShow -> it.id
                }
            }
        )

        return people
    }


    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val episodeId = when (videoType) {
            is Video.Type.Movie -> {
                val response = service.getTvShowEpisodes(tvShowId = id.substringAfterLast("-"))

                Jsoup.parse(response.html).select("div.ss-list > a[href].ssl-item.ep-item")
                    .firstOrNull()
                    ?.let {
                        it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("=")
                    } ?: ""
            }
            is Video.Type.Episode -> id
        }

        val servers = Jsoup.parse(service.getServers(episodeId).html)
            .select("div.server-item[data-type][data-id]")
            .map {
                Video.Server(
                    id = it.attr("data-id"),
                    name = "${it.text().trim()} - ${it.attr("data-type").uppercase()}",
                )
            }

        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val link = service.getLink(server.id)

        return Extractor.extract(link.link)
    }


    private interface HiAnimeService {

        companion object {
            fun build(): HiAnimeService {
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

                return retrofit.create(HiAnimeService::class.java)
            }
        }


        @GET("home")
        suspend fun getHome(): Document

        @GET("search")
        suspend fun search(
            @Query("keyword", encoded = true) keyword: String,
            @Query("page") page: Int
        ): Document

        @GET("movie")
        suspend fun getMovies(@Query("page") page: Int): Document

        @GET("tv")
        suspend fun getTvSeries(@Query("page") page: Int): Document


        @GET("{id}")
        suspend fun getMovie(@Path("id") id: String): Document


        @GET("{id}")
        suspend fun getTvShow(@Path("id") id: String): Document

        @GET("ajax/v2/episode/list/{id}")
        suspend fun getTvShowEpisodes(@Path("id") tvShowId: String): Response


        @GET("genre/{id}")
        suspend fun getGenre(@Path("id") id: String, @Query("page") page: Int): Document


        @GET("people/{id}")
        suspend fun getPeople(@Path("id") id: String): Document


        @GET("ajax/v2/episode/servers")
        suspend fun getServers(@Query("episodeId") episodeId: String): Response

        @GET("ajax/v2/episode/sources")
        suspend fun getLink(@Query("id") id: String): Link


        data class Response(
            val status: Boolean,
            val html: String,
            val totalItems: Int? = null,
            val continueWatch: Boolean? = null,
        )

        data class Link(
            val type: String = "",
            val link: String = "",
            val sources: List<String> = listOf(),
            val tracks: List<String> = listOf(),
            val title: String = "",
        )
    }
}