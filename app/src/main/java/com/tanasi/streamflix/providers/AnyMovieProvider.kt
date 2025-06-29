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
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

object AnyMovieProvider : Provider {

    private const val URL = "https://anymovie.cc/"
    override val baseUrl = URL
    override val name = "AnyMovie"
    override val logo = "$URL/wp-content/uploads/2023/08/AM-LOGO-1.png"
    override val language = "en"

    private var _wpsearch = ""

    private val service = AllMoviesForYouService.build()


    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        Regex("\"nonce\":\"(.*?)\"").find(document.toString())
            ?.groupValues?.get(1)
            ?.let {
                _wpsearch = it
            }

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = Category.FEATURED,
                list = document.select("div#home-slider div.swiper-slide").mapNotNull {
                    val id = it.selectFirst("ul.rw a")
                        ?.attr("href")
                        ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                    val title = it.selectFirst("h2.entry-title")
                        ?.text() ?: ""
                    val overview = it.selectFirst("div.entry-content")
                        ?.text()
                    val released = it.selectFirst("span.year")
                        ?.text()
                    val runtime = it.selectFirst("span.duration")
                        ?.text()?.toMinutes()
                    val rating = it.selectFirst("span.rating.fa-star")
                        ?.text()?.toDoubleOrNull()
                    val banner = it.selectFirst("div.bg")
                        ?.attr("style")
                        ?.substringAfter("url(")?.substringBefore(");")
                        ?.toSafeUrl()

                    val genres = it.select("span.categories").map { element ->
                        Genre(
                            id = element.selectFirst("a")
                                ?.attr("href")
                                ?.substringBeforeLast("/")?.substringAfterLast("/")
                                ?: "",
                            name = element.text(),
                        )
                    }

                    val href = it.selectFirst("ul.rw a")
                        ?.attr("href")
                        ?: ""
                    when {
                        href.contains("/movies/") -> {
                            Movie(
                                id = id,
                                title = title,
                                overview = overview,
                                released = released,
                                runtime = runtime,
                                rating = rating,
                                banner = banner,

                                genres = genres,
                            )
                        }
                        href.contains("/series/") -> {
                            TvShow(
                                id = id,
                                title = title,
                                overview = overview,
                                released = released,
                                runtime = runtime,
                                rating = rating,
                                banner = banner,

                                genres = genres,
                            )
                        }
                        else -> null
                    }
                },
            )
        )

        document.select("section.section").forEach { section ->
            val category = Category(
                name = section.selectFirst("h2.section-title")?.text()?.trim() ?: "",
                list = section.select("div.swiper-slide").mapNotNull {
                    val id = it.selectFirst("ul.rw a")
                        ?.attr("href")
                        ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                    val title = it.selectFirst("h2.entry-title")
                        ?.text() ?: ""
                    val overview = it.selectFirst("div.entry-content")
                        ?.text()
                    val released = it.selectFirst("span.year")
                        ?.text()
                    val runtime = it.selectFirst("span.duration")
                        ?.text()?.toMinutes()
                    val quality = it.selectFirst("span.quality")
                        ?.text()
                    val rating = it.selectFirst("span.rating.fa-star")
                        ?.text()?.toDoubleOrNull()
                    val poster = it.selectFirst("div.post-thumbnail img")
                        ?.attr("src")
                        ?.toSafeUrl()

                    val genres = it.select("li.rw.sm")
                        .find { element -> element.selectFirst("span")?.text() == "Genres" }
                        ?.select("a")?.map { element ->
                            Genre(
                                id = element.attr("href")
                                    .substringBeforeLast("/").substringAfterLast("/"),
                                name = element.text(),
                            )
                        } ?: listOf()
                    val directors = it.select("li.rw.sm")
                        .find { element -> element.selectFirst("span")?.text() == "Director" }
                        ?.select("a")?.map { element ->
                            People(
                                id = element.attr("href")
                                    .substringBeforeLast("/").substringAfterLast("/"),
                                name = element.text(),
                            )
                        } ?: listOf()
                    val cast = it.select("li.rw.sm")
                        .find { element -> element.selectFirst("span")?.text() == "Cast" }
                        ?.select("a")?.map { element ->
                            People(
                                id = element.attr("href")
                                    .substringBeforeLast("/").substringAfterLast("/"),
                                name = element.text(),
                            )
                        } ?: listOf()

                    val href = it.selectFirst("ul.rw a")
                        ?.attr("href")
                        ?: ""
                    when {
                        href.contains("/movies/") -> {
                            Movie(
                                id = id,
                                title = title,
                                overview = overview,
                                released = released,
                                runtime = runtime,
                                quality = quality,
                                rating = rating,
                                poster = poster,

                                genres = genres,
                                directors = directors,
                                cast = cast,
                            )
                        }
                        href.contains("/series/") -> {
                            TvShow(
                                id = id,
                                title = title,
                                overview = overview,
                                released = released,
                                runtime = runtime,
                                quality = quality,
                                rating = rating,
                                poster = poster,

                                genres = genres,
                            )
                        }
                        else -> null
                    }
                },
            )

            categories.add(category)
        }

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.search("")

            val genres = document.selectFirst("ul.fg1 li")
                ?.select("li")?.map {
                    Genre(
                        id = it.attr("data-genre"),
                        name = it.text(),
                    )
                }
                ?.distinctBy { it.id }
                ?.sortedBy { it.name }
                ?: emptyList()

            return genres
        }

        val response = service.api(JSONObject(mapOf(
            "_wpsearch" to _wpsearch,
            "taxonomy" to "none",
            "search" to query,
            "term" to "none",
            "type" to "mixed",
            "genres" to emptyList<String>(),
            "years" to emptyList<String>(),
            "sort" to "1",
            "page" to page
        )).toString())

        val results = Jsoup.parse(response.html).select("article.movies").mapNotNull {
            val id = it.selectFirst("ul.rw a")
                ?.attr("href")
                ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
            val title = it.selectFirst("h2.entry-title")
                ?.text() ?: ""
            val overview = it.selectFirst("div.entry-content")
                ?.text()
            val released = it.selectFirst("span.year")
                ?.text()
            val runtime = it.selectFirst("span.duration")
                ?.text()?.toMinutes()
            val quality = it.selectFirst("span.quality")
                ?.text()
            val rating = it.selectFirst("span.rating.fa-star")
                ?.text()?.toDoubleOrNull()
            val poster = it.selectFirst("div.post-thumbnail img")
                ?.attr("src")
                ?.toSafeUrl()

            val genres = it.select("li.rw.sm")
                .find { element -> element.selectFirst("span")?.text() == "Genres" }
                ?.select("a")?.map { element ->
                    Genre(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                } ?: listOf()
            val directors = it.select("li.rw.sm")
                .find { element -> element.selectFirst("span")?.text() == "Director" }
                ?.select("a")?.map { element ->
                    People(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                } ?: listOf()
            val cast = it.select("li.rw.sm")
                .find { element -> element.selectFirst("span")?.text() == "Cast" }
                ?.select("a")?.map { element ->
                    People(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                } ?: listOf()

            val href = it.selectFirst("ul.rw a")
                ?.attr("href")
                ?: ""
            when {
                href.contains("/movies/") -> {
                    Movie(
                        id = id,
                        title = title,
                        overview = overview,
                        released = released,
                        runtime = runtime,
                        quality = quality,
                        rating = rating,
                        poster = poster,

                        genres = genres,
                        directors = directors,
                        cast = cast,
                    )
                }
                href.contains("/series/") -> {
                    TvShow(
                        id = id,
                        title = title,
                        overview = overview,
                        released = released,
                        runtime = runtime,
                        quality = quality,
                        rating = rating,
                        poster = poster,

                        genres = genres,
                    )
                }
                else -> null
            }
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val response = service.api(JSONObject(mapOf(
            "_wpsearch" to _wpsearch,
            "taxonomy" to "none",
            "search" to "",
            "term" to "none",
            "type" to "movies",
            "genres" to emptyList<String>(),
            "years" to emptyList<String>(),
            "sort" to "1",
            "page" to page
        )).toString())

        val movies = Jsoup.parse(response.html).select("article.movies").map {
            Movie(
                id = it.selectFirst("ul.rw a")
                    ?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                title = it.selectFirst("h2.entry-title")
                    ?.text() ?: "",
                overview = it.selectFirst("div.entry-content")
                    ?.text(),
                released = it.selectFirst("span.year")
                    ?.text(),
                runtime = it.selectFirst("span.duration")
                    ?.text()?.toMinutes(),
                quality = it.selectFirst("span.quality")
                    ?.text(),
                rating = it.selectFirst("span.rating.fa-star")
                    ?.text()?.toDoubleOrNull(),
                poster = it.selectFirst("div.post-thumbnail img")
                    ?.attr("src")
                    ?.toSafeUrl(),

                genres = it.select("li.rw.sm")
                    .find { element -> element.selectFirst("span")?.text() == "Genres" }
                    ?.select("a")?.map { element ->
                        Genre(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    } ?: listOf(),
                directors = it.select("li.rw.sm")
                    .find { element -> element.selectFirst("span")?.text() == "Director" }
                    ?.select("a")?.map { element ->
                        People(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    } ?: listOf(),
                cast = it.select("li.rw.sm")
                    .find { element -> element.selectFirst("span")?.text() == "Cast" }
                    ?.select("a")?.map { element ->
                        People(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    } ?: listOf()
            )
        }

        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val response = service.api(JSONObject(mapOf(
            "_wpsearch" to _wpsearch,
            "taxonomy" to "none",
            "search" to "",
            "term" to "none",
            "type" to "series",
            "genres" to emptyList<String>(),
            "years" to emptyList<String>(),
            "sort" to "1",
            "page" to page
        )).toString())

        val tvShows = Jsoup.parse(response.html).select("article.movies").map {
            TvShow(
                id = it.selectFirst("ul.rw a")
                    ?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                title = it.selectFirst("h2.entry-title")
                    ?.text() ?: "",
                overview = it.selectFirst("div.entry-content")
                    ?.text(),
                released = it.selectFirst("span.year")
                    ?.text(),
                runtime = it.selectFirst("span.duration")
                    ?.text()?.toMinutes(),
                quality = it.selectFirst("span.quality")
                    ?.text(),
                rating = it.selectFirst("span.rating.fa-star")
                    ?.text()?.toDoubleOrNull(),
                poster = it.selectFirst("div.post-thumbnail img")
                    ?.attr("src")
                    ?.toSafeUrl(),

                genres = it.select("li.rw.sm")
                    .find { element -> element.selectFirst("span")?.text() == "Genres" }
                    ?.select("a")?.map { element ->
                        Genre(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    } ?: listOf(),
                directors = it.select("li.rw.sm")
                    .find { element -> element.selectFirst("span")?.text() == "Director" }
                    ?.select("a")?.map { element ->
                        People(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    } ?: listOf(),
                cast = it.select("li.rw.sm")
                    .find { element -> element.selectFirst("span")?.text() == "Cast" }
                    ?.select("a")?.map { element ->
                        People(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    } ?: listOf()
            )
        }

        return tvShows
    }


    override suspend fun getMovie(id: String): Movie {
        val document = service.getMovie(id)

        val movie = Movie(
            id = id,
            title = document.selectFirst("h1.entry-title")
                ?.text() ?: "",
            overview = document.selectFirst("div.entry-content")
                ?.text(),
            released = document.selectFirst("span.year")
                ?.text(),
            runtime = document.selectFirst("span.duration")
                ?.text()?.toMinutes(),
            trailer = Regex("src=\"https://www.youtube.com/embed/(.*)\"").find(document.toString())
                ?.groupValues?.get(1)
                ?.let { "https://www.youtube.com/watch?v=${it}" },
            quality = document.selectFirst("span.quality")
                ?.text(),
            rating = document.selectFirst("span.rating")
                ?.text()?.toDoubleOrNull(),
            poster = document.selectFirst("div.post-thumbnail img")
                ?.attr("src"),

            genres = document.select("article.single ul.details-lst li.rw.sm")
                .find { it.selectFirst("span")?.text() == "Genres" }
                ?.select("a")?.map { element ->
                    Genre(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                } ?: listOf(),
            directors = document.select("article.single ul.details-lst li.rw.sm")
                .find { it.selectFirst("span")?.text() == "Director" }
                ?.select("a")?.map { element ->
                    People(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                } ?: listOf(),
            cast = document.select("article.single ul.details-lst li.rw.sm")
                .find { it.selectFirst("span")?.text() == "Cast" }
                ?.select("a")?.map { element ->
                    People(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                } ?: listOf(),
            recommendations = document.select("article.movies").mapNotNull {
                val showId = it.selectFirst("a")
                    ?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                val showTitle = it.selectFirst("h2.entry-title")
                    ?.text() ?: ""
                val showReleased = it.selectFirst("span.year")
                    ?.text()
                val showPoster = it.selectFirst("div.post-thumbnail img")
                    ?.attr("src")
                    ?.toSafeUrl()

                val href = it.selectFirst("a")
                    ?.attr("href")
                    ?: ""
                when {
                    href.contains("/movies/") -> {
                        Movie(
                            id = showId,
                            title = showTitle,
                            released = showReleased,
                            poster = showPoster,
                        )
                    }
                    href.contains("/series/") -> {
                        TvShow(
                            id = showId,
                            title = showTitle,
                            released = showReleased,
                            poster = showPoster,
                        )
                    }
                    else -> null
                }
            },
        )

        return movie
    }


    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTvShow(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("h1.entry-title")
                ?.text() ?: "",
            overview = document.selectFirst("div.entry-content")
                ?.text(),
            released = document.selectFirst("span.year")
                ?.text(),
            runtime = document.selectFirst("span.duration")
                ?.text()?.toMinutes(),
            trailer = Regex("src=\"https://www.youtube.com/embed/(.*)\"").find(document.toString())
                ?.groupValues?.get(1)
                ?.let { "https://www.youtube.com/watch?v=${it}" },
            quality = document.selectFirst("span.quality")
                ?.text(),
            rating = document.selectFirst("span.rating")
                ?.text()?.toDoubleOrNull(),
            poster = document.selectFirst("div.post-thumbnail img")
                ?.attr("src"),

            seasons = document.select("div.seasons div.seasons-bx").mapIndexed { index, it ->
                Season(
                    id = "$id/$index",
                    number = it.selectFirst("div p span")
                        ?.text()?.toIntOrNull() ?: 0,
                    title = it.selectFirst("div p")
                        ?.text(),
                    poster = it.selectFirst("img")
                        ?.attr("src")?.replace("w92", "w500"),
                )
            },
            genres = document.select("article.single ul.details-lst li.rw.sm")
                .find { it.selectFirst("span")?.text() == "Genres" }
                ?.select("a")?.map { element ->
                    Genre(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                } ?: listOf(),
            directors = document.select("article.single ul.details-lst li.rw.sm")
                .find { it.selectFirst("span")?.text() == "Director" }
                ?.select("a")?.map { element ->
                    People(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                } ?: listOf(),
            cast = document.select("article.single ul.details-lst li.rw.sm")
                .find { it.selectFirst("span")?.text() == "Cast" }
                ?.select("a")?.map { element ->
                    People(
                        id = element.attr("href")
                            .substringBeforeLast("/").substringAfterLast("/"),
                        name = element.text(),
                    )
                } ?: listOf(),
            recommendations = document.select("article.movies").mapNotNull {
                val showId = it.selectFirst("a")
                    ?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                val showTitle = it.selectFirst("h2.entry-title")
                    ?.text() ?: ""
                val showReleased = it.selectFirst("span.year")
                    ?.text()
                val showPoster = it.selectFirst("div.post-thumbnail img")
                    ?.attr("src")
                    ?.toSafeUrl()

                val href = it.selectFirst("a")
                    ?.attr("href")
                    ?: ""
                when {
                    href.contains("/movies/") -> {
                        Movie(
                            id = showId,
                            title = showTitle,
                            released = showReleased,
                            poster = showPoster,
                        )
                    }
                    href.contains("/series/") -> {
                        TvShow(
                            id = showId,
                            title = showTitle,
                            released = showReleased,
                            poster = showPoster,
                        )
                    }
                    else -> null
                }
            },
        )

        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val (tvShowId, seasonIndex) = seasonId.split("/")

        val document = service.getTvShow(tvShowId)

        val episodes = document.select("div.seasons div.seasons-bx").getOrNull(seasonIndex.toInt())
            ?.select("ul.seasons-lst li")?.map {
                Episode(
                    id = it.selectFirst("ul.rw a")
                        ?.attr("href")
                        ?.substringBeforeLast("/")?.substringAfterLast("/") ?: "",
                    number = it.selectFirst("h3.title > span")
                        ?.text()?.substringAfter("-E")?.toIntOrNull() ?: 0,
                    title = it.selectFirst("h3.title")
                        ?.ownText(),
                    released = it.selectFirst("span.date")
                        ?.text(),
                    poster = it.selectFirst("img")
                        ?.attr("src")?.toSafeUrl()?.replace("w185", "w500"),
                )
            } ?: emptyList()

        return episodes
    }


    override suspend fun getGenre(id: String, page: Int): Genre {
        val response = service.api(JSONObject(mapOf(
            "_wpsearch" to _wpsearch,
            "taxonomy" to "none",
            "search" to "",
            "term" to "none",
            "type" to "mixed",
            "genres" to listOf(id),
            "years" to emptyList<String>(),
            "sort" to "1",
            "page" to page
        )).toString())

        val genre = Genre(
            id = id,
            name = "",

            shows = Jsoup.parse(response.html).select("article.movies").mapNotNull {
                val showId = it.selectFirst("ul.rw a")
                    ?.attr("href")
                    ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                val title = it.selectFirst("h2.entry-title")
                    ?.text() ?: ""
                val overview = it.selectFirst("div.entry-content")
                    ?.text()
                val released = it.selectFirst("span.year")
                    ?.text()
                val runtime = it.selectFirst("span.duration")
                    ?.text()?.toMinutes()
                val rating = it.selectFirst("span.rating.fa-star")
                    ?.text()?.toDoubleOrNull()
                val poster = it.selectFirst("div.post-thumbnail img")
                    ?.attr("src")
                    ?.toSafeUrl()

                val genres = it.select("li.rw.sm")
                    .find { element -> element.selectFirst("span")?.text() == "Genres" }
                    ?.select("a")?.map { element ->
                        Genre(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    } ?: listOf()
                val directors = it.select("li.rw.sm")
                    .find { element -> element.selectFirst("span")?.text() == "Director" }
                    ?.select("a")?.map { element ->
                        People(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    } ?: listOf()
                val cast = it.select("li.rw.sm")
                    .find { element -> element.selectFirst("span")?.text() == "Cast" }
                    ?.select("a")?.map { element ->
                        People(
                            id = element.attr("href")
                                .substringBeforeLast("/").substringAfterLast("/"),
                            name = element.text(),
                        )
                    } ?: listOf()

                val href = it.selectFirst("ul.rw a")
                    ?.attr("href")
                    ?: ""
                when {
                    href.contains("/movies/") -> {
                        Movie(
                            id = showId,
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
                    href.contains("/series/") -> {
                        TvShow(
                            id = showId,
                            title = title,
                            overview = overview,
                            released = released,
                            runtime = runtime,
                            rating = rating,
                            poster = poster,

                            genres = genres,
                        )
                    }
                    else -> null
                }
            },
        )

        return genre
    }


    override suspend fun getPeople(id: String, page: Int): People {
        if (page > 1) {
            // TODO: Not implemented yet
            return People(id, "")
        }

        val castDocument = try {
            service.getCast(id)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> null
                else -> throw e
            }
        }
        val castTvDocument = try {
            service.getCastTv(id)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> null
                else -> throw e
            }
        }

        val people = People(
            id = id,
            name = castDocument?.selectFirst("h1.section-title > span")
                ?.text() ?: "",

            filmography = listOfNotNull(
                castDocument?.select("article.movies"),
                castTvDocument?.select("article.movies"),
            ).flatten()
                .mapNotNull {
                    val showId = it.selectFirst("ul.rw a")
                        ?.attr("href")
                        ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                    val title = it.selectFirst("h2.entry-title")
                        ?.text() ?: ""
                    val overview = it.selectFirst("div.entry-content")
                        ?.text()
                    val released = it.selectFirst("span.year")
                        ?.text()
                    val runtime = it.selectFirst("span.duration")
                        ?.text()?.toMinutes()
                    val quality = it.selectFirst("span.quality")
                        ?.text()
                    val rating = it.selectFirst("span.rating.fa-star")
                        ?.text()?.toDoubleOrNull()
                    val poster = it.selectFirst("div.post-thumbnail img")
                        ?.attr("src")
                        ?.toSafeUrl()

                    val genres = it.select("li.rw.sm")
                        .find { element -> element.selectFirst("span")?.text() == "Genres" }
                        ?.select("a")?.map { element ->
                            Genre(
                                id = element.attr("href")
                                    .substringBeforeLast("/").substringAfterLast("/"),
                                name = element.text(),
                            )
                        } ?: listOf()
                    val directors = it.select("li.rw.sm")
                        .find { element -> element.selectFirst("span")?.text() == "Director" }
                        ?.select("a")?.map { element ->
                            People(
                                id = element.attr("href")
                                    .substringBeforeLast("/").substringAfterLast("/"),
                                name = element.text(),
                            )
                        } ?: listOf()
                    val cast = it.select("li.rw.sm")
                        .find { element -> element.selectFirst("span")?.text() == "Cast" }
                        ?.select("a")?.map { element ->
                            People(
                                id = element.attr("href")
                                    .substringBeforeLast("/").substringAfterLast("/"),
                                name = element.text(),
                            )
                        } ?: listOf()

                    val href = it.selectFirst("ul.rw a")
                        ?.attr("href")
                        ?: ""
                    when {
                        href.contains("/movies/") -> {
                            Movie(
                                id = showId,
                                title = title,
                                overview = overview,
                                released = released,
                                runtime = runtime,
                                quality = quality,
                                rating = rating,
                                poster = poster,

                                genres = genres,
                                directors = directors,
                                cast = cast,
                            )
                        }
                        href.contains("/series/") -> {
                            TvShow(
                                id = showId,
                                title = title,
                                overview = overview,
                                released = released,
                                runtime = runtime,
                                quality = quality,
                                rating = rating,
                                poster = poster,

                                genres = genres,
                            )
                        }
                        else -> null
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

        val servers = document.select("aside.options li").mapIndexed { index, it ->
            Video.Server(
                id = it.attr("data-id"),
                name = it.selectFirst("span.option")?.text() ?: "",
                src = document.selectFirst("div.player div.fg${index + 1}")
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
                    .baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(AllMoviesForYouService::class.java)
            }
        }


        @GET(".")
        suspend fun getHome(): Document

        @GET(".")
        suspend fun search(@Query("s") s: String): Document

        @POST("https://anymovie.cc/wp-admin/admin-ajax.php")
        @FormUrlEncoded
        suspend fun api(
            @Field("vars") vars: String,
            @Field("action") action: String = "action_search",
        ): SearchResponse


        @GET("movies/{slug}")
        suspend fun getMovie(@Path("slug") slug: String): Document


        @GET("series/{slug}")
        suspend fun getTvShow(@Path("slug") slug: String): Document

        @GET("episode/{id}")
        suspend fun getEpisode(@Path("id") id: String): Document


        @GET("cast/{slug}")
        suspend fun getCast(@Path("slug") slug: String): Document

        @GET("cast_tv/{slug}")
        suspend fun getCastTv(@Path("slug") slug: String): Document


        @GET
        suspend fun getLink(@Url url: String): Document


        data class SearchResponse(
            val next: Boolean,
            val html: String,
        )
    }
}