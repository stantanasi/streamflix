package com.tanasi.streamflix.providers

import com.google.gson.annotations.SerializedName
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.VixcloudExtractor
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.dnsoverhttps.DnsOverHttps
import org.json.JSONObject
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

object StreamingCommunityProvider : Provider {
    private const val DEFAULT_DOMAIN: String = "streamingunity.to"
    override val baseUrl = DEFAULT_DOMAIN
    private var _domain: String? = null
    private var domain: String
        get() {
            if (!_domain.isNullOrEmpty())
                return _domain!!

            val storedDomain = UserPreferences.streamingcommunityDomain

            _domain = if (storedDomain.isNullOrEmpty())
                DEFAULT_DOMAIN
            else
                storedDomain

            return _domain!!
        }
        set(value) {
            if (value != domain) {
                _domain = value
                UserPreferences.streamingcommunityDomain = value
                rebuildService(value)
            }
        }
    private const val lang = "it"

    override val name = "StreamingCommunity"
    override val logo = "https://$domain/apple-touch-icon.png"
    override val language = "it"
    private const val MAX_SEARCH_RESULTS = 60

    private var service = StreamingCommunityService.build("https://$domain/")

    fun rebuildService(newDomain: String = domain) {
        service = StreamingCommunityService.build("https://$newDomain/")
    }

    private var version: String = ""
        get() {
            if (field != "") return field

            val document = runBlocking { service.getHome() }
            field = JSONObject(document.selectFirst("#app")?.attr("data-page") ?: "").getString("version")
            return field
        }

    private fun getImageLink(filename: String?): String? {
        if (filename.isNullOrEmpty())
            return null
        return "https://cdn.$domain/images/$filename"
    }

