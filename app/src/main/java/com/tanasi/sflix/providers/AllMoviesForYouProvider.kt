package com.tanasi.sflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.sflix.fragments.player.PlayerFragment
import com.tanasi.sflix.models.*
import com.tanasi.sflix.utils.JsUnpacker
import com.tanasi.sflix.utils.retry
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

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
        if (query.isEmpty()) return listOf()

        val document = service.search(query)

        val results = document.select("ul.MovieList article.TPost.B").map {
            val id = it.selectFirst("a")?.attr("href")
                ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
            val title = it.selectFirst("h2.Title")
                ?.text() ?: ""
            val overview = it.selectFirst("div.Description > p")
                ?.text() ?: ""
            val released = it.selectFirst("div.Image span.Yr")
                ?.text()
            val runtime = it.selectFirst("span.Time")
                ?.text()?.toMinutes()
            val rating = it.selectFirst("div.Vote > div.post-ratings > span")
                ?.text()?.toDoubleOrNull()
            val poster = it.selectFirst("div.Image img")
                ?.attr("data-src")?.toSafeUrl()

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
                        quality = it.selectFirst("div.Image span.Qlty")?.text(),
                        rating = rating,
                        poster = poster,

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
                        poster = poster,

                        genres = genres,
                        directors = directors,
                        cast = cast,
                    )
                }
            }
        }

        return results
    }

    override suspend fun getMovies(): List<Movie> {
        val document = service.getMovies()

        val movies = document.select("ul.MovieList article.TPost.B").map {
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
        }

        return movies
    }

    override suspend fun getTvShows(): List<TvShow> {
        val document = service.getTvShows()

        val tvShows = document.select("ul.MovieList article.TPost.B").map {
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
        }

        return tvShows
    }


    override suspend fun getMovie(id: String): Movie {
        val document = service.getMovie(id)

        val movie = Movie(
            id = id,
            title = document.selectFirst("h1.Title")
                ?.text() ?: "",
            overview = document.selectFirst("div.Description > p")
                ?.text() ?: "",
            released = document.selectFirst("span.Date")
                ?.text(),
            runtime = document.selectFirst("span.Time")
                ?.text()?.toMinutes(),
            youtubeTrailerId = Regex("\"trailer\":\".*?src=\\\\\"(.*?)\\\\\"").find(document.toString())
                ?.groupValues?.get(1)?.replace("\\\\", "")?.substringAfterLast("/"),
            quality = document.selectFirst("span.Qlty")
                ?.text(),
            rating = document.selectFirst("div.Vote > div.post-ratings > span")
                ?.text()?.toDoubleOrNull(),
            banner = document.selectFirst("div.Image img")
                ?.attr("src")?.toSafeUrl(),

            genres = document.select("div.Description > p.Genre a").map {
                Genre(
                    id = it.attr("href")
                        .substringBeforeLast("/").substringAfterLast("/"),
                    name = it.text(),
                )
            },
            directors = document.select("div.Description > p.Director a").map {
                People(
                    id = it.attr("href")
                        .substringBeforeLast("/").substringAfterLast("/"),
                    name = it.text(),
                )
            },
            cast = document.select("div.Description > p.Cast a").map {
                People(
                    id = it.attr("href")
                        .substringBeforeLast("/").substringAfterLast("/"),
                    name = it.text(),
                )
            },
            recommendations = document.select("div.MovieListTop div.TPost.B").map {
                val showId = it.selectFirst("a")?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                val showTitle = it.selectFirst("h2.Title")
                    ?.text() ?: ""
                val showPoster = it.selectFirst("div.Image img")
                    ?.attr("data-src")?.toSafeUrl()

                when {
                    it.isMovie() -> {
                        Movie(
                            id = showId,
                            title = showTitle,
                            poster = showPoster,
                        )
                    }
                    else -> {
                        TvShow(
                            id = showId,
                            title = showTitle,
                            poster = showPoster,
                        )
                    }
                }
            },
        )

        return movie
    }


    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTvShow(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("h1.Title")
                ?.text() ?: "",
            overview = document.selectFirst("div.Description > p")
                ?.text() ?: "",
            released = document.selectFirst("span.Date")
                ?.text(),
            runtime = document.selectFirst("span.Time")
                ?.text()?.toMinutes(),
            youtubeTrailerId = Regex("\"trailer\":\".*?src=\\\\\"(.*?)\\\\\"").find(document.toString())
                ?.groupValues?.get(1)?.replace("\\\\", "")?.substringAfterLast("/"),
            rating = document.selectFirst("div.Vote > div.post-ratings > span")?.text()
                ?.toDoubleOrNull(),
            banner = document.selectFirst("div.Image img")
                ?.attr("src")?.toSafeUrl(),

            seasons = document.select("section.SeasonBx").map {
                Season(
                    id = it.selectFirst("a")?.attr("href")
                        ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                    number = it.selectFirst("div.Title > a > span")
                        ?.text()?.toIntOrNull() ?: 0,
                    title = it.selectFirst("div.Title")
                        ?.text() ?: "",
                )
            },
            genres = document.select("div.Description > p.Genre a").map {
                Genre(
                    id = it.attr("href")
                        .substringBeforeLast("/").substringAfterLast("/"),
                    name = it.text(),
                )
            },
            directors = document.select("div.Description > p.Director a").map {
                People(
                    id = it.attr("href")
                        .substringBeforeLast("/").substringAfterLast("/"),
                    name = it.text(),
                )
            },
            cast = document.select("div.Description > p.Cast a").map {
                People(
                    id = it.attr("href")
                        .substringBeforeLast("/").substringAfterLast("/"),
                    name = it.text(),
                )
            },
            recommendations = document.select("div.MovieListTop div.TPost.B").map {
                val showId = it.selectFirst("a")?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                val showTitle = it.selectFirst("h2.Title")
                    ?.text() ?: ""
                val showPoster = it.selectFirst("div.Image img")
                    ?.attr("data-src")?.toSafeUrl()

                when {
                    it.isMovie() -> {
                        Movie(
                            id = showId,
                            title = showTitle,
                            poster = showPoster,
                        )
                    }
                    else -> {
                        TvShow(
                            id = showId,
                            title = showTitle,
                            poster = showPoster,
                        )
                    }
                }
            },
        )

        return tvShow
    }

    override suspend fun getSeasonEpisodes(seasonId: String): List<Episode> {
        val document = service.getSeasonEpisodes(seasonId)

        val episodes = document.select("section.SeasonBx tr.Viewed").map {
            Episode(
                id = it.selectFirst("a")?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                number = it.selectFirst("span.Num")
                    ?.text()?.toIntOrNull() ?: 0,
                title = it.selectFirst("td.MvTbTtl > a")
                    ?.text() ?: "",
                released = it.selectFirst("td.MvTbTtl > span")
                    ?.text(),
                poster = it.selectFirst("td.MvTbImg img")
                    ?.attr("src")?.toSafeUrl()?.replace("w92", "w500"),
            )
        }

        return episodes
    }


    override suspend fun getPeople(id: String): People {
        val document = service.getPeople(id)

        val people = People(
            id = id,
            name = document.selectFirst("h2.Title")
                ?.text() ?: "",

            filmography = document.select("ul.MovieList article.TPost.B").map {
                val showId = it.selectFirst("a")?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                val showTitle = it.selectFirst("h2.Title")
                    ?.text() ?: ""
                val showOverview = it.selectFirst("div.Description > p")
                    ?.text() ?: ""
                val showReleased = it.selectFirst("div.Image span.Yr")
                    ?.text()
                val showRuntime = it.selectFirst("span.Time")
                    ?.text()?.toMinutes()
                val showRating = it.selectFirst("div.Vote > div.post-ratings > span")
                    ?.text()?.toDoubleOrNull()
                val showPoster = it.selectFirst("div.Image img")
                    ?.attr("data-src")?.toSafeUrl()

                val showGenres = it.select("div.Description > p.Genre a").map { element ->
                    Genre(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                }
                val showDirectors = it.select("div.Description > p.Director a").map { element ->
                    People(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                }
                val showCast = it.select("div.Description > p.Cast a").map { element ->
                    People(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                }

                when {
                    it.isMovie() -> {
                        Movie(
                            id = showId,
                            title = showTitle,
                            overview = showOverview,
                            released = showReleased,
                            runtime = showRuntime,
                            quality = it.selectFirst("div.Image span.Qlty")?.text(),
                            rating = showRating,
                            poster = showPoster,

                            genres = showGenres,
                            directors = showDirectors,
                            cast = showCast,
                        )
                    }
                    else -> {
                        TvShow(
                            id = showId,
                            title = showTitle,
                            overview = showOverview,
                            released = showReleased,
                            runtime = showRuntime,
                            rating = showRating,
                            poster = showPoster,

                            genres = showGenres,
                            directors = showDirectors,
                            cast = showCast,
                        )
                    }
                }
            },
        )

        return people
    }


    override suspend fun getVideo(id: String, videoType: PlayerFragment.VideoType): Video {
        val document = when (videoType) {
            PlayerFragment.VideoType.Movie -> service.getMovie(id)
            PlayerFragment.VideoType.Episode -> service.getEpisode(id)
        }

        val links = document.select("body iframe")
            .map { it.attr("src") }
            .mapNotNull { src ->
                if (src.contains("trembed")) {
                    service.getLink(src)
                        .selectFirst("body iframe")
                        ?.attr("src")
                        ?.replace("streamhub.to/d/", "streamhub.to/e/")
                } else {
                    src
                }
            }

        val video = retry(links.size) { attempt ->
            val link = service.getSource(links.getOrNull(attempt - 1) ?: "")

            val jsEval = Regex("eval((.|\\n)*?)</script>").find(link.toString())?.let {
                it.groupValues[1]
            } ?: throw Exception("No sources found")

            val unPacked = JsUnpacker("eval$jsEval").unpack()
                ?: throw Exception("No sources found")

            val sources = Regex("src:\"(.*?)\"").findAll(
                Regex("\\{sources:\\[(.*?)]").find(unPacked)?.let {
                    it.groupValues[1]
                } ?: throw Exception("No sources found")
            ).map { it.groupValues[1] }.toList()

            Video(
                sources = sources,
            )
        }

        return video
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

        @GET(".")
        suspend fun search(@Query("s") query: String): Document

        @GET("movies")
        suspend fun getMovies(): Document

        @GET("shows")
        suspend fun getTvShows(): Document


        @GET("movies/{slug}")
        suspend fun getMovie(@Path("slug") slug: String): Document


        @GET("series/{slug}")
        suspend fun getTvShow(@Path("slug") slug: String): Document

        @GET("season/{id}")
        suspend fun getSeasonEpisodes(@Path("id") seasonId: String): Document

        @GET("episode/{id}")
        suspend fun getEpisode(@Path("id") id: String): Document


        @GET("cast/{slug}")
        suspend fun getPeople(@Path("slug") slug: String): Document


        @GET
        suspend fun getLink(@Url url: String): Document

        @GET
        suspend fun getSource(@Url url: String): Document
    }
}