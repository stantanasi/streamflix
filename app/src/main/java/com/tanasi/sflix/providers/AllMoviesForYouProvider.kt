package com.tanasi.sflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.sflix.fragments.player.PlayerFragment
import com.tanasi.sflix.models.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Retrofit
import retrofit2.http.GET

object AllMoviesForYouProvider : Provider {

    private val service = AllMoviesForYouService.build()


    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = "Featured",
                list = document.select("div.MovieListSldCn article.TPost.A").map {
                    val id = it.selectFirst("a")?.attr("href")
                        ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                    val title = it.selectFirst("div.Title")
                        ?.text() ?: ""
                    val overview = it.selectFirst("div.Description > p")
                        ?.text() ?: ""
                    val released = it.selectFirst("span.Date")
                        ?.text()
                    val runtime = it.selectFirst("span.Time")
                        ?.text()?.toMinutes()
                    val rating = it.selectFirst("span.st-vote")
                        ?.text()?.toDoubleOrNull()
                    val banner = it.selectFirst("div.Image img")
                        ?.attr("src")?.toSafeUrl()

                    val genres = it.select("div.Description > p.Genre a").map { element ->
                        Genre(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    }
                    val directors = it.select("div.Description > p.Director a").map { element ->
                        People(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    }
                    val cast = it.select("div.Description > p.Cast a").map { element ->
                        People(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    }

                    when {
                        it.isMovie() -> {
                            Movie(
                                id = id,
                                title = title,
                                overview = overview,
                                released = released,
                                runtime = runtime,
                                rating = rating,
                                banner = banner,

                                genres = genres,
                                directors = directors,
                                cast = cast,
                            )
                        }
                        else -> {
                            TvShow(
                                id = id,
                                title = title,
                                overview = overview,
                                released = released,
                                runtime = runtime,
                                rating = rating,
                                banner = banner,

                                genres = genres,
                                directors = directors,
                                cast = cast,
                            )
                        }
                    }
                },
            )
        )

        categories.add(
            Category(
                name = "Most Popular",
                list = document
                    .select("div.MovieListTop div.TPost.B")
                    .map {
                        val id = it.selectFirst("a")?.attr("href")
                            ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                        val title = it.selectFirst("h2.Title")
                            ?.text() ?: ""
                        val poster = it.selectFirst("div.Image img")
                            ?.attr("data-src")?.toSafeUrl()

                        when {
                            it.isMovie() -> {
                                Movie(
                                    id = id,
                                    title = title,
                                    poster = poster,
                                )
                            }
                            else -> {
                                TvShow(
                                    id = id,
                                    title = title,
                                    poster = poster,
                                )
                            }
                        }
                    },
            )
        )

        categories.add(
            Category(
                name = "Latest Movies",
                list = document
                    .select("section[data-id=movies] article.TPost.B")
                    .map {
                        Movie(
                            id = it.selectFirst("a")?.attr("href")
                                ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                            title = it.selectFirst("h2.Title")
                                ?.text() ?: "",
                            overview = it.selectFirst("div.Description > p")
                                ?.text() ?: "",
                            released = it.selectFirst("div.Image span.Yr")
                                ?.text(),
                            runtime = it.selectFirst("span.Time")
                                ?.text()?.toMinutes(),
                            quality = it.selectFirst("div.Image span.Qlty")
                                ?.text(),
                            rating = it.selectFirst("div.Vote > div.post-ratings > span")
                                ?.text()?.toDoubleOrNull(),
                            poster = it.selectFirst("div.Image img")
                                ?.attr("data-src")?.toSafeUrl(),

                            genres = it.select("div.Description > p.Genre a").map { element ->
                                Genre(
                                    id = element.attr("href")
                                        .substringBeforeLast("/").substringAfterLast("/"),
                                    name = element.text(),
                                )
                            },
                            directors = it.select("div.Description > p.Director a").map { element ->
                                People(
                                    id = element.attr("href")
                                        .substringBeforeLast("/").substringAfterLast("/"),
                                    name = element.text(),
                                )
                            },
                            cast = it.select("div.Description > p.Cast a").map { element ->
                                People(
                                    id = element.attr("href")
                                        .substringBeforeLast("/").substringAfterLast("/"),
                                    name = element.text(),
                                )
                            },
                        )
                    },
            )
        )

        categories.add(
            Category(
                name = "Latest TV Shows",
                list = document
                    .select("section[data-id=series] article.TPost.B")
                    .map {
                        TvShow(
                            id = it.selectFirst("a")?.attr("href")
                                ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                            title = it.selectFirst("h2.Title")
                                ?.text() ?: "",
                            overview = it.selectFirst("div.Description > p")
                                ?.text() ?: "",
                            released = it.selectFirst("div.Image span.Yr")
                                ?.text(),
                            runtime = it.selectFirst("span.Time")
                                ?.text()?.toMinutes(),
                            rating = it.selectFirst("div.Vote > div.post-ratings > span")
                                ?.text()?.toDoubleOrNull(),
                            poster = it.selectFirst("div.Image img")
                                ?.attr("data-src")?.toSafeUrl(),

                            genres = it.select("div.Description > p.Genre a").map { element ->
                                Genre(
                                    id = element.attr("href")
                                        .substringBeforeLast("/").substringAfterLast("/"),
                                    name = element.text(),
                                )
                            },
                            directors = it.select("div.Description > p.Director a").map { element ->
                                People(
                                    id = element.attr("href")
                                        .substringBeforeLast("/").substringAfterLast("/"),
                                    name = element.text(),
                                )
                            },
                            cast = it.select("div.Description > p.Cast a").map { element ->
                                People(
                                    id = element.attr("href")
                                        .substringBeforeLast("/").substringAfterLast("/"),
                                    name = element.text(),
                                )
                            },
                        )
                    },
            )
        )

        return categories
    }

    override suspend fun search(query: String): List<Show> {
        TODO("Not yet implemented")
    }

    override suspend fun getMovies(): List<Movie> {
        TODO("Not yet implemented")
    }

    override suspend fun getTvShows(): List<TvShow> {
        TODO("Not yet implemented")
    }


    override suspend fun getMovie(id: String): Movie {
        TODO("Not yet implemented")
    }


    override suspend fun getTvShow(id: String): TvShow {
        TODO("Not yet implemented")
    }

    override suspend fun getSeasonEpisodes(seasonId: String): List<Episode> {
        TODO("Not yet implemented")
    }


    override suspend fun getPeople(id: String): People {
        TODO("Not yet implemented")
    }


    override suspend fun getVideo(id: String, videoType: PlayerFragment.VideoType): Video {
        TODO("Not yet implemented")
    }


    private fun Element.isMovie(): Boolean = this.selectFirst("a")?.attr("href")
        ?.contains("/movies/") ?: false

    private fun String.toMinutes(): Int {
        val result = Regex("(\\d+)h (\\d+)m|(\\d+) min").find(this)?.groupValues.let {
            object {
                val hours = it?.getOrNull(1)?.toIntOrNull() ?: 0
                val minutes = it?.getOrNull(2)?.toIntOrNull()
                    ?: it?.getOrNull(3)?.toIntOrNull() ?: 0
            }
        }
        return result.hours * 60 + result.minutes
    }

    private fun String.toSafeUrl(): String = when {
        this.startsWith("https:") -> this
        else -> "https:$this"
    }.substringBefore("?")


    interface AllMoviesForYouService {

        companion object {
            fun build(): AllMoviesForYouService {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://allmoviesforyou.net/")
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()

                return retrofit.create(AllMoviesForYouService::class.java)
            }
        }


        @GET(".")
        suspend fun getHome(): Document
    }
}