    override suspend fun getHome(): List<Category> {
        val res = service.getHome(version = version)
        if (version != res.version) version = res.version

        val mainTitles = res.props.sliders[2].titles

        val categories = mutableListOf<Category>()

        categories.add(
            // 2: top10
            Category(
                name = Category.FEATURED,
                list = mainTitles.map {
                    if (it.type == "movie")
                        Movie(
                            id = it.id + "-" + it.slug,
                            title = it.name,
                            banner = getImageLink(it.images.find { it.type == "background" }?.filename),
                            rating = it.score
                        )
                    else
                        TvShow(
                            id = it.id + "-" + it.slug,
                            title = it.name,
                            banner = getImageLink(it.images.find { it.type == "background" }?.filename),
                            rating = it.score
                        )
                },
            )
        )

        categories.addAll(
            // 0: trending, 1:latest
            listOf(0, 1).map { index ->
                val slider = res.props.sliders[index]
                Category(
                    name = slider.label,
                    list = slider.titles.map {
                        if (it.type == "movie")
                            Movie(
                                id = it.id + "-" + it.slug,
                                title = it.name,
                                released = it.lastAirDate,
                                rating = it.score,
                                poster = getImageLink(it.images.find { it.type == "poster" }?.filename),
                                banner = getImageLink(it.images.find { it.type == "background" }?.filename)
                            )
                        else
                            TvShow(
                                id = it.id + "-" + it.slug,
                                title = it.name,
                                released = it.lastAirDate,
                                rating = it.score,
                                poster = getImageLink(it.images.find { it.type == "poster" }?.filename),
                                banner = getImageLink(it.images.find { it.type == "background" }?.filename)
                            )
                    }
                )
            }
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val res = service.getHome(version = version)
            if (version != res.version) version = res.version

            return res.props.genres.map {
                Genre(
                    id = it.id,
                    name = it.name
                )
            }.sortedBy { it.name }
        }

        val res = service.search(query, (page - 1) * MAX_SEARCH_RESULTS)
        if (res.currentPage == null || res.lastPage == null || res.currentPage > res.lastPage) {
            return listOf()
        }

        return res.data.map {
            val poster = getImageLink(it.images.find { it.type == "poster" }?.filename)

            if (it.type == "movie")
                Movie(
                    id = it.id + "-" + it.slug,
                    title = it.name,
                    released = it.lastAirDate,
                    rating = it.score,
                    poster = poster
                )
            else
                TvShow(
                    id = it.id + "-" + it.slug,
                    title = it.name,
                    released = it.lastAirDate,
                    rating = it.score,
                    poster = poster
                )
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        if (page > 1)
            return listOf()

        val res = service.getArchive(type = "movie", version = version)

        val movies = mutableListOf<Movie>()

        res.titles.map { title ->
            val poster = getImageLink(title.images.find { it.type == "poster" }?.filename)

            movies.add(
                Movie(
                    id = title.id + "-" + title.slug,
                    title = title.name,
                    released = title.lastAirDate,
                    rating = title.score,
                    poster = poster
                )
            )
        }

        return movies.distinctBy { it.id }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        if (page > 1)
            return listOf()

        val res = service.getArchive(type = "tv", version = version)

        val tvShows = mutableListOf<TvShow>()

        res.titles.map { title ->
            val poster = getImageLink(title.images.find { it.type == "poster" }?.filename)

            tvShows.add(
                TvShow(
                    id = title.id + "-" + title.slug,
                    title = title.name,
                    released = title.lastAirDate,
                    rating = title.score,
                    poster = poster
                )
            )
        }

        return tvShows.distinctBy { it.id }
    }


    override suspend fun getMovie(id: String): Movie {
        val res = service.getDetails(id, version = version)
        if (version != res.version) version = res.version

        val title = res.props.title

        return Movie(
            id = id,
            title = title.name,
            overview = title.plot,
            released = title.lastAirDate,
            rating = title.score,
            poster = getImageLink(title.images.find { it.type == "poster" }?.filename),
            genres = title.genres?.map {
                Genre(
                    id = it.id,
                    name = it.name
                )
            } ?: listOf(),
            cast = title.actors?.map {
                People (
                    id = it.name,
                    name = it.name
                )
            } ?: listOf(),
            trailer = let {
                val id = title.trailers?.find { it.youtubeId != "" }?.youtubeId
                if (!id.isNullOrEmpty())
                    "https://youtube.com/watch?v=$id"
                else
                    null
            },
            recommendations = let {
                if (res.props.sliders.isNotEmpty())
                    res.props.sliders[0].titles.map {
                        if (it.type == "movie") {
                            Movie(
                                id = it.id + "-" + it.slug,
                                title = it.name,
                                rating = it.score,
                                poster = getImageLink(it.images.find { it.type == "poster" }?.filename)
                            )
                        } else {
                            TvShow(
                                id = it.id + "-" + it.slug,
                                title = it.name,
                                rating = it.score,
                                poster = getImageLink(it.images.find { it.type == "poster" }?.filename)
                            )
                        }
                    }
                else
                    listOf()
            }
        )
    }

    override suspend fun getTvShow(id: String): TvShow {
        val res = service.getDetails(id, version = version)
        if (version != res.version) version = res.version

        val title = res.props.title

        return TvShow(
            id = id,
            title = title.name,
            overview = title.plot,
            released = title.lastAirDate,
            rating = title.score,
            poster = getImageLink(title.images.find { it.type == "poster" }?.filename),
            genres = title.genres?.map {
                Genre(
                    id = it.id,
                    name = it.name
                )
            } ?: listOf(),
            cast = title.actors?.map {
                People (
                    id = it.name,
                    name = it.name
                )
            } ?: listOf(),
            trailer = let {
                val id = title.trailers?.find { it.youtubeId != "" }?.youtubeId
                if (!id.isNullOrEmpty())
                    "https://youtube.com/watch?v=$id"
                else
                    null
            },
            recommendations = let {
                if (res.props.sliders.isNotEmpty())
                    res.props.sliders[0].titles.map {
                        if (it.type == "movie") {
                            Movie(
                                id = it.id + "-" + it.slug,
                                title = it.name,
                                rating = it.score,
                                poster = getImageLink(it.images.find { it.type == "poster" }?.filename)
                            )
                        } else {
                            TvShow(
                                id = it.id + "-" + it.slug,
                                title = it.name,
                                rating = it.score,
                                poster = getImageLink(it.images.find { it.type == "poster" }?.filename)
                            )
                        }
                    }
                else
                    listOf()
            },
            seasons = title.seasons?.map {
                Season(
                    id = "$id/season-${it.number}",
                    number = it.number.toIntOrNull() ?: (title.seasons.indexOf(it) + 1),
                    title = it.name
                )
            } ?: listOf()
        )
    }


    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val res = service.getSeasonDetails(seasonId, version = version)
        if (version != res.version) version = res.version

        return res.props.loadedSeason.episodes.map {
            Episode(
                id = "${seasonId.substringBefore("-")}?episode_id=${it.id}",
                number = it.number.toIntOrNull() ?: (res.props.loadedSeason.episodes.indexOf(it) + 1),
                title = it.name,
                poster = getImageLink(it.images.find { it.type == "cover" }?.filename)
            )
        }
    }


    override suspend fun getGenre(id: String, page: Int): Genre {
        val res = service.getArchive(genreId = id, offset = (page - 1) * MAX_SEARCH_RESULTS, version = version)

        val genre = Genre(
            id = id,
            name = "",

            shows = res.titles.map {
                val poster = getImageLink(it.images.find { it.type == "poster" }?.filename)

                if (it.type == "movie")
                    Movie(
                        id = it.id + "-" + it.slug,
                        title = it.name,
                        released = it.lastAirDate,
                        rating = it.score,
                        poster = poster
                    )
                else
                    TvShow(
                        id = it.id + "-" + it.slug,
                        title = it.name,
                        released = it.lastAirDate,
                        rating = it.score,
                        poster = poster
                    )
            }
        )

        return genre
    }


