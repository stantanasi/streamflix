package com.tanasi.sflix.providers

import android.util.Base64
import com.google.gson.*
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.sflix.models.*
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SflixProvider {

    private val sflixService = SflixService.build()


    suspend fun getHome(): List<Category> {
        val document = sflixService.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = "Featured",
                list = document
                    .select("div.swiper-wrapper > div.swiper-slide")
                    .map {
                        val isMovie = it.selectFirst("a")
                            ?.attr("href")
                            ?.contains("/movie/")
                            ?: false

                        val id = it.selectFirst("a")
                            ?.attr("href")
                            ?.substringAfterLast("-") ?: ""
                        val title = it.select("h2.film-title").text()
                        val overview = it.selectFirst("p.sc-desc")?.text() ?: ""
                        val poster = it.selectFirst("img.film-poster-img")
                            ?.attr("src") ?: ""
                        val banner = it.selectFirst("div.slide-photo img")
                            ?.attr("src") ?: ""

                        when (isMovie) {
                            true -> {
                                val info = it
                                    .select("div.sc-detail > div.scd-item")
                                    .toList()
                                    .map { element -> element.text() }
                                    .let { info ->
                                        object {
                                            val rating = when (info.size) {
                                                1 -> info[0].toDoubleOrNull()
                                                2 -> info[0].toDoubleOrNull()
                                                3 -> info[0].toDoubleOrNull()
                                                else -> null
                                            }
                                            val quality = when (info.size) {
                                                2 -> info[1] ?: ""
                                                3 -> info[1] ?: ""
                                                else -> null
                                            }
                                            val released = when (info.size) {
                                                3 -> info[2] ?: ""
                                                else -> null
                                            }
                                        }
                                    }

                                Movie(
                                    id = id,
                                    title = title,
                                    overview = overview,
                                    released = info.released,
                                    quality = info.quality ?: "",
                                    rating = info.rating,
                                    poster = poster,
                                    banner = banner,
                                )
                            }
                            false -> {
                                val info = it
                                    .select("div.sc-detail > div.scd-item")
                                    .toList()
                                    .map { element -> element.text() }
                                    .let { info ->
                                        object {
                                            val rating = when (info.size) {
                                                1 -> info[0].toDoubleOrNull()
                                                2 -> info[0].toDoubleOrNull()
                                                3 -> info[0].toDoubleOrNull()
                                                else -> null
                                            }
                                            val quality = when (info.size) {
                                                3 -> info[1] ?: ""
                                                else -> null
                                            }
                                            val lastEpisode = when (info.size) {
                                                2 -> info[1] ?: ""
                                                3 -> info[2] ?: ""
                                                else -> null
                                            }
                                        }
                                    }

                                TvShow(
                                    id = id,
                                    title = title,
                                    overview = overview,
                                    quality = info.quality ?: "",
                                    rating = info.rating,
                                    poster = poster,
                                    banner = banner,

                                    seasons = info.lastEpisode?.let { lastEpisode ->
                                        listOf(
                                            Season(
                                                id = "",
                                                number = lastEpisode
                                                    .substringAfter("S")
                                                    .substringBefore(" :")
                                                    .toIntOrNull() ?: 0,

                                                episodes = listOf(
                                                    Episode(
                                                        id = "",
                                                        number = lastEpisode
                                                            .substringAfter(":")
                                                            .substringAfter("E")
                                                            .toIntOrNull() ?: 0,
                                                    )
                                                )
                                            )
                                        )
                                    } ?: listOf(),
                                )
                            }
                        }
                    },
            )
        )

        categories.add(
            Category(
                name = "Trending Movies",
                list = document
                    .select("div#trending-movies")
                    .select("div.flw-item")
                    .map {
                        val info = it
                            .select("div.film-detail > div.fd-infor > span")
                            .toList()
                            .map { element -> element.text() }
                            .let { info ->
                                object {
                                    val released = when (info.size) {
                                        1 -> info[0] ?: ""
                                        2 -> info[1] ?: ""
                                        3 -> info[2] ?: ""
                                        else -> null
                                    }
                                    val quality = when (info.size) {
                                        3 -> info[1] ?: ""
                                        else -> null
                                    }
                                    val rating = when (info.size) {
                                        2 -> info[0].toDoubleOrNull()
                                        3 -> info[0].toDoubleOrNull()
                                        else -> null
                                    }
                                }
                            }

                        Movie(
                            id = it.selectFirst("a")
                                ?.attr("href")
                                ?.substringAfterLast("-")
                                ?: "",
                            title = it.select("h3.film-name").text(),
                            released = info.released ?: "",
                            quality = info.quality ?: "",
                            rating = info.rating,
                            poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                ?.attr("data-src")
                                ?: "",
                        )
                    },
            )
        )

        categories.add(
            Category(
                name = "Trending TV Shows",
                list = document
                    .select("div#trending-tv")
                    .select("div.flw-item")
                    .map {
                        val info = it
                            .select("div.film-detail > div.fd-infor > span")
                            .toList()
                            .map { element -> element.text() }
                            .let { info ->
                                object {
                                    val quality = when (info.size) {
                                        3 -> info[1] ?: ""
                                        else -> null
                                    }
                                    val rating = when (info.size) {
                                        2 -> info[0].toDoubleOrNull()
                                        3 -> info[0].toDoubleOrNull()
                                        else -> null
                                    }
                                    val lastEpisode = when (info.size) {
                                        1 -> info[0] ?: ""
                                        2 -> info[1] ?: ""
                                        3 -> info[2] ?: ""
                                        else -> null
                                    }
                                }
                            }

                        TvShow(
                            id = it.selectFirst("a")
                                ?.attr("href")
                                ?.substringAfterLast("-")
                                ?: "",
                            title = it.select("h3.film-name").text(),
                            quality = info.quality ?: "",
                            rating = info.rating,
                            poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                ?.attr("data-src")
                                ?: "",

                            seasons = info.lastEpisode?.let { lastEpisode ->
                                listOf(
                                    Season(
                                        id = "",
                                        number = lastEpisode
                                            .substringAfter("S")
                                            .substringBefore(":")
                                            .toIntOrNull() ?: 0,

                                        episodes = listOf(
                                            Episode(
                                                id = "",
                                                number = lastEpisode
                                                    .substringAfter(":")
                                                    .substringAfter("E")
                                                    .toIntOrNull() ?: 0,
                                            )
                                        )
                                    )
                                )
                            } ?: listOf()
                        )
                    },
            )
        )

        categories.add(
            Category(
                name = "Latest Movies",
                list = document
                    .select(".section-id-02:has(h2:matchesOwn(Latest Movies))")
                    .select("div.flw-item")
                    .map {
                        val info = it
                            .select("div.film-detail > div.fd-infor > span")
                            .toList()
                            .map { element -> element.text() }
                            .let { info ->
                                object {
                                    val released = when (info.size) {
                                        1 -> info[0] ?: ""
                                        2 -> info[1] ?: ""
                                        3 -> info[2] ?: ""
                                        else -> null
                                    }
                                    val quality = when (info.size) {
                                        3 -> info[1] ?: ""
                                        else -> null
                                    }
                                    val rating = when (info.size) {
                                        2 -> info[0].toDoubleOrNull()
                                        3 -> info[0].toDoubleOrNull()
                                        else -> null
                                    }
                                }
                            }

                        Movie(
                            id = it.selectFirst("a")
                                ?.attr("href")
                                ?.substringAfterLast("-")
                                ?: "",
                            title = it.select("h3.film-name").text(),
                            released = info.released ?: "",
                            quality = info.quality ?: "",
                            rating = info.rating,
                            poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                ?.attr("data-src")
                                ?: "",
                        )
                    },
            )
        )

        categories.add(
            Category(
                name = "Latest TV Shows",
                list = document
                    .select(".section-id-02:has(h2:matchesOwn(Latest TV Shows))")
                    .select("div.flw-item")
                    .map {
                        val info = it
                            .select("div.film-detail > div.fd-infor > span")
                            .toList()
                            .map { element -> element.text() }
                            .let { info ->
                                object {
                                    val quality = when (info.size) {
                                        3 -> info[1] ?: ""
                                        else -> null
                                    }
                                    val rating = when (info.size) {
                                        2 -> info[0].toDoubleOrNull()
                                        3 -> info[0].toDoubleOrNull()
                                        else -> null
                                    }
                                    val lastEpisode = when (info.size) {
                                        1 -> info[0] ?: ""
                                        2 -> info[1] ?: ""
                                        3 -> info[2] ?: ""
                                        else -> null
                                    }
                                }
                            }

                        TvShow(
                            id = it.selectFirst("a")
                                ?.attr("href")
                                ?.substringAfterLast("-")
                                ?: "",
                            title = it.select("h3.film-name").text(),
                            quality = info.quality ?: "",
                            rating = info.rating,
                            poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                ?.attr("data-src")
                                ?: "",

                            seasons = info.lastEpisode?.let { lastEpisode ->
                                listOf(
                                    Season(
                                        id = "",
                                        number = lastEpisode
                                            .substringAfter("S")
                                            .substringBefore(":")
                                            .toIntOrNull() ?: 0,

                                        episodes = listOf(
                                            Episode(
                                                id = "",
                                                number = lastEpisode
                                                    .substringAfter(":")
                                                    .substringAfter("E")
                                                    .toIntOrNull() ?: 0,
                                            )
                                        )
                                    )
                                )
                            } ?: listOf()
                        )
                    },
            )
        )

        return categories
    }

    suspend fun search(query: String): List<Show> {
        if (query.isEmpty()) return listOf()

        val document = sflixService.search(query.replace(" ", "-"))

        val results = document.select("div.flw-item").map {
            val isMovie = it.selectFirst("a")
                ?.attr("href")
                ?.contains("/movie/")
                ?: false

            val id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: ""
            val title = it.select("h2.film-name").text()
            val poster =
                it.selectFirst("div.film-poster > img.film-poster-img").let { img ->
                    img?.attr("data-src") ?: img?.attr("src")
                } ?: ""

            when (isMovie) {
                true -> {
                    val info = it
                        .select("div.film-detail > div.fd-infor > span")
                        .toList()
                        .map { element -> element.text() }
                        .let { info ->
                            object {
                                val released = when (info.size) {
                                    1 -> info[0] ?: ""
                                    2 -> info[1] ?: ""
                                    3 -> info[2] ?: ""
                                    else -> null
                                }
                                val quality = when (info.size) {
                                    3 -> info[1] ?: ""
                                    else -> null
                                }
                                val rating = when (info.size) {
                                    2 -> info[0].toDoubleOrNull()
                                    3 -> info[0].toDoubleOrNull()
                                    else -> null
                                }
                            }
                        }

                    Movie(
                        id = id,
                        title = title,
                        released = info.released,
                        quality = info.quality ?: "",
                        rating = info.rating,
                        poster = poster,
                    )
                }
                false -> {
                    val info = it
                        .select("div.film-detail > div.fd-infor > span")
                        .toList()
                        .map { element -> element.text() }
                        .let { info ->
                            object {
                                val quality = when (info.size) {
                                    3 -> info[1] ?: ""
                                    else -> null
                                }
                                val rating = when (info.size) {
                                    2 -> info[0].toDoubleOrNull()
                                    3 -> info[0].toDoubleOrNull()
                                    else -> null
                                }
                                val lastEpisode = when (info.size) {
                                    1 -> info[0] ?: ""
                                    2 -> info[1] ?: ""
                                    3 -> info[2] ?: ""
                                    else -> null
                                }
                            }
                        }

                    TvShow(
                        id = id,
                        title = title,
                        quality = info.quality ?: "",
                        rating = info.rating,
                        poster = poster,

                        seasons = info.lastEpisode?.let { lastEpisode ->
                            listOf(
                                Season(
                                    id = "",
                                    number = lastEpisode
                                        .substringAfter("S")
                                        .substringBefore(":")
                                        .toIntOrNull() ?: 0,

                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = lastEpisode
                                                .substringAfter(":")
                                                .substringAfter("E")
                                                .toIntOrNull() ?: 0,
                                        )
                                    )
                                )
                            )
                        } ?: listOf(),
                    )
                }
            }
        }

        return results
    }

    suspend fun getMovies(): List<Movie> {
        val document = sflixService.getMovies()

        val movies = document
            .select("div.flw-item")
            .map {
                val info = it
                    .select("div.film-detail > div.fd-infor > span")
                    .toList()
                    .map { element -> element.text() }
                    .let { info ->
                        object {
                            val released = when (info.size) {
                                1 -> info[0] ?: ""
                                2 -> info[1] ?: ""
                                3 -> info[2] ?: ""
                                else -> null
                            }
                            val quality = when (info.size) {
                                3 -> info[1] ?: ""
                                else -> null
                            }
                            val rating = when (info.size) {
                                2 -> info[0].toDoubleOrNull()
                                3 -> info[0].toDoubleOrNull()
                                else -> null
                            }
                        }
                    }

                Movie(
                    id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: "",
                    title = it.select("h2.film-name").text(),
                    released = info.released ?: "",
                    quality = info.quality ?: "",
                    rating = info.rating,
                    poster = it.selectFirst("div.film-poster > img.film-poster-img")
                        .let { img ->
                            img?.attr("data-src") ?: img?.attr("src")
                        } ?: "",
                )
            }

        return movies
    }

    suspend fun getTvShows(): List<TvShow> {
        val document = sflixService.getTvShows()

        val tvShows = document
            .select("div.flw-item")
            .map {
                val info = it
                    .select("div.film-detail > div.fd-infor > span")
                    .toList()
                    .map { element -> element.text() }
                    .let { info ->
                        object {
                            val quality = when (info.size) {
                                3 -> info[1] ?: ""
                                else -> null
                            }
                            val rating = when (info.size) {
                                2 -> info[0].toDoubleOrNull()
                                3 -> info[0].toDoubleOrNull()
                                else -> null
                            }
                            val lastEpisode = when (info.size) {
                                1 -> info[0] ?: ""
                                2 -> info[1] ?: ""
                                3 -> info[2] ?: ""
                                else -> null
                            }
                        }
                    }

                TvShow(
                    id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: "",
                    title = it.select("h2.film-name").text(),
                    quality = info.quality ?: "",
                    rating = info.rating,
                    poster = it.selectFirst("div.film-poster > img.film-poster-img")
                        .let { img ->
                            img?.attr("data-src") ?: img?.attr("src")
                        } ?: "",

                    seasons = info.lastEpisode?.let { lastEpisode ->
                        listOf(
                            Season(
                                id = "",
                                number = lastEpisode
                                    .substringAfter("S")
                                    .substringBefore(":")
                                    .toIntOrNull() ?: 0,

                                episodes = listOf(
                                    Episode(
                                        id = "",
                                        number = lastEpisode
                                            .substringAfter(":")
                                            .substringAfter("E")
                                            .toIntOrNull() ?: 0,
                                    )
                                )
                            )
                        )
                    } ?: listOf()
                )
            }

        return tvShows
    }


    suspend fun getMovie(id: String): Movie {
        val document = sflixService.getMovieById(id)

        val movie = Movie(
            id = id,
            title = document.selectFirst("h2.heading-name")?.text() ?: "",
            overview = document.selectFirst("div.description")?.ownText() ?: "",
            released = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Released") ?: false }
                ?.ownText()
                ?.trim()
                ?: "",
            runtime = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Duration") ?: false }
                ?.ownText()
                ?.removeSuffix("min")
                ?.trim()
                ?.toIntOrNull(),
            youtubeTrailerId = document.selectFirst("iframe#iframe-trailer")
                ?.attr("data-src")
                ?.substringAfterLast("/"),
            quality = document.selectFirst(".fs-item > .quality")?.text()
                ?.trim() ?: "",
            rating = document.selectFirst(".fs-item > .imdb")?.text()
                ?.trim()
                ?.removePrefix("IMDB:")
                ?.toDoubleOrNull(),
            poster = document.selectFirst("div.detail_page-watch img.film-poster-img")
                ?.attr("src"),
            banner = document.selectFirst("div.detail-container > div.cover_follow")
                ?.attr("style")
                ?.substringAfter("background-image: url(")
                ?.substringBefore(");"),

            genres = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Genre") ?: false }
                ?.select("a")
                ?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            cast = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Casts") ?: false }
                ?.select("a")
                ?.map {
                    People(
                        id = it.attr("href").substringAfter("/cast/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            recommendations = document
                .select("div.film_related")
                .select("div.flw-item")
                .map {
                    val isMovie = it.selectFirst("a")
                        ?.attr("href")
                        ?.contains("/movie/")
                        ?: false

                    when (isMovie) {
                        true -> {
                            val info = it
                                .select("div.film-detail > div.fd-infor > span")
                                .toList()
                                .map { element -> element.text() }
                                .let { info ->
                                    object {
                                        val released = when (info.size) {
                                            1 -> info[0] ?: ""
                                            2 -> info[1] ?: ""
                                            3 -> info[2] ?: ""
                                            else -> null
                                        }
                                        val quality = when (info.size) {
                                            3 -> info[1] ?: ""
                                            else -> null
                                        }
                                        val rating = when (info.size) {
                                            2 -> info[0].toDoubleOrNull()
                                            3 -> info[0].toDoubleOrNull()
                                            else -> null
                                        }
                                    }
                                }

                            Movie(
                                id = it
                                    .selectFirst("a")
                                    ?.attr("href")
                                    ?.substringAfterLast("-")
                                    ?: "",
                                title = it.select("h3.film-name").text(),
                                released = info.released,
                                quality = info.quality ?: "",
                                rating = info.rating,
                                poster = it
                                    .selectFirst("div.film-poster > img.film-poster-img")
                                    .let { img ->
                                        img?.attr("data-src") ?: img?.attr("src")
                                    }
                                    ?: "",
                            )
                        }
                        false -> {
                            val info = it
                                .select("div.film-detail > div.fd-infor > span")
                                .toList()
                                .map { element -> element.text() }
                                .let { info ->
                                    object {
                                        val quality = when (info.size) {
                                            3 -> info[1] ?: ""
                                            else -> null
                                        }
                                        val rating = when (info.size) {
                                            2 -> info[0].toDoubleOrNull()
                                            3 -> info[0].toDoubleOrNull()
                                            else -> null
                                        }
                                        val lastEpisode = when (info.size) {
                                            1 -> info[0] ?: ""
                                            2 -> info[1] ?: ""
                                            3 -> info[2] ?: ""
                                            else -> null
                                        }
                                    }
                                }

                            TvShow(
                                id = it
                                    .selectFirst("a")
                                    ?.attr("href")
                                    ?.substringAfterLast("-")
                                    ?: "",
                                title = it.select("h3.film-name").text(),
                                quality = info.quality ?: "",
                                rating = info.rating,
                                poster = it
                                    .selectFirst("div.film-poster > img.film-poster-img")
                                    .let { img ->
                                        img?.attr("data-src") ?: img?.attr("src")
                                    }
                                    ?: "",

                                seasons = info.lastEpisode?.let { lastEpisode ->
                                    listOf(
                                        Season(
                                            id = "",
                                            number = lastEpisode
                                                .substringAfter("S")
                                                .substringBefore(":")
                                                .toIntOrNull() ?: 0,

                                            episodes = listOf(
                                                Episode(
                                                    id = "",
                                                    number = lastEpisode
                                                        .substringAfter(":")
                                                        .substringAfter("E")
                                                        .toIntOrNull() ?: 0,
                                                )
                                            )
                                        )
                                    )
                                } ?: listOf(),
                            )
                        }
                    }
                },
        )

        return movie
    }


    suspend fun getTvShow(id: String): TvShow {
        val document = sflixService.getTvShowById(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("h2.heading-name")?.text() ?: "",
            overview = document.selectFirst("div.description")?.ownText() ?: "",
            released = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Released") ?: false }
                ?.ownText()
                ?.trim()
                ?: "",
            runtime = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Duration") ?: false }
                ?.ownText()
                ?.removeSuffix("min")
                ?.trim()
                ?.toIntOrNull(),
            youtubeTrailerId = document.selectFirst("iframe#iframe-trailer")
                ?.attr("data-src")
                ?.substringAfterLast("/"),
            quality = document.selectFirst(".fs-item > .quality")?.text()
                ?.trim() ?: "",
            rating = document.selectFirst(".fs-item > .imdb")?.text()
                ?.trim()
                ?.removePrefix("IMDB:")
                ?.toDoubleOrNull(),
            poster = document.selectFirst("div.detail_page-watch img.film-poster-img")
                ?.attr("src"),
            banner = document.selectFirst("div.detail-container > div.cover_follow")
                ?.attr("style")
                ?.substringAfter("background-image: url(")
                ?.substringBefore(");"),

            seasons = sflixService.getTvShowSeasonsById(id)
                .select("div.dropdown-menu.dropdown-menu-model > a")
                .mapIndexed { seasonNumber, seasonElement ->
                    Season(
                        id = seasonElement.attr("data-id"),
                        number = seasonNumber + 1,
                        title = seasonElement.text(),
                    )
                },
            genres = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Genre") ?: false }
                ?.select("a")
                ?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            cast = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Casts") ?: false }
                ?.select("a")
                ?.map {
                    People(
                        id = it.attr("href").substringAfter("/cast/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            recommendations = document
                .select("div.film_related")
                .select("div.flw-item")
                .map {
                    val isMovie = it.selectFirst("a")
                        ?.attr("href")
                        ?.contains("/movie/")
                        ?: false

                    when (isMovie) {
                        true -> {
                            val info = it
                                .select("div.film-detail > div.fd-infor > span")
                                .toList()
                                .map { element -> element.text() }
                                .let { info ->
                                    object {
                                        val released = when (info.size) {
                                            1 -> info[0] ?: ""
                                            2 -> info[1] ?: ""
                                            3 -> info[2] ?: ""
                                            else -> null
                                        }
                                        val quality = when (info.size) {
                                            3 -> info[1] ?: ""
                                            else -> null
                                        }
                                        val rating = when (info.size) {
                                            2 -> info[0].toDoubleOrNull()
                                            3 -> info[0].toDoubleOrNull()
                                            else -> null
                                        }
                                    }
                                }

                            Movie(
                                id = it
                                    .selectFirst("a")
                                    ?.attr("href")
                                    ?.substringAfterLast("-")
                                    ?: "",
                                title = it.select("h3.film-name").text(),
                                released = info.released,
                                quality = info.quality ?: "",
                                rating = info.rating,
                                poster = it
                                    .selectFirst("div.film-poster > img.film-poster-img")
                                    .let { img ->
                                        img?.attr("data-src") ?: img?.attr("src")
                                    }
                                    ?: "",
                            )
                        }
                        false -> {
                            val info = it
                                .select("div.film-detail > div.fd-infor > span")
                                .toList()
                                .map { element -> element.text() }
                                .let { info ->
                                    object {
                                        val quality = when (info.size) {
                                            3 -> info[1] ?: ""
                                            else -> null
                                        }
                                        val rating = when (info.size) {
                                            2 -> info[0].toDoubleOrNull()
                                            3 -> info[0].toDoubleOrNull()
                                            else -> null
                                        }
                                        val lastEpisode = when (info.size) {
                                            1 -> info[0] ?: ""
                                            2 -> info[1] ?: ""
                                            3 -> info[2] ?: ""
                                            else -> null
                                        }
                                    }
                                }

                            TvShow(
                                id = it
                                    .selectFirst("a")
                                    ?.attr("href")
                                    ?.substringAfterLast("-")
                                    ?: "",
                                title = it.select("h3.film-name").text(),
                                quality = info.quality ?: "",
                                rating = info.rating,
                                poster = it
                                    .selectFirst("div.film-poster > img.film-poster-img")
                                    .let { img ->
                                        img?.attr("data-src") ?: img?.attr("src")
                                    }
                                    ?: "",

                                seasons = info.lastEpisode?.let { lastEpisode ->
                                    listOf(
                                        Season(
                                            id = "",
                                            number = lastEpisode
                                                .substringAfter("S")
                                                .substringBefore(":")
                                                .toIntOrNull() ?: 0,

                                            episodes = listOf(
                                                Episode(
                                                    id = "",
                                                    number = lastEpisode
                                                        .substringAfter(":")
                                                        .substringAfter("E")
                                                        .toIntOrNull() ?: 0,
                                                )
                                            )
                                        )
                                    )
                                } ?: listOf(),
                            )
                        }
                    }
                },
        )

        return tvShow
    }

    suspend fun getSeasonEpisodes(seasonId: String): List<Episode> {
        val document = sflixService.getSeasonEpisodesById(seasonId)

        val episodes = document
            .select("div.flw-item.film_single-item.episode-item.eps-item")
            .mapIndexed { episodeNumber, episodeElement ->
                val episodeId = episodeElement.attr("data-id")
                Episode(
                    id = episodeId,
                    number = episodeElement
                        .selectFirst("div.episode-number")
                        ?.text()
                        ?.substringAfter("Episode ")
                        ?.substringBefore(":")
                        ?.toIntOrNull()
                        ?: episodeNumber,
                    title = episodeElement
                        .selectFirst("h3.film-name")
                        ?.text()
                        ?: "",
                    poster = episodeElement.selectFirst("img")
                        ?.attr("src") ?: "",
                )
            }

        return episodes
    }


    suspend fun getPeople(id: String): People {
        val document = sflixService.getPeopleBySlug(id)

        val people = People(
            id = id,
            name = document.selectFirst("h2.cat-heading")?.text() ?: "",

            filmography = document.select("div.flw-item").map {
                val isMovie = it.selectFirst("a")
                    ?.attr("href")
                    ?.contains("/movie/")
                    ?: false

                val showId = it.selectFirst("a")?.attr("href")
                    ?.substringAfterLast("-")
                    ?: ""
                val showTitle = it.select("h2.film-name").text()
                val showPoster = it.selectFirst("div.film-poster > img.film-poster-img")
                    ?.attr("data-src")
                    ?: ""

                when (isMovie) {
                    true -> {
                        val info = it
                            .select("div.film-detail > div.fd-infor > span")
                            .toList()
                            .map { element -> element.text() }
                            .let { info ->
                                object {
                                    val released = when (info.size) {
                                        1 -> info[0] ?: ""
                                        2 -> info[1] ?: ""
                                        3 -> info[2] ?: ""
                                        else -> null
                                    }
                                    val quality = when (info.size) {
                                        3 -> info[1] ?: ""
                                        else -> null
                                    }
                                    val rating = when (info.size) {
                                        2 -> info[0].toDoubleOrNull()
                                        3 -> info[0].toDoubleOrNull()
                                        else -> null
                                    }
                                }
                            }

                        Movie(
                            id = showId,
                            title = showTitle,
                            released = info.released,
                            quality = info.quality ?: "",
                            rating = info.rating,
                            poster = showPoster,
                        )
                    }
                    false -> {
                        val info = it
                            .select("div.film-detail > div.fd-infor > span")
                            .toList()
                            .map { element -> element.text() }
                            .let { info ->
                                object {
                                    val quality = when (info.size) {
                                        3 -> info[1] ?: ""
                                        else -> null
                                    }
                                    val rating = when (info.size) {
                                        2 -> info[0].toDoubleOrNull()
                                        3 -> info[0].toDoubleOrNull()
                                        else -> null
                                    }
                                    val lastEpisode = when (info.size) {
                                        1 -> info[0] ?: ""
                                        2 -> info[1] ?: ""
                                        3 -> info[2] ?: ""
                                        else -> null
                                    }
                                }
                            }

                        TvShow(
                            id = showId,
                            title = showTitle,
                            quality = info.quality ?: "",
                            rating = info.rating,
                            poster = showPoster,

                            seasons = info.lastEpisode?.let { lastEpisode ->
                                listOf(
                                    Season(
                                        id = "",
                                        number = lastEpisode
                                            .substringAfter("S")
                                            .substringBefore(":")
                                            .toIntOrNull() ?: 0,

                                        episodes = listOf(
                                            Episode(
                                                id = "",
                                                number = lastEpisode
                                                    .substringAfter(":")
                                                    .substringAfter("E")
                                                    .toIntOrNull() ?: 0,
                                            )
                                        )
                                    )
                                )
                            } ?: listOf(),
                        )
                    }
                }
            },
        )

        return people
    }


    interface SflixService {

        companion object {
            fun build(): SflixService {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://sflix.to/")
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(
                        GsonConverterFactory.create(
                            GsonBuilder()
                                .registerTypeAdapter(
                                    SourcesResponse::class.java,
                                    SourcesResponse.Deserializer(),
                                )
                                .create()
                        )
                    )
                    .build()

                return retrofit.create(SflixService::class.java)
            }
        }


        @GET("home")
        suspend fun getHome(): Document

        @GET("search/{query}")
        suspend fun search(@Path("query") query: String): Document

        @GET("movie")
        suspend fun getMovies(): Document

        @GET("tv-show")
        suspend fun getTvShows(): Document


        @GET("movie/free-{id}")
        suspend fun getMovieById(@Path("id") id: String): Document

        @GET("ajax/movie/episodes/{id}")
        suspend fun getMovieServersById(@Path("id") movieId: String): Document


        @GET("tv/free-{id}")
        suspend fun getTvShowById(@Path("id") id: String): Document

        @GET("ajax/v2/tv/seasons/{id}")
        suspend fun getTvShowSeasonsById(@Path("id") tvShowId: String): Document

        @GET("ajax/v2/season/episodes/{id}")
        suspend fun getSeasonEpisodesById(@Path("id") seasonId: String): Document

        @GET("ajax/v2/episode/servers/{id}")
        suspend fun getEpisodeServersById(@Path("id") episodeId: String): Document


        @GET("cast/{slug}")
        suspend fun getPeopleBySlug(@Path("slug") slug: String): Document


        @GET("ajax/get_link/{id}")
        suspend fun getLink(@Path("id") id: String): Link

        @GET
        @Headers(
            "Accept: */*",
            "Accept-Language: en-US,en;q=0.5",
            "Connection: keep-alive",
            "referer: https://sflix.to",
            "TE: trailers",
            "X-Requested-With: XMLHttpRequest",
        )
        suspend fun getSources(
            @Url url: String,
            @Query("id") id: String,
        ): SourcesResponse

        @GET("https://raw.githubusercontent.com/consumet/rapidclown/dokicloud/key.txt")
        suspend fun getSourceEncryptedKey(): Document


        data class Link(
            val type: String = "",
            val link: String = "",
            val sources: List<String> = listOf(),
            val tracks: List<String> = listOf(),
            val title: String = "",
        )

        sealed class SourcesResponse {
            class Deserializer : JsonDeserializer<SourcesResponse> {
                override fun deserialize(
                    json: JsonElement?,
                    typeOfT: Type?,
                    context: JsonDeserializationContext?
                ): SourcesResponse {
                    val jsonObject = json?.asJsonObject ?: JsonObject()

                    return when (jsonObject.get("sources")?.isJsonArray ?: false) {
                        true -> Gson().fromJson(json, Sources::class.java)
                        false -> Gson().fromJson(json, Sources.Encrypted::class.java)
                    }
                }
            }
        }

        data class Sources(
            val sources: List<Source> = listOf(),
            val sourcesBackup: List<Source> = listOf(),
            val tracks: List<Track> = listOf(),
            val server: Int? = null,
        ) : SourcesResponse() {

            data class Encrypted(
                val sources: String,
                val sourcesBackup: String? = null,
                val tracks: List<Track> = listOf(),
                val server: Int? = null,
            ) : SourcesResponse() {
                fun decrypt(secret: String): Sources {
                    fun decryptSourceUrl(decryptionKey: ByteArray, sourceUrl: String): String {
                        val cipherData = Base64.decode(sourceUrl, Base64.DEFAULT)
                        val encrypted = cipherData.copyOfRange(16, cipherData.size)
                        val aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding")!!

                        aesCBC.init(
                            Cipher.DECRYPT_MODE,
                            SecretKeySpec(
                                decryptionKey.copyOfRange(0, 32),
                                "AES"
                            ),
                            IvParameterSpec(decryptionKey.copyOfRange(32, decryptionKey.size))
                        )
                        val decryptedData = aesCBC.doFinal(encrypted)
                        return String(decryptedData, StandardCharsets.UTF_8)
                    }

                    fun generateKey(salt: ByteArray, secret: ByteArray): ByteArray {
                        fun md5(input: ByteArray) = MessageDigest.getInstance("MD5").digest(input)

                        var key = md5(secret + salt)
                        var currentKey = key
                        while (currentKey.size < 48) {
                            key = md5(key + secret + salt)
                            currentKey += key
                        }
                        return currentKey
                    }

                    val decrypted = decryptSourceUrl(
                        generateKey(
                            Base64.decode(sources, Base64.DEFAULT).copyOfRange(8, 16),
                            secret.toByteArray(),
                        ),
                        sources,
                    )

                    return Sources(
                        sources = Gson().fromJson(decrypted, Array<Source>::class.java).toList(),
                        tracks = tracks
                    )
                }

                data class SecretKey(val key: String = "")
            }

            data class Source(
                val file: String = "",
                val type: String = "",
            )

            data class Track(
                val file: String = "",
                val label: String = "",
                val kind: String = "",
                val default: Boolean = false,
            )
        }
    }
}