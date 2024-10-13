package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

object UnJourUnFilmProvider : Provider {

    override val name = "1JOUR1FILM"
    override val logo =
        "https://1jour1film.pics/wp-content/uploads/2022/12/screenshot-looka.com-2021.10.19-12_28_21.png"
    override val language = "fr"
    private const val URL = "https://1jour1film.pics/"

    private val service = Service.build()

    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = Category.FEATURED,
                list = document.select("div#slider-movies-tvshows article.item").mapNotNull {
                    val id = it.selectFirst("a")
                        ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                        ?: ""
                    val title = it.selectFirst("h3.title")
                        ?.text()
                        ?: ""
                    val released = it.selectFirst("div.data span")
                        ?.text()
                    val banner = it.selectFirst("img")
                        ?.attr("src")

                    val href = it.selectFirst("a")
                        ?.attr("href")
                        ?: ""
                    if (href.contains("/movies/")) {
                        Movie(
                            id = id,
                            title = title,
                            released = released,
                            banner = banner,
                        )
                    } else if (href.contains("/tvshows/")) {
                        TvShow(
                            id = id,
                            title = title,
                            released = released,
                            banner = banner,
                        )
                    } else {
                        null
                    }
                }
            )
        )

        categories.add(
            Category(
                name = "Le TOP Actuel",
                list = document.select("div.items")[0].select("article.item").mapNotNull {
                    val id = it.selectFirst("a")
                        ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                        ?: ""
                    val title = it.selectFirst("div.data h3")
                        ?.text()
                        ?: ""
                    val released = it.selectFirst("div.data span")
                        ?.text()
                    val rating = it.selectFirst("div.rating")
                        ?.text()?.toDoubleOrNull()
                    val poster = it.selectFirst("img")
                        ?.attr("data-src")

                    val href = it.selectFirst("a")
                        ?.attr("href")
                        ?: ""
                    if (href.contains("/movies/")) {
                        Movie(
                            id = id,
                            title = title,
                            released = released,
                            rating = rating,
                            poster = poster,
                        )
                    } else if (href.contains("/tvshows/")) {
                        TvShow(
                            id = id,
                            title = title,
                            released = released,
                            rating = rating,
                            poster = poster,
                        )
                    } else {
                        null
                    }
                },
            )
        )

        categories.add(
            Category(
                name = "Derniers Films",
                list = document.select("div.items")[1].select("article.item").mapNotNull {
                    Movie(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                            ?: "",
                        title = it.selectFirst("div.data h3")
                            ?.text()
                            ?: "",
                        released = it.selectFirst("div.data span")
                            ?.text(),
                        quality = it.selectFirst("span.quality")
                            ?.text(),
                        rating = it.selectFirst("div.rating")
                            ?.text()?.toDoubleOrNull(),
                        poster = it.selectFirst("img")
                            ?.attr("src"),
                    )
                },
            )
        )

        categories.add(
            Category(
                name = "Dernières Séries",
                list = document.select("div.items")[2].select("article.item").mapNotNull {
                    TvShow(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                            ?: "",
                        title = it.selectFirst("div.data h3")
                            ?.text()
                            ?: "",
                        released = it.selectFirst("div.data span")
                            ?.text(),
                        rating = it.selectFirst("div.rating")
                            ?.text()?.toDoubleOrNull(),
                        poster = it.selectFirst("img")
                            ?.attr("src"),
                    )
                },
            )
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getHome()

            val genres = document.select("li.menu-item-object-genres").map {
                Genre(
                    id = it.selectFirst("a")
                        ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                        ?: "",
                    name = it.text(),
                )
            }.distinctBy { it.id }

            return genres
        }

        val document = try {
            service.search(page, query)
        } catch (e: HttpException) {
            if (e.code() == 404) return emptyList()
            else throw e
        }

        val results = document.select("div.result-item").mapNotNull {
            val id = it.selectFirst("a")
                ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                ?: ""
            val title = it.selectFirst("div.title")
                ?.text()
                ?: ""
            val overview = it.selectFirst("div.contenido")
                ?.text()?.substringAfter(": ")
            val released = it.selectFirst("span.year")
                ?.text()
            val rating = it.selectFirst("div.rating")
                ?.text()?.substringAfter("IMDb ")?.toDoubleOrNull()
            val poster = it.selectFirst("img")
                ?.attr("src")

            val href = it.selectFirst("a")
                ?.attr("href")
                ?: ""
            if (href.contains("/movies/")) {
                Movie(
                    id = id,
                    title = title,
                    overview = overview,
                    released = released,
                    rating = rating,
                    poster = poster,
                )
            } else if (href.contains("/tvshows/")) {
                TvShow(
                    id = id,
                    title = title,
                    overview = overview,
                    released = released,
                    rating = rating,
                    poster = poster,
                )
            } else {
                null
            }
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        TODO("Not yet implemented")
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        TODO("Not yet implemented")
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

    override suspend fun getGenre(id: String, page: Int): Genre {
        TODO("Not yet implemented")
    }

    override suspend fun getPeople(id: String, page: Int): People {
        TODO("Not yet implemented")
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        TODO("Not yet implemented")
    }

    override suspend fun getVideo(server: Video.Server): Video {
        TODO("Not yet implemented")
    }


    private interface Service {

        companion object {
            fun build(): Service {
                val client = OkHttpClient.Builder()
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET(".")
        suspend fun getHome(): Document

        @GET("page/{page}")
        suspend fun search(
            @Path("page") page: Int,
            @Query("s") s: String,
        ): Document
    }
}