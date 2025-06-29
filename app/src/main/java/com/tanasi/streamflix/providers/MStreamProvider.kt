package com.tanasi.streamflix.providers

import android.util.Base64
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
import com.tanasi.streamflix.models.Video.Server
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import okhttp3.ResponseBody
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.String

object MStreamProvider : Provider {
    private val URL = Base64.decode(
        "aHR0cHM6Ly9tb2ZsaXg=", Base64.NO_WRAP
    ).toString(Charsets.UTF_8) + Base64.decode(
        "LXN0cmVhbS54eXo=", Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    override val baseUrl = URL
    override val name = Base64.decode(
        "TW9mbGl4", Base64.NO_WRAP
    ).toString(Charsets.UTF_8) + Base64.decode(
        "LXN0cmVhbQ==", Base64.NO_WRAP
    ).toString(Charsets.UTF_8)

    override val logo = "$URL/storage/branding_media/b0d168ea-8d1b-4b40-9292-65e9a600d3c6.png"
    override val language = "de"

    private val service = MStreamService.build()

    inline fun <R> JSONArray.map(transform: (JSONObject?) -> R): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until this.length()) {
            result.add(transform(this.get(i) as JSONObject))
        }
        return result
    }

    fun getMovieObj(json: JSONObject?, reducePosterSize: Boolean): Movie {
        val jsonTitle = json?.optJSONObject("title") ?: json
        return Movie(
            id = jsonTitle?.optString("id") + "#" + jsonTitle?.optJSONObject("primary_video")
                ?.optString("id"),
            title = jsonTitle?.optString("name") ?: "unknown title",
            poster = (((if (jsonTitle?.optString("poster")
                    ?.contains("http") == false
            ) ("$URL/") else "") + jsonTitle?.optString("poster"))).replace(
                "original", if (reducePosterSize == true) "original" else "w300"
            ),
            banner = (if (jsonTitle?.optString("backdrop")
                    ?.contains("http") == false
            ) ("$URL/") else "") + jsonTitle?.optString("backdrop"),
            overview = jsonTitle?.optString("description"),
            released = jsonTitle?.optString("year"),
            rating = jsonTitle?.optDouble("rating"),
            runtime = jsonTitle?.optInt("runtime"),
            genres = jsonTitle?.optJSONArray("genres")?.map {
                Genre(
                    id = it?.getString("id") ?: "",
                    name = it?.getString("display_name") ?: "",
                    shows = emptyList()
                )
            } ?: emptyList(),
            cast = json?.optJSONObject("credits")?.optJSONArray("actors")?.map {
                People(
                    id = it?.getString("id") ?: "",
                    name = it?.getString("name") ?: "",
                    image = it?.getString("poster") ?: "",
                )
            } ?: emptyList(),
            directors = json?.optJSONObject("credits")?.optJSONArray("directing")?.map {
                People(
                    id = it?.getString("id") ?: "",
                    name = it?.getString("name") ?: "",
                    image = it?.getString("poster") ?: "",
                )
            } ?: emptyList(),
        )
    }

    fun getTvShowObj(json: JSONObject?): TvShow {
        val jsonTitle = json?.optJSONObject("title") ?: json
        return TvShow(
            id = jsonTitle?.optString("id") ?: "0",
            title = jsonTitle?.optString("name") ?: "unknown title",
            poster = ((if (jsonTitle?.optString("poster")
                    ?.contains("http") == false
            ) ("$URL/") else "") + jsonTitle?.optString("poster")),
            banner = (if (jsonTitle?.optString("backdrop")
                    ?.contains("http") == false
            ) ("$URL/") else "") + jsonTitle?.optString("backdrop"),
            overview = jsonTitle?.optString("description"),
            released = jsonTitle?.optString("year"),
            rating = jsonTitle?.optDouble("rating"),
            genres = jsonTitle?.optJSONArray("genres")?.map {
                Genre(
                    id = it?.getString("id") ?: "",
                    name = it?.getString("display_name") ?: "",
                    shows = emptyList()
                )
            } ?: emptyList(),
            cast = json?.optJSONObject("credits")?.optJSONArray("actors")?.map {
                People(
                    id = it?.getString("id") ?: "",
                    name = it?.getString("name") ?: "",
                    image = it?.getString("poster") ?: "",
                )
            } ?: emptyList(),
            directors = json?.optJSONObject("credits")?.optJSONArray("directing")?.map {
                People(
                    id = it?.getString("id") ?: "",
                    name = it?.getString("name") ?: "",
                    image = it?.getString("poster") ?: "",
                )
            } ?: emptyList(),
            seasons = json?.optJSONObject("seasons")?.optJSONArray("data")?.map {
                Season(
                    id = it?.getString("title_id") + "_" + it?.getInt("number"),
                    number = it?.getInt("number") ?: 0,
                    poster = it?.getString("poster") ?: "",
                )
            } ?: emptyList(),
        )
    }

    override suspend fun getHome(): List<Category> {
        val homepageChannel = "350"
        val document = service.getChannel(homepageChannel)
        val json = JSONObject(document.string())
        val channelContent =
            json.getJSONObject("channel").getJSONObject("content").getJSONArray("data")
        return channelContent.map {
            Category(name = (if (it?.getString("id") == "354") Category.FEATURED
            else it?.getString("name")).toString(),
                list = (it?.optJSONObject("content")?.optJSONArray("data"))?.map {
                    if (it?.optBoolean("is_series") == true) getTvShowObj(it)
                    else getMovieObj(it, true)
                } ?: emptyList())
        }
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        var genres = listOf(
            "Drama",
            "Action",
            "Animation",
            "Abenteuer",
            "Familie",
            "Fantasy",
            "Komödie",
            "Thriller",
            "Krimi",
            "Mystery",
            "Horror",
            "Liebesfilm",
            "Historie",
            "Kriegsfilm",
            "Western",
            "Musik",
            "Dokumentarfilm",
            "Action & Adventure",
            "Sci-Fi & Fantasy",
            "Soap",
            "Kids",
            "Anime",
            "Science Fiction"
        )
        if (query.isEmpty() && page == 1) {
            return genres.map {
                Genre(id = it.replace(" & ", "-").replace(" / ", "-").replace(" ", "-"), name = it)
            }
        }
        if (page > 1) return emptyList()

        val document = service.getSearch(query)
        val json = JSONObject(document.string())
        return (json.getJSONArray("results").map {
            if (it?.optBoolean("is_series") == true) getTvShowObj(it)
            else if (it?.getString("model_type") == "movie" || it?.getString("model_type") == "title") getMovieObj(
                it, true
            )
            else null
        }).mapNotNull { it }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val homepageChannel = "345"
        val document = service.getChannelWithPage(homepageChannel, page.toString())
        val json = JSONObject(document.string())
        val channelContent = json.getJSONObject("pagination").getJSONArray("data")
        return channelContent.map { getMovieObj(it, false) }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val homepageChannel = "346"
        val document = service.getChannelWithPage(homepageChannel, page.toString())
        val json = JSONObject(document.string())
        val channelContent = json.getJSONObject("pagination").getJSONArray("data")
        return channelContent.map { getTvShowObj(it) }
    }

    override suspend fun getMovie(id: String): Movie {
        val idCleaned = id.split("#")[0]
        val document = service.getTitle(idCleaned)
        val json = JSONObject(document.string())
        return getMovieObj(json, false)
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTitle(id)
        val json = JSONObject(document.string())
        return getTvShowObj(json)
    }

    override suspend fun getEpisodesBySeason(idCombined: String): List<Episode> {
        val seasonId = idCombined.split("_")[0]
        val seasonNumber = idCombined.split("_")[1]
        val document = service.getEpisodes(seasonId, seasonNumber)
        val json = JSONObject(document.string())
        return json.getJSONObject("pagination").getJSONArray("data").map {
            Episode(
                id = (it?.getString("title_id") + "_" + it?.getInt("season_number") + "_" + it?.getInt(
                    "episode_number"
                )),
                title = it?.getString("name") ?: "",
                number = it?.getInt("episode_number") ?: 0,
                poster = it?.getString("poster") ?: "",
            )
        }
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = service.getGenre(id)
        val json = JSONObject(document.string())
        val genres = json.getJSONObject("channel").getJSONObject("content").getJSONArray("data")
        return Genre(id,
            name = json.getJSONObject("channel").getJSONObject("restriction")
                .getString("display_name"),
            shows = if (page == 1) genres.map {
                if (it?.optBoolean("is_series") == true) getTvShowObj(it)
                else getMovieObj(it, true)
            } else emptyList())
    }

    override suspend fun getPeople(id: String, page: Int): People {
        val document = service.getPerson(id)
        val json = JSONObject(document.string())
        val person = json.getJSONObject("person")
        val knownFor = json.getJSONArray("knownFor")
        return People(id = person.optString("id"),
            name = person.optString("name"),
            image = person.optString("poster"),
            placeOfBirth = person.optString("birth_place"),
            birthday = person.optString("birth_date"),
            deathday = person.optString("death_date"),

            filmography = if (page == 1) knownFor.map {
                if (it?.optBoolean("is_series") == true) getTvShowObj(it)
                else getMovieObj(it, true)
            }
            else emptyList())
    }

    override suspend fun getServers(idCombined: String, videoType: Video.Type): List<Server> {
        val isShow = idCombined.contains("_")
        var document: ResponseBody
        if (isShow) {
            val idSplit = idCombined.split("_")
            val titleId = idSplit[0]
            val seasonNumber = if (idSplit.size > 1) idSplit[1] else ""
            val episodeNumber = if (idSplit.size > 1) idSplit[2] else ""
            document = service.getEpisodeStreams(titleId, seasonNumber, episodeNumber)
            val json = JSONObject(document.string())
            return json.getJSONObject("episode").getJSONArray("videos").map {
                Server(
                    id = it?.getString("id") ?: "",
                    name = URL(it?.getString("src")).host + " ( " + it?.getString("name") + ")",
                    src = it?.getString("src") ?: ""
                )
            }
        } else {
            val idSplit = idCombined.split("#")
            val watchId = idSplit[1]
            document = service.getStreams(watchId)
            val json = JSONObject(document.string())
            return json.getJSONArray("alternative_videos").map {
                Server(
                    id = it?.getString("id") ?: "",
                    name = URL(it?.getString("src")).host + " ( " + it?.getString("name") + ")",
                    src = it?.getString("src") ?: ""
                )
            }
        }
    }

    override suspend fun getVideo(server: Server): Video {
        val link = server.src
        return Extractor.extract(link)
    }

    interface MStreamService {
        companion object {
            private const val DNS_QUERY_URL = "https://1.1.1.1/dns-query"

            private fun getOkHttpClient(): OkHttpClient {
                val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
                val clientBuilder = Builder().cache(appCache).readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                clientBuilder.addInterceptor { chain ->
                    val original = chain.request()
                    val requestWithHeaders = original.newBuilder().header("Referer", URL).build()
                    chain.proceed(requestWithHeaders)
                }
                val client = clientBuilder.addNetworkInterceptor(
                    HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
                ).build()

                val dns =
                    DnsOverHttps.Builder().client(client).url(DNS_QUERY_URL.toHttpUrl()).build()
                val clientToReturn = clientBuilder.dns(dns).build()
                return clientToReturn
            }

            fun build(): MStreamService {
                val client = getOkHttpClient()
                val retrofit = Retrofit.Builder().client(client).baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                return retrofit.create(MStreamService::class.java)
            }
        }

        @GET("/api/v1/channel/{channelId}")
        suspend fun getChannel(
            @Path("channelId") channelId: String
        ): ResponseBody

        @GET("/api/v1/channel/{channelId}?returnContentOnly=true")
        suspend fun getChannelWithPage(
            @Path("channelId") channelId: String, @Query("page") page: String
        ): ResponseBody

        @GET("/api/v1/titles/{titleId}")
        suspend fun getTitle(@Path("titleId") titleId: String): ResponseBody

        @GET("/api/v1/titles/{titleId}/seasons/{seasonNumber}/episodes?perPage=999&orderBy=episode_number&orderDir=asc")
        suspend fun getEpisodes(
            @Path("titleId") titleId: String, @Path("seasonNumber") seasonNumber: String
        ): ResponseBody

        @GET("/api/v1/titles/{titleId}/seasons/{seasonNumber}/episodes/{episodeNumber}?loader=episodePage")
        suspend fun getEpisodeStreams(
            @Path("titleId") titleId: String,
            @Path("seasonNumber") seasonNumber: String,
            @Path("episodeNumber") episodeNumber: String
        ): ResponseBody

        @GET("/api/v1/search/{searchText}?loader=searchPage")
        suspend fun getSearch(@Path("searchText") id: String): ResponseBody

        @GET("/api/v1/people/{personId}?loader=personPage")
        suspend fun getPerson(@Path("personId") id: String): ResponseBody

        @GET("/api/v1/channel/genre?channelType=channel&loader=channelPage")
        suspend fun getGenre(@Query("restriction") channelName: String): ResponseBody

        @GET("/api/v1/watch/{watchId}")
        suspend fun getStreams(@Path("watchId") id: String): ResponseBody
    }
}