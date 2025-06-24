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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Document
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

object OtakufrProvider : Provider {

    private const val URL = "https://otakufr.cc/"
    override val baseUrl = URL
    override val name = "Otakufr"
    override val logo = "https://i.ibb.co/GndKBbF/otakufr-logo.webp"
    override val language = "fr"

    private val service = Service.build()

    override suspend fun getHome(): List<Category> {
        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                "En cours",
                service.getEnCours().select("article.card").map {
                    TvShow(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                            ?: "",
                        title = it.selectFirst("a.episode-name.h4")
                            ?.text()
                            ?: "",
                        overview = it.selectFirst("div.except p")
                            ?.ownText(),
                        poster = it.selectFirst("img")
                            ?.attr("src")
                    )
                }
            )
        )

        categories.add(
            Category(
                "Terminé",
                service.getTermines().select("article.card").map {
                    TvShow(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                            ?: "",
                        title = it.selectFirst("a.episode-name.h4")
                            ?.text()
                            ?: "",
                        overview = it.selectFirst("div.except p")
                            ?.ownText(),
                        poster = it.selectFirst("img")
                            ?.attr("src")
                    )
                }
            )
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getHome()

            val genres = document.select("div.dropdown-menu a").map {
                Genre(
                    id = it.attr("href").substringBeforeLast("/").substringAfterLast("/"),
                    name = it.text(),
                )
            }

            return genres
        }

        if (page > 1) return emptyList()

        val document = service.search(query)

        val results = document.select("article.card").map {
            TvShow(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                    ?: "",
                title = it.selectFirst("a.episode-name.h4")
                    ?.text()
                    ?: "",
                overview = it.selectFirst("div.except p")
                    ?.ownText(),
                poster = it.selectFirst("img")
                    ?.attr("src")
            )
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        throw Exception("Not yet implemented")
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = service.getAnimes(page)

        val tvShows = document.select("article.card").map {
            TvShow(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                    ?: "",
                title = it.selectFirst("a.episode-name.h4")
                    ?.text()
                    ?: "",
                overview = it.selectFirst("div.except p")
                    ?.ownText(),
                poster = it.selectFirst("img")
                    ?.attr("src")
            )
        }

        return tvShows
    }

    override suspend fun getMovie(id: String): Movie {
        throw Exception("Not yet implemented")
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getAnime(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("div.title.h1")
                ?.text()
                ?: "",
            overview = document.select("div.synop p")
                .joinToString("\n") { it.ownText() },
            released = document.select("div.synop ul li")
                .find { it.selectFirst("strong")?.text()?.contains("Sortie initiale") == true }
                ?.ownText(),
            runtime = document.select("div.synop ul li")
                .find { it.selectFirst("strong")?.text()?.contains("Durée") == true }
                ?.ownText()?.substringBefore(" min")?.toIntOrNull(),
            poster = document.selectFirst("div.card-body img")
                ?.attr("src"),

            seasons = listOf(
                Season(
                    id = id,
                    title = "Épisodes",
                ),
            ),
            genres = document.select("div.synop ul li")
                .find { it.selectFirst("strong")?.text()?.contains("Genre") == true }
                ?.select("a")?.map {
                    Genre(
                        id = it.attr("href").substringBeforeLast("/").substringAfterLast("/"),
                        name = it.text(),
                    )
                }
                ?: emptyList(),
        )

        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val document = service.getAnime(seasonId)

        val episodes = document.select("div.list-episodes a").reversed().mapIndexed { index, it ->
            Episode(
                id = it.attr("href").substringBeforeLast("/").substringAfterLast("/"),
                number = index + 1,
                title = it.ownText(),
                released = it.selectFirst("span")
                    ?.text(),
            )
        }

        return episodes
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = try {
            service.getGenre(id, page)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> return Genre(id, "")
                else -> throw e
            }
        }

        val genre = Genre(
            id = id,
            name = document.selectFirst("div.title.h1")
                ?.text()
                ?: "",
            shows = document.select("article.card").map {
                TvShow(
                    id = it.selectFirst("a")
                        ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                        ?: "",
                    title = it.selectFirst("a.episode-name.h4")
                        ?.text()
                        ?: "",
                    overview = it.selectFirst("div.except p")
                        ?.ownText(),
                    poster = it.selectFirst("img")
                        ?.attr("src")
                )
            }
        )

        return genre
    }

    override suspend fun getPeople(id: String, page: Int): People {
        throw Exception("Not yet implemented")
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val document = service.getEpisode(id)

        val servers = document.select("div#nav-tab a").map {
            Video.Server(
                id = it.id(),
                name = it.text(),
                src = document.selectFirst("div#${it.attr("aria-controls")} iframe")
                    ?.attr("src")
                    ?: "",
            )
        }

        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val link = if (server.src.contains("parisanime.com")) {
            val doc = service.getParisanime(server.src)

            doc.selectFirst("div[data-url]")
                ?.attr("data-url")
                ?.let {
                    when {
                        it.startsWith("//") -> "https:$it"
                        else -> it
                    }
                }
                ?: throw Exception("Can't retrieve data-url")
        } else {
            server.src
        }

        val video = Extractor.extract(link)

        return video
    }


    private interface Service {

        companion object {
            fun build(): Service {
                val client = OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val response = chain.proceed(chain.request())

                        if (!response.isSuccessful) {
                            val body = response.body?.string()

                            if (!body.isNullOrEmpty()) {
                                return@addInterceptor response.newBuilder()
                                    .code(200)
                                    .body(body.toResponseBody(response.body?.contentType()))
                                    .build()
                            }
                        }

                        response
                    }
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(Service::class.java)
            }
        }


        @GET(".")
        suspend fun getHome(): Document

        @GET("en-cours")
        suspend fun getEnCours(): Document

        @GET("termine")
        suspend fun getTermines(): Document

        @GET("toute-la-liste-affiches")
        suspend fun search(@Query("q") q: String): Document

        @GET("en-cours/page/{page}")
        suspend fun getAnimes(@Path("page") page: Int): Document

        @GET("anime/{id}")
        suspend fun getAnime(@Path("id") id: String): Document

        @GET("genre/{slug}/page/{page}")
        suspend fun getGenre(
            @Path("slug") slug: String,
            @Path("page") page: Int,
        ): Document

        @GET("episode/{id}")
        suspend fun getEpisode(@Path("id") id: String): Document

        @GET
        @Headers(
            "X-Requested-With: XMLHttpRequest",
        )
        suspend fun getParisanime(@Url url: String): Document
    }
}