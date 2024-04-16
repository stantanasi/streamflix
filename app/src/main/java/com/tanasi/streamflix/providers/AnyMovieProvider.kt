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
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

object AnyMovieProvider : Provider {

    override val name = "AnyMovie"
    override val logo = "https://anymovie.cc/wp-content/uploads/2023/08/AM-LOGO-1.png"
    val url = "https://anymovie.cc/"

    private val service = AllMoviesForYouService.build()


    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = Category.FEATURED,
                list = document.select("div.MovieListSldCn article.TPost.A").map {
                    val id = it.selectFirst("a")?.attr("href")
                        ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                    val title = it.selectFirst("div.Title")
                        ?.text() ?: ""
                    val overview = it.selectFirst("div.Description > p")
                        ?.text()
                    val released = it.selectFirst("span.Date")
                        ?.text()
                    val runtime = it.selectFirst("span.Time")
                        ?.text()?.toMinutes()
                    val rating = it.selectFirst("span.st-vote")
                        ?.text()?.toDoubleOrNull()
                    val banner = it.selectFirst("div.Image img")
                        ?.let { img ->
                            when {
                                img.hasAttr("src") -> img.attr("src")
                                img.hasAttr("data-src") -> img.attr("data-src")
                                else -> null
                            }
                        }?.toSafeUrl()

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
                    .select("section")
                    .find { it.attr("data-id") == "movies" }
                    ?.select("article.TPost.B")
                    ?.map {
                        Movie(
                            id = it.selectFirst("a")?.attr("href")
                                ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                            title = it.selectFirst("h2.Title")
                                ?.text() ?: "",
                            overview = it.selectFirst("div.Description > p")
                                ?.text(),
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
                    } ?: listOf(),
            )
        )

        categories.add(
            Category(
                name = "Latest TV Shows",
                list = document
                    .select("section")
                    .find { it.attr("data-id") == "series" }
                    ?.select("article.TPost.B")
                    ?.map {
                        TvShow(
                            id = it.selectFirst("a")?.attr("href")
                                ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                            title = it.selectFirst("h2.Title")
                                ?.text() ?: "",
                            overview = it.selectFirst("div.Description > p")
                                ?.text(),
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
                    } ?: listOf(),
            )
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getHome()

            val genres = document.select("div.Description > p.Genre a")
                .map {
                    Genre(
                        id = it.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = it.text(),
                    )
                }
                .distinctBy { it.id }
                .sortedBy { it.name }

            return genres
        }

        val document = try {
            service.search(page, query)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> null
                else -> throw e
            }
        }

