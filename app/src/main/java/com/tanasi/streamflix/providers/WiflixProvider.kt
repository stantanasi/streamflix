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
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

object WiflixProvider : Provider {

    private var URL = "https://wiflix-hd.vip/"
    override val baseUrl = URL
    override val name = "Wiflix"
    override val logo get() = "$URL/templates/wiflixnew/images/logo.png"
    override val language = "fr"

    private lateinit var service: Service
    private var serviceInitialized = false
    private val initializationMutex = Mutex()

    // Flag to track if more search results are available. Set to false when API returns fewer items than requested.
    // This prevents querying non-existent pages that could return random/incorrect results.
    private var hasMore = true

    override suspend fun getHome(): List<Category> {
        initializeService()
        val document = service.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = "TOP Séries 2024",
                list = document.select("div.block-main").getOrNull(0)?.select("div.mov")?.map {
                    TvShow(
                        id = it.selectFirst("a.mov-t")
                            ?.attr("href")?.substringAfterLast("/")
                            ?: "",
                        title = listOfNotNull(
                            it.selectFirst("a.mov-t")?.text(),
                            it.selectFirst("span.block-sai")?.text(),
                        ).joinToString(" - "),
                        poster = it.selectFirst("img")
                            ?.attr("src")?.let { src -> URL + src },
                    )
                } ?: emptyList(),
            )
        )

        categories.add(
            Category(
                name = "TOP Films 2024",
                list = document.select("div.block-main").getOrNull(1)?.select("div.mov")?.map {
                    Movie(
                        id = it.selectFirst("a.mov-t")
                            ?.attr("href")?.substringAfterLast("/")
                            ?: "",
                        title = it.selectFirst("a.mov-t")
                            ?.text()
                            ?: "",
                        poster = it.selectFirst("img")
                            ?.attr("src")?.let { src -> URL + src },
                    )
                } ?: emptyList(),
            )
        )
        categories.add(
            Category(
                name = "Films Anciens",
                list = document.select("div.block-main").getOrNull(2)?.select("div.mov")?.map {
                    Movie(
                        id = it.selectFirst("a.mov-t")
                            ?.attr("href")?.substringAfterLast("/")
                            ?: "",
                        title = it.selectFirst("a.mov-t")
                            ?.text()
                            ?: "",
                        poster = it.selectFirst("img")
                            ?.attr("src")?.let { src -> URL + src },
                    )
                } ?: emptyList(),
            )
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        initializeService()

        if (query.isEmpty()) {
            val document = service.getHome()

            val genres = document.select("div.side-b").getOrNull(1)?.select("ul li")?.map {
                Genre(
                    id = it.selectFirst("a")
                        ?.attr("href")?.substringBeforeLast("/")?.substringAfterLast("/")
                        ?: "",
                    name = it.selectFirst("a")
                        ?.text()
                        ?: "",
                )
            } ?: emptyList()

            return genres
        }

        if (page > 1 && !hasMore) return emptyList()

        val document = service.search(
            story = query,
            searchStart = page,
            resultFrom = 1 + 20 * (page - 1)
        )

        document.selectFirst("div.berrors")?.text()?.let { resultText ->
            val totalResults = resultText.substringAfter("trouvé ")
                .substringBefore(" réponses").toIntOrNull() ?: 0
            val currentRange = resultText.substringAfter("Résultats de la requête ")
                .substringBefore(")").split(" - ")
            val receivedItems = currentRange.getOrNull(1)?.toIntOrNull() ?: 0

            hasMore = receivedItems < totalResults
        }

        val results = document.select("div.mov").mapNotNull {
            val showId = it.selectFirst("a.mov-t")
                ?.attr("href")?.substringAfterLast("/")
                ?: ""
            val showPoster = it.selectFirst("img")
                ?.attr("src")?.let { src -> URL + src }

            val href = it.selectFirst("a.mov-t")
                ?.attr("href")
                ?: ""
            if (href.contains("film-en-streaming/") || href.contains("film-ancien/")) {
                Movie(
                    id = showId,
                    title = it.selectFirst("a.mov-t")
                        ?.text()
                        ?: "",
                    poster = showPoster,
                )
            } else if (href.contains("serie-en-streaming/") || href.contains("vf/")) {
                TvShow(
                    id = showId,
                    title = listOfNotNull(
                        it.selectFirst("a.mov-t")?.text(),
                        it.selectFirst("span.block-sai")?.text(),
                    ).joinToString(" - "),
                    poster = showPoster,
                )
            } else {
                null
            }
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        initializeService()
        val document = service.getMovies(page)

        val movies = document.select("div.mov").map {
            Movie(
                id = it.selectFirst("a.mov-t")
                    ?.attr("href")?.substringAfterLast("/")
                    ?: "",
                title = it.selectFirst("a.mov-t")
                    ?.text()
                    ?: "",
                poster = it.selectFirst("img")
                    ?.attr("src")?.let { src -> URL + src },
            )
        }

        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        initializeService()
        val document = service.getTvShows(page)

        val tvShows = document.select("div.mov").map {
            TvShow(
                id = it.selectFirst("a.mov-t")
                    ?.attr("href")?.substringAfterLast("/")
                    ?: "",
                title = listOfNotNull(
                    it.selectFirst("a.mov-t")?.text(),
                    it.selectFirst("span.block-sai")?.text(),
                ).joinToString(" - "),
                poster = it.selectFirst("img")
                    ?.attr("src")?.let { src -> URL + src },
            )
        }

        return tvShows
    }

    override suspend fun getMovie(id: String): Movie {
        initializeService()
        val document = service.getMovie(id)

        val movie = Movie(
            id = id,
            title = document.selectFirst("header.full-title h1")
                ?.text()
                ?: "",
            overview = document.selectFirst("div.screenshots-full")
                ?.ownText(),
            released = document.select("ul.mov-list li")
                .find {
                    it.selectFirst("div.mov-label")?.text()?.contains("Date de sortie") == true
                }
                ?.selectFirst("div.mov-desc")
                ?.text()?.trim(),
            runtime = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text()?.contains("Durée") == true }
                ?.selectFirst("div.mov-desc")
                ?.text()?.let {
                    val hours = it.substringBefore("h").toIntOrNull() ?: 0
                    val minutes =
                        it.substringBeforeLast("min").substringAfterLast("h").trim().toIntOrNull() ?: 0
                    hours * 60 + minutes
                }?.takeIf { it != 0 },
            quality = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text()?.contains("Qualité") == true }
                ?.selectFirst("div.mov-desc")
                ?.text(),
            poster = document.selectFirst("img#posterimg")
                ?.attr("src")?.let { URL + it },

            genres = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text()?.contains("GENRE") == true }
                ?.select("div.mov-desc a")?.mapNotNull {
                    if (it.text() == "Film") return@mapNotNull null

                    Genre(
                        id = it.attr("href").substringBeforeLast("/").substringAfterLast("/"),
                        name = it.text(),
                    )
                }
                ?: emptyList(),
            cast = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text()?.contains("ACTEURS") == true }
                ?.select("div.mov-desc a")?.map {
                    People(
                        id = it.attr("href").substringBeforeLast("/").substringAfterLast("/"),
                        name = it.text(),
                    )
                }
                ?: emptyList(),
            recommendations = document.select("div.related div.item").mapNotNull {
                if (it.hasClass("cloned")) return@mapNotNull null

                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/")
                    ?: ""
                val showTitle = it.selectFirst("span.title1")
                    ?.text()
                    ?: ""
                val showPoster = it.selectFirst("img")
                    ?.attr("src")?.let { src -> URL + src }

                val href = it.selectFirst("a")
                    ?.attr("href")
                    ?: ""
                if (href.contains("film-en-streaming/") || href.contains("film-ancien/")) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                } else if (href.contains("serie-en-streaming/") || href.contains("vf/")) {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                } else {
                    null
                }
            }
        )

        return movie
    }

    override suspend fun getTvShow(id: String): TvShow {
        initializeService()
        val document = service.getTvShow(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("header.full-title h1")
                ?.text()
                ?: "",
            overview = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text()?.contains("Synopsis") == true }
                ?.selectFirst("div.mov-desc")
                ?.text(),
            released = document.select("ul.mov-list li")
                .find {
                    it.selectFirst("div.mov-label")?.text()?.contains("Date de sortie") == true
                }
                ?.selectFirst("div.mov-desc")
                ?.text()?.trim(),
            runtime = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text()?.contains("Durée") == true }
                ?.selectFirst("div.mov-desc")
                ?.text()?.let {
                    val hours = it.substringBefore("h").toIntOrNull() ?: 0
                    val minutes =
                        it.substringBeforeLast(" mn").substringAfterLast(" ").toIntOrNull() ?: 0
                    hours * 60 + minutes
                }?.takeIf { it != 0 },
            poster = document.selectFirst("img#posterimg")
                ?.attr("src")?.let { URL + it },

            seasons = listOfNotNull(
                Season(
                    id = "$id/blocvostfr",
                    title = "Épisodes - VOSTFR",
                ).takeIf { document.select("div.blocvostfr ul.eplist li").size > 0 },
                Season(
                    id = "$id/blocfr",
                    title = "Épisodes - VF",
                ).takeIf { document.select("div.blocfr ul.eplist li").size > 0 },
            ),
            cast = document.select("ul.mov-list li")
                .find { it.selectFirst("div.mov-label")?.text()?.contains("ACTEURS") == true }
                ?.select("div.mov-desc a")?.map {
                    People(
                        id = it.attr("href").substringBeforeLast("/").substringAfterLast("/"),
                        name = it.text(),
                    )
                }
                ?: emptyList(),
            recommendations = document.select("div.related div.item").mapNotNull {
                if (it.hasClass("cloned")) return@mapNotNull null

                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("/")
                    ?: ""
                val showTitle = it.selectFirst("span.title1")
                    ?.text()
                    ?: ""
                val showPoster = it.selectFirst("img")
                    ?.attr("src")?.let { src -> URL + src }

                val href = it.selectFirst("a")
                    ?.attr("href")
                    ?: ""
                if (href.contains("film-en-streaming/") || href.contains("film-ancien/")) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                } else if (href.contains("serie-en-streaming/") || href.contains("vf/")) {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        poster = showPoster,
                    )
                } else {
                    null
                }
            }
        )

        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        initializeService()
        val (tvShowId, className) = seasonId.split("/")

        val document = service.getTvShow(tvShowId)

        val episodes = document.select("div.$className ul.eplist li").map {
            Episode(
                id = "$tvShowId/${it.attr("rel")}",
                number = it.text().substringAfter("Episode ").toIntOrNull() ?: 0,
                title = it.text(),
            )
        }

        return episodes
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        initializeService()
        val document = service.getGenre(id, page)

        val genre = Genre(
            id = id,
            name = "",

            shows = document.select("div.mov").map {
                Movie(
                    id = it.selectFirst("a.mov-t")
                        ?.attr("href")?.substringAfterLast("/")
                        ?: "",
                    title = it.selectFirst("a.mov-t")
                        ?.text()
                        ?: "",
                    poster = it.selectFirst("img")
                        ?.attr("src")?.let { src -> URL + src },
                )
            },
        )

        return genre
    }

    override suspend fun getPeople(id: String, page: Int): People {
        initializeService()
        val document = try {
            service.getPeople(id, page)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> return People(id, "")
                else -> throw e
            }
        }


        val people = People(
            id = id,
            name = "",

            filmography = document.select("div.mov").mapNotNull {
                val showId = it.selectFirst("a.mov-t")
                    ?.attr("href")?.substringAfterLast("/")
                    ?: ""
                val showPoster = it.selectFirst("img")
                    ?.attr("src")?.let { src -> URL + src }

                val href = it.selectFirst("a.mov-t")
                    ?.attr("href")
                    ?: ""
                if (href.contains("film-en-streaming/") || href.contains("film-ancien/")) {
                    Movie(
                        id = showId,
                        title = it.selectFirst("a.mov-t")
                            ?.text()
                            ?: "",
                        poster = showPoster,
                    )
                } else if (href.contains("serie-en-streaming/") || href.contains("vf/")) {
                    TvShow(
                        id = showId,
                        title = listOfNotNull(
                            it.selectFirst("a.mov-t")?.text(),
                            it.selectFirst("span.block-sai")?.text(),
                        ).joinToString(" - "),
                        poster = showPoster,
                    )
                } else {
                    null
                }
            },
        )

        return people
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        initializeService()
        val servers = when (videoType) {
            is Video.Type.Episode -> {
                val (tvShowId, rel) = id.split("/")

                val document = service.getTvShow(tvShowId)

                document.select("div.$rel a").mapIndexed { index, it ->
                    Video.Server(
                        id = it.selectFirst("span")
                            ?.text()
                            ?: index.toString(),
                        name = it.selectFirst("span")
                            ?.text()
                            ?: "",
                        src = it.attr("onclick")
                            .substringAfter("loadVideo('").substringBeforeLast("')"),
                    )
                }
            }

            is Video.Type.Movie -> {
                val document = service.getMovie(id)

                document.select("div.tabs-sel a").mapIndexed { index, it ->
                    Video.Server(
                        id = it.selectFirst("span")
                            ?.text()
                            ?: index.toString(),
                        name = it.selectFirst("span")
                            ?.text()
                            ?: "",
                        src = it.attr("onclick")
                            .substringAfter("loadVideo('").substringBeforeLast("')"),
                    )
                }
            }
        }

        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val video = Extractor.extract(server.src)

        return video
    }

    /**
     * Initializes the service with the current domain URL.
     * This function is necessary because the provider's domain frequently changes.
     * We fetch the latest URL from a dedicated website that tracks these changes.
     */
    private suspend fun initializeService() {
        initializationMutex.withLock {
            val addressService = Service.buildAddressFetcher()

            try {
                val document = addressService.getHome()

                val newUrl = document.select("div.alert-success")
                    .firstOrNull {
                        it.text().contains("Nom de domaine principal") ||
                            it.text().contains("Nouveau site")
                    }
                    ?.selectFirst("a")?.attr("href")?.trim()
                    ?.replace("http://", "https://")

                if (!newUrl.isNullOrEmpty()) {
                    URL = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
                }
            } catch (e: Exception) {
                // In case of failure, we'll use the default URL
                // No need to throw as we already have a fallback URL
            }

            service = Service.build(URL)
            serviceInitialized = true
        }
    }

    private interface Service {

        companion object {
            private val client = OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build()

            fun buildAddressFetcher(): Service {
                val addressRetrofit = Retrofit.Builder()
                    .baseUrl("https://ww1.wiflix-adresses.fun/")
                    .addConverterFactory(JsoupConverterFactory.create())
                    .client(client)
                    .build()

                return addressRetrofit.create(Service::class.java)
            }

            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(Service::class.java)
            }
        }


        @GET(".")
        suspend fun getHome(): Document

        @POST("index.php?do=search")
        @FormUrlEncoded
        suspend fun search(
            @Field("story") story: String,
            @Field("do") doo: String = "search",
            @Field("subaction") subaction: String = "search",
            @Field("search_start") searchStart: Int = 0,
            @Field("full_search") fullSearch: Int = 0,
            @Field("result_from") resultFrom: Int = 1,
        ): Document

        @GET("film-en-streaming/page/{page}")
        suspend fun getMovies(@Path("page") page: Int): Document

        @GET("serie-en-streaming/page/{page}")
        suspend fun getTvShows(@Path("page") page: Int): Document

        @GET("film-en-streaming/{id}")
        suspend fun getMovie(@Path("id") id: String): Document

        @GET("serie-en-streaming/{id}")
        suspend fun getTvShow(@Path("id") id: String): Document

        @GET("film-en-streaming/{genre}/page/{page}")
        suspend fun getGenre(
            @Path("genre") genre: String,
            @Path("page") page: Int,
        ): Document

        @GET("xfsearch/acteurs/{id}/page/{page}")
        suspend fun getPeople(
            @Path("id") id: String,
            @Path("page") page: Int,
        ): Document
    }
}