    override suspend fun getPeople(id: String, page: Int): People {
        val res = service.search(id, (page - 1) * MAX_SEARCH_RESULTS)
        if (res.currentPage == null || res.lastPage == null || res.currentPage > res.lastPage) {
            return People(
                id = id,
                name = id
            )
        }

        return People(
            id = id,
            name = id,
            filmography = res.data.map {
                val poster = getImageLink(it.images.find { it.type == "poster" }?.filename)

                if (it.type == "movie")
                    Movie(
                        id = it.id + "-" + it.slug,
                        title = it.name,
                        released = it.lastAirDate,
                        rating = it.score,
                        poster = poster
                    )
                else
                    TvShow(
                        id = it.id + "-" + it.slug,
                        title = it.name,
                        released = it.lastAirDate,
                        rating = it.score,
                        poster = poster
                    )
            }
        )
    }


    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val document = when (videoType) {
            is Video.Type.Movie -> service.getIframe(
                id.substringBefore("-")
            )
            is Video.Type.Episode -> service.getIframe(
                id.substringBefore("?"),
                id.substringAfter("=")
            )
        }

        val src = document.selectFirst("iframe")?.attr("src") ?: ""

        return listOf(Video.Server(
            id = id,
            name = "Vixcloud",
            src = src
        ))
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return VixcloudExtractor().extract(server.src)
    }


    private class UserAgentInterceptor(private val userAgent: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build()
            return chain.proceed(requestWithUserAgent)
        }
    }

    private class RedirectInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            val location = response.header("Location")
            if (!location.isNullOrEmpty()) {
                domain = location
                    .substringAfter("https://")
                    .substringBefore("/")
            }
            return response
        }
    }

    private interface StreamingCommunityService {

        companion object {
            private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"

            fun build(baseUrl: String): StreamingCommunityService {
                val clientBuilder = OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(UserAgentInterceptor(USER_AGENT))
                    .addNetworkInterceptor(RedirectInterceptor())

                if (UserPreferences.streamingcommunityDnsOverHttps) {
                    val bootstrapClient = OkHttpClient.Builder().build()
                    val dohDns = DnsOverHttps.Builder().client(bootstrapClient)
                        .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                        .build()

                    clientBuilder.dns(dohDns)
                }

                val client = clientBuilder.build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(StreamingCommunityService::class.java)
            }
        }


        @GET("/$lang")
        suspend fun getHome(): Document

        @GET("/$lang")
        suspend fun getHome(
            @Header("x-inertia") xInertia: String = "true",
            @Header("x-inertia-version") version: String
        ): HomeRes

        @GET("api/search")
        suspend fun search(
            @Query("q", encoded = true) keyword: String,
            @Query("offset") offset: Int = 0,
            @Query("lang") language: String = lang
        ): SearchRes

        @GET("api/archive")
        suspend fun getArchive(
            @Query("genre[]") genreId: String? = null,
            @Query("type") type: String? = null,
            @Query("offset") offset: Int = 0,
            @Header("x-inertia") xInertia: String = "true",
            @Header("x-inertia-version") version: String,
            @Query("lang") language: String = lang
        ): ArchiveRes

        @GET("$lang/titles/{id}")
        suspend fun getDetails(
            @Path("id") id: String,
            @Header("x-inertia") xInertia: String = "true",
            @Header("x-inertia-version") version: String
        ): HomeRes

        @GET("$lang/titles/{id}/")
        suspend fun getSeasonDetails(
            @Path("id") id: String,
            @Header("x-inertia") xInertia: String = "true",
            @Header("x-inertia-version") version: String
        ): SeasonRes

        @GET("$lang/iframe/{id}")
        suspend fun getIframe(@Path("id") id: String): Document

        @GET("$lang/iframe/{id}")
        suspend fun getIframe(@Path("id") id: String,
                              @Query("episode_id") episodeId: String,
                              @Query("next_episode") nextEpisode: Char = '1'
        ): Document

        data class Image(
            val filename: String,
            val type: String
        )
        data class Genre(
            val id: String,
            val name: String
        )
        data class Actor(
            val id: String,
            val name: String
        )
        data class Trailer(
            @SerializedName("youtube_id") val youtubeId: String?
        )
        data class Season(
            val number: String,
            val name: String?
        )
        data class Show(
            val id: String,
            val name: String,
            val type: String,
            val score: Double,
            val lastAirDate: String,
            val images: List<Image>,
            val slug: String,
            val plot: String?,
            val genres: List<Genre>?,
            @SerializedName("main_actors") val actors: List<Actor>?,
            val trailers: List<Trailer>?,
            val seasons: List<Season>?
        )
        data class Slider(
            val label: String,
            val name: String,
            val titles: List<Show>
        )
        data class Props(
            val genres: List<Genre>,
            val sliders: List<Slider>,
            val title: Show
        )

        data class HomeRes(
            val version: String,
            val props: Props
        )

        data class SearchRes(
            val data: List<Show>,
            @SerializedName("current_page") val currentPage: Int?,
            @SerializedName("last_page") val lastPage: Int?
        )

        data class SeasonPropsEpisodes(
            val id: String,
            val images: List<Image>,
            val name: String,
            val number: String
        )
        data class SeasonPropsDetails(
            val episodes: List<SeasonPropsEpisodes>
        )
        data class SeasonProps(
            val loadedSeason: SeasonPropsDetails
        )
        data class SeasonRes(
            val version: String,
            val props: SeasonProps
        )

        data class ArchiveRes(
            val titles: List<Show>
        )
    }
}