        val results = document?.select("ul.MovieList article.TPost.B")?.map {
            val id = it.selectFirst("a")?.attr("href")
                ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
            val title = it.selectFirst("h2.Title")
                ?.text() ?: ""
            val overview = it.selectFirst("div.Description > p")
                ?.text()
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
        } ?: listOf()

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val document = try {
            service.getMovies(page)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> null
                else -> throw e
            }
        }

        val movies = document?.select("ul.MovieList article.TPost.B")?.map {
            Movie(
                id = it.selectFirst("a")?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                title = it.selectFirst("h2.Title")
                    ?.text() ?: "",
                overview = it.selectFirst("div.Description > p")
                    ?.text(),
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
        } ?: listOf()

        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = try {
            service.getTvShows(page)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> null
                else -> throw e
            }
        }

        val tvShows = document?.select("ul.MovieList article.TPost.B")?.map {
            TvShow(
                id = it.selectFirst("a")?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                title = it.selectFirst("h2.Title")
                    ?.text() ?: "",
                overview = it.selectFirst("div.Description > p")
                    ?.text(),
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
        } ?: listOf()

        return tvShows
    }


    override suspend fun getMovie(id: String): Movie {
        val document = service.getMovie(id)

        val movie = Movie(
            id = id,
            title = document.selectFirst("h1.Title")
                ?.text() ?: "",
            overview = document.selectFirst("div.Description > p")
                ?.text(),
            released = document.selectFirst("span.Date")
                ?.text(),
            runtime = document.selectFirst("span.Time")
                ?.text()?.toMinutes(),
            trailer = Regex("\"trailer\":\".*?src=\\\\\"(.*?)\\\\\"").find(document.toString())
                ?.groupValues?.get(1)?.replace("\\\\", "")?.substringAfterLast("/")
                ?.let { "https://www.youtube.com/watch?v=${it}" },
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
                ?.text(),
            released = document.selectFirst("span.Date")
                ?.text(),
            runtime = document.selectFirst("span.Time")
                ?.text()?.toMinutes(),
            trailer = Regex("\"trailer\":\".*?src=\\\\\"(.*?)\\\\\"").find(document.toString())
                ?.groupValues?.get(1)?.replace("\\\\", "")?.substringAfterLast("/")
                ?.let { "https://www.youtube.com/watch?v=${it}" },
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
                        ?.text(),
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

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val document = service.getSeasonEpisodes(seasonId)

        val episodes = document.select("section.SeasonBx tr.Viewed").map {
            Episode(
                id = it.selectFirst("a")?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                number = it.selectFirst("span.Num")
                    ?.text()?.toIntOrNull() ?: 0,
                title = it.selectFirst("td.MvTbTtl > a")
                    ?.text(),
                released = it.selectFirst("td.MvTbTtl > span")
                    ?.text(),
                poster = it.selectFirst("td.MvTbImg img")
                    ?.attr("src")?.toSafeUrl()?.replace("w92", "w500"),
            )
        }

        return episodes
    }


    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = try {
            service.getGenre(id, page)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> null
                else -> throw e
            }
        }

        val genre = Genre(
            id = id,
            name = document?.selectFirst("h2.Title")
                ?.text() ?: "",

            shows = document?.select("ul.MovieList article.TPost.B")?.map {
                val showId = it.selectFirst("a")?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                val showTitle = it.selectFirst("h2.Title")
                    ?.text() ?: ""
                val showOverview = it.selectFirst("div.Description > p")
                    ?.text()
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
                val showDirectors =
                    it.select("div.Description > p.Director a").map { element ->
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
            } ?: listOf()
        )

        return genre
    }


    override suspend fun getPeople(id: String, page: Int): People {
        val cast = try {
            service.getCast(id, page)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> null
                else -> throw e
            }
        }
        val castTv = try {
            service.getCastTv(id, page)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> null
                else -> throw e
            }
        }

        val people = People(
            id = id,
            name = cast?.selectFirst("h2.Title")
                ?.text() ?: "",

            filmography = listOfNotNull(
                cast?.select("ul.MovieList article.TPost.B"),
                castTv?.select("ul.MovieList article.TPost.B"),
            )
                .flatMap { elements ->
                    elements.map {
                        val showId = it.selectFirst("a")?.attr("href")
                            ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                        val showTitle = it.selectFirst("h2.Title")
                            ?.text() ?: ""
                        val showOverview = it.selectFirst("div.Description > p")
                            ?.text()
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
                        val showDirectors =
                            it.select("div.Description > p.Director a").map { element ->
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
                    }
                }
                .sortedByDescending {
                    when (it) {
                        is Movie -> it.released
                        is TvShow -> it.released
                    }
                },
        )

        return people
    }


    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val document = when (videoType) {
            is Video.Type.Movie -> service.getMovie(id)
            is Video.Type.Episode -> service.getEpisode(id)
        }

        val servers = document.select("ul.optnslst button").map {
            val nmopt = it.selectFirst("span")?.text() ?: ""
            Video.Server(
                id = it.attr("data-id"),
                name = it.select("span").getOrNull(1)?.text() ?: "",
                src = document.selectFirst("div#VideoOption${nmopt}")
                    ?.selectFirst("iframe")
                    ?.attr("src")
                    ?: "",
            )
        }

        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val link = if (server.src.contains("trembed")) {
            service.getLink(server.src)
                .selectFirst("body iframe")
                ?.attr("src")
                ?: ""
        } else {
            server.src
        }

        return Extractor.extract(link)
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


    private interface AllMoviesForYouService {

        companion object {
            fun build(): AllMoviesForYouService {
                val client = OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(AllMoviesForYouService::class.java)
            }
        }


        @GET(".")
        suspend fun getHome(): Document

        @GET("page/{page}")
        suspend fun search(@Path("page") page: Int, @Query("s") query: String): Document

        @GET("movies/page/{page}")
        suspend fun getMovies(@Path("page") page: Int): Document

        @GET("series/page/{page}")
        suspend fun getTvShows(@Path("page") page: Int): Document


        @GET("movies/{slug}")
        suspend fun getMovie(@Path("slug") slug: String): Document


        @GET("series/{slug}")
        suspend fun getTvShow(@Path("slug") slug: String): Document

        @GET("season/{id}")
        suspend fun getSeasonEpisodes(@Path("id") seasonId: String): Document

        @GET("episode/{id}")
        suspend fun getEpisode(@Path("id") id: String): Document


        @GET("category/{id}/page/{page}")
        suspend fun getGenre(@Path("id") id: String, @Path("page") page: Int): Document


        @GET("cast/{slug}/page/{page}")
        suspend fun getCast(@Path("slug") slug: String, @Path("page") page: Int): Document

        @GET("cast_tv/{slug}/page/{page}")
        suspend fun getCastTv(@Path("slug") slug: String, @Path("page") page: Int): Document


        @GET
        suspend fun getLink(@Url url: String): Document
    }
}