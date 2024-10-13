package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
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
        val document = try {
            service.getMovies(page)
        } catch (e: HttpException) {
            if (e.code() == 404) return emptyList()
            else throw e
        }

        val movies = document.select("div#archive-content article.item").map {
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
        }

        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = try {
            service.getTvShows(page)
        } catch (e: HttpException) {
            if (e.code() == 404) return emptyList()
            else throw e
        }


        val tvShows = document.select("div#archive-content article.item").map {
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
        }

        return tvShows
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getMovie(id)

        val movie = Movie(
            id = id,
            title = document.selectFirst("h1")
                ?.text()
                ?: "",
            overview = document.selectFirst("div#info div.wp-content p")
                ?.text(),
            released = document.selectFirst("div.extra span.date")
                ?.text(),
            runtime = document.selectFirst("div.extra span.runtime")
                ?.text()?.substringBefore(" Min")?.toIntOrNull(),
            rating = document.selectFirst("span.dt_rating_vgs")
                ?.text()?.toDoubleOrNull(),
            poster = document.selectFirst("div.poster img")
                ?.attr("src"),
            banner = document.selectFirst("img.cover")
                ?.attr("src"),

            genres = document.select("div.sgeneros a").map {
                Genre(
                    id = it.attr("href").substringBeforeLast("/").substringAfterLast("/"),
                    name = it.text(),
                )
            },
            directors = document.select("div#cast div.person[itemprop=director]").map {
                People(
                    id = it.selectFirst("a")
                        ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                        ?: "",
                    name = it.selectFirst("div.name")
                        ?.text()
                        ?: "",
                    image = it.selectFirst("img")
                        ?.attr("src"),
                )
            },
            cast = document.select("div#cast div.person[itemprop=actor]").map {
                People(
                    id = it.selectFirst("a")
                        ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                        ?: "",
                    name = it.selectFirst("div.name")
                        ?.text()
                        ?: "",
                    image = it.selectFirst("img")
                        ?.attr("src"),
                )
            },
            recommendations = document.select("div.srelacionados article").mapNotNull {
                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                    ?: ""
                val showTitle = it.selectFirst("img")
                    ?.attr("alt")
                    ?: ""
                val showPoster = it.selectFirst("img")
                    ?.attr("src")

                val href = it.selectFirst("a")
                    ?.attr("href")
                    ?: ""
                if (href.contains("/movies/")) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                } else if (href.contains("/tvshows/")) {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                } else {
                    null
                }
            },
        )

        return movie
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTvShow(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("h1")
                ?.text()
                ?: "",
            overview = document.selectFirst("div#info div.wp-content p")
                ?.text(),
            rating = document.selectFirst("span.dt_rating_vgs")
                ?.text()?.toDoubleOrNull(),
            poster = document.selectFirst("div.poster img")
                ?.attr("src"),

            seasons = document.select("div#seasons div.se-c").mapIndexed { index, it ->
                Season(
                    id = "$id/$index",
                    number = it.selectFirst("span.se-t")
                        ?.text()?.toIntOrNull()
                        ?: 0,
                    title = it.selectFirst("span.title")
                        ?.ownText(),
                )
            },
            genres = document.select("div.sgeneros a").map {
                Genre(
                    id = it.attr("href").substringBeforeLast("/").substringAfterLast("/"),
                    name = it.text(),
                )
            },
            recommendations = document.select("div.srelacionados article").mapNotNull {
                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                    ?: ""
                val showTitle = it.selectFirst("img")
                    ?.attr("alt")
                    ?: ""
                val showPoster = it.selectFirst("img")
                    ?.attr("src")

                val href = it.selectFirst("a")
                    ?.attr("href")
                    ?: ""
                if (href.contains("/movies/")) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                } else if (href.contains("/tvshows/")) {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                } else {
                    null
                }
            },
        )

        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val (tvShowId, seasonIndex) = seasonId.split("/")

        val document = service.getTvShow(tvShowId)

        val episodes = document.select("div#seasons div.se-c").getOrNull(seasonIndex.toInt())
            ?.select("ul.episodios li")?.map {
                Episode(
                    id = it.selectFirst("a")
                        ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                        ?: "",
                    number = it.selectFirst("div.numerando")
                        ?.text()?.substringAfter("- ")?.toIntOrNull()
                        ?: 0,
                    title = it.selectFirst("div.episodiotitle > a")
                        ?.text(),
                    released = it.selectFirst("span.date")
                        ?.text(),
                    poster = it.selectFirst("img")
                        ?.attr("src"),
                )
            } ?: emptyList()

        return episodes
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = try {
            service.getGenre(id, page)
        } catch (e: HttpException) {
            if (e.code() == 404) return Genre(id, "")
            else throw e
        }

        val genre = Genre(
            id = id,
            name = document.selectFirst("h1.heading-archive")
                ?.text()
                ?: "",

            shows = document.select("div.items article.item").mapNotNull {
                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                    ?: ""
                val showTitle = it.selectFirst("div.data h3")
                    ?.text()
                    ?: ""
                val showReleased = it.selectFirst("div.data span")
                    ?.text()
                val showQuality = it.selectFirst("span.quality")
                    ?.text()
                val showRating = it.selectFirst("div.rating")
                    ?.text()?.toDoubleOrNull()
                val showPoster = it.selectFirst("img")
                    ?.attr("src")

                val href = it.selectFirst("a")
                    ?.attr("href")
                    ?: ""
                if (href.contains("/movies/")) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        released = showReleased,
                        quality = showQuality,
                        rating = showRating,
                        poster = showPoster,
                    )
                } else if (href.contains("/tvshows/")) {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        released = showReleased,
                        quality = showQuality,
                        rating = showRating,
                        poster = showPoster,
                    )
                } else {
                    null
                }
            },
        )

        return genre
    }

    override suspend fun getPeople(id: String, page: Int): People {
        if (page > 1) return People(id, name = "")

        val document = service.getCast(id)

        val people = People(
            id = id,
            name = document.selectFirst("h1.heading-archive")
                ?.text()
                ?: "",

            filmography = document.select("div.items article.item").mapNotNull {
                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                    ?: ""
                val showTitle = it.selectFirst("div.data h3")
                    ?.text()
                    ?: ""
                val showReleased = it.selectFirst("div.data span")
                    ?.text()
                val showQuality = it.selectFirst("span.quality")
                    ?.text()
                val showRating = it.selectFirst("div.rating")
                    ?.text()?.toDoubleOrNull()
                val showPoster = it.selectFirst("img")
                    ?.attr("src")

                val href = it.selectFirst("a")
                    ?.attr("href")
                    ?: ""
                if (href.contains("/movies/")) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        released = showReleased,
                        quality = showQuality,
                        rating = showRating,
                        poster = showPoster,
                    )
                } else if (href.contains("/tvshows/")) {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        released = showReleased,
                        quality = showQuality,
                        rating = showRating,
                        poster = showPoster,
                    )
                } else {
                    null
                }
            }
        )

        return people
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val document = when (videoType) {
            is Video.Type.Episode -> service.getEpisode(id)
            is Video.Type.Movie -> service.getMovie(id)
        }

        val servers = document.select("ul#playeroptionsul > li").mapNotNull {
            if (it.attr("data-nume") == "trailer")
                return@mapNotNull null

            Video.Server(
                id = "${it.attr("data-post")}/${it.attr("data-nume")}",
                name = it.selectFirst("span.title")
                    ?.text()
                    ?: "",
            )
        }

        return servers
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

        @GET("movies/page/{page}")
        suspend fun getMovies(@Path("page") page: Int): Document

        @GET("tvshows/page/{page}")
        suspend fun getTvShows(@Path("page") page: Int): Document

        @GET("movies/{id}")
        suspend fun getMovie(@Path("id") id: String): Document

        @GET("tvshows/{id}")
        suspend fun getTvShow(@Path("id") id: String): Document

        @GET("episodes/{id}")
        suspend fun getEpisode(@Path("id") id: String): Document

        @GET("genre/{id}/page/{page}")
        suspend fun getGenre(
            @Path("id") id: String,
            @Path("page") page: Int,
        ): Document

        @GET("cast/{id}")
        suspend fun getCast(@Path("id") id: String): Document
    }
}