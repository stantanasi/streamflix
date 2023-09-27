package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.fragments.player.PlayerFragment
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
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

object AniwatchProvider : Provider {

    override val name = "Aniwatch"
    override val logo = "https://aniwatch.to/images/logo.png"
    override val url = "https://aniwatch.to/"

    private val service = AniwatchService.build()


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
                        ?.text() ?: ""
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
                            id =id,
                            title=title,
                            overview = overview,
                            runtime = runtime,
                            quality = quality,
                            banner = banner,
                        )
                    } else {
                        TvShow(
                            id =id,
                            title=title,
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

    override suspend fun search(query: String): List<AppAdapter.Item> {
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

        val document = service.search(query.replace(" ", "+"))

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

    override suspend fun getMovies(): List<Movie> {
        val document = service.getMovies()

        val movies = document.select("div.flw-item").map {
            Movie(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/") ?: "",
                title = it.selectFirst("h3.film-name")
                    ?.text() ?: "",
                overview = it.selectFirst("div.description")
                    ?.text() ?: "",
                runtime = it.selectFirst("span.fdi-duration")
                    ?.text()?.removeSuffix("m")?.toIntOrNull(),
                poster = it.selectFirst("img.film-poster-img")
                    ?.attr("data-src"),
            )
        }

        return movies
    }

    override suspend fun getTvShows(): List<TvShow> {
        val document = service.getTvSeries()

        val tvShows = document.select("div.flw-item").map {
            TvShow(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/") ?: "",
                title = it.selectFirst("h3.film-name")
                    ?.text() ?: "",
                overview = it.selectFirst("div.description")
                    ?.text() ?: "",
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
        TODO("Not yet implemented")
    }


    override suspend fun getTvShow(id: String): TvShow {
        TODO("Not yet implemented")
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        TODO("Not yet implemented")
    }


    override suspend fun getGenre(id: String): Genre {
        TODO("Not yet implemented")
    }


    override suspend fun getPeople(id: String): People {
        TODO("Not yet implemented")
    }


    override suspend fun getVideo(id: String, videoType: PlayerFragment.VideoType): Video {
        TODO("Not yet implemented")
    }


    interface AniwatchService {

        companion object {
            fun build(): AniwatchService {
                val client = OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(AniwatchService::class.java)
            }
        }


        @GET("home")
        suspend fun getHome(): Document

        @GET("search")
        suspend fun search(@Query("keyword", encoded = true) keyword: String): Document

        @GET("movie")
        suspend fun getMovies(): Document

        @GET("tv")
        suspend fun getTvSeries(): Document
    }
}