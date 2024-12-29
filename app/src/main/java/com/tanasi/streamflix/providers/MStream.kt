package com.tanasi.streamflix.providers

import android.util.Base64
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
import com.tanasi.streamflix.models.Video.Server
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import okhttp3.dnsoverhttps.DnsOverHttps
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.String

object MStream : Provider {
    private val URL = Base64.decode(
        "aHR0cHM6Ly9tb2ZsaXg=", Base64.NO_WRAP
    ).toString(Charsets.UTF_8) + Base64.decode(
        "LXN0cmVhbS54eXo=", Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    override val name = Base64.decode(
        "TW9mbGl4", Base64.NO_WRAP
    ).toString(Charsets.UTF_8) + Base64.decode(
        "LXN0cmVhbQ==", Base64.NO_WRAP
    ).toString(Charsets.UTF_8)

    override val logo = URL + "/favicon/icon-192x192.png"
    override val language = "de"

    private val service = MStreamService.build()

    inline fun <R> JSONArray.map(transform: (JSONObject?) -> R): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until this.length()) {
            result.add(transform(this.get(i) as JSONObject))
        }
        return result
    }

    override suspend fun getHome(): List<Category> {
        val homepageChannel = "350"
        val document = service.getChannel(homepageChannel)
        val documentWithoutEscapeChars = document.select("body").text()
        val json = JSONObject(documentWithoutEscapeChars)
        val channelContent =
            json.getJSONObject("channel").getJSONObject("content").getJSONArray("data")

        return channelContent.map { itemCategory ->
            Category(name = itemCategory?.get("name").toString(),
                list = (itemCategory?.getJSONObject("content")
                    ?.getJSONArray("data"))?.map { itemShowMovie ->
                        if (itemShowMovie?.getBoolean("is_series") == true) TvShow(
                            id = itemShowMovie.getString("id") ?: "",
                            title = itemShowMovie.getString("name") ?: "",
                            poster = itemShowMovie.getString("poster").replace("original", "w300"),
                        )
                        else Movie(
                            id = itemShowMovie?.getString("id") ?: "",
                            title = itemShowMovie?.getString("name") ?: "",
                            poster = itemShowMovie?.getString("poster")?.replace("original", "w300")
                                ?: "",
                        )
                    } ?: emptyList())
        }

    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        return emptyList()
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        TODO("Not yet implemented")
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        TODO("Not yet implemented")
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getTitle(id)
        val documentWithoutEscapeChars = document.select("body").text()
        val json = JSONObject(documentWithoutEscapeChars)
        val jsonTitle = json.getJSONObject("title")
        return Movie(
            id = id + "#" + jsonTitle.getJSONObject("primary_video").getString("id"),
            title = jsonTitle.getString("name"),
            poster = jsonTitle.getString("poster"),
            banner = jsonTitle.getString("backdrop"),
            overview = jsonTitle.getString("description"),
            released = jsonTitle.getString("year"),
            rating = jsonTitle.getDouble("rating"),
            runtime = jsonTitle.getInt("runtime"),
            genres = jsonTitle.optJSONArray("genres")?.map { genreItem ->
                Genre(
                    id = genreItem?.getString("id") ?: "",
                    name = genreItem?.getString("display_name") ?: "",
                    shows = emptyList()
                )
            } ?: emptyList(),
            cast = json.optJSONObject("credits")?.optJSONArray("actors")?.map { peopleItem ->
                People(
                    id = peopleItem?.getString("id") ?: "",
                    name = peopleItem?.getString("name") ?: "",
                    image = peopleItem?.getString("poster") ?: "",
                )
            } ?: emptyList(),
            directors = json.optJSONObject("credits")?.optJSONArray("directing")
                ?.map { peopleItem ->
                    People(
                        id = peopleItem?.getString("id") ?: "",
                        name = peopleItem?.getString("name") ?: "",
                        image = peopleItem?.getString("poster") ?: "",
                    )
                } ?: emptyList(),
        )
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTitle(id)
        val documentWithoutEscapeChars = document.select("body").text()
        val json = JSONObject(documentWithoutEscapeChars)
        val jsonTitle = json.getJSONObject("title")
        return TvShow(
            id = id,
            title = jsonTitle.getString("name"),
            poster = jsonTitle.getString("poster"),
            banner = jsonTitle.getString("backdrop"),
            overview = jsonTitle.getString("description"),
            released = jsonTitle.getString("year"),
            rating = jsonTitle.getDouble("rating"),
            genres = jsonTitle.optJSONArray("genres")?.map { genreItem ->
                Genre(
                    id = genreItem?.getString("id") ?: "",
                    name = genreItem?.getString("display_name") ?: "",
                    shows = emptyList()
                )
            } ?: emptyList(),
            cast = json.optJSONObject("credits")?.optJSONArray("actors")?.map { peopleItem ->
                People(
                    id = peopleItem?.getString("id") ?: "",
                    name = peopleItem?.getString("name") ?: "",
                    image = peopleItem?.getString("poster") ?: "",
                )
            } ?: emptyList(),
            directors = json.optJSONObject("credits")?.optJSONArray("directing")
                ?.map { peopleItem ->
                    People(
                        id = peopleItem?.getString("id") ?: "",
                        name = peopleItem?.getString("name") ?: "",
                        image = peopleItem?.getString("poster") ?: "",
                    )
                } ?: emptyList(),
            seasons = json.optJSONObject("seasons")?.optJSONArray("data")?.map { seasonItem ->
                Season(
                    id = seasonItem?.getString("title_id") + "_" + seasonItem?.getInt("number"),
                    number = seasonItem?.getInt("number") ?: 0,
                    poster = seasonItem?.getString("poster") ?: "",
                )
            } ?: emptyList(),
        )
    }

    override suspend fun getEpisodesBySeason(idCombined: String): List<Episode> {
        val seasonId = idCombined.split("_")[0]
        val seasonNumber = idCombined.split("_")[1]

        val document = service.getEpisodes(seasonId, seasonNumber)
        val documentWithoutEscapeChars = document.select("body").text()
        val json = JSONObject(documentWithoutEscapeChars)

        return json.getJSONObject("pagination").getJSONArray("data").map { episodeItem ->
            Episode(
                id = (episodeItem?.getString("title_id") + "_" + episodeItem?.getInt("season_number") + "_" + episodeItem?.getInt(
                    "episode_number"
                )),
                title = episodeItem?.getString("name") ?: "",
                number = episodeItem?.getInt("episode_number") ?: 0,
                poster = episodeItem?.getString("poster") ?: "",
            )
        }
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        return Genre(
            id, name = "", shows = emptyList()
        )
    }

    override suspend fun getPeople(id: String, page: Int): People {
        return People(
            id = TODO(),
            name = TODO(),
            image = TODO(),
            biography = TODO(),
            placeOfBirth = TODO(),
            birthday = TODO(),
            deathday = TODO(),
            filmography = TODO()
        )
    }

    override suspend fun getServers(idCombined: String, videoType: Video.Type): List<Server> {
        val isShow = idCombined.contains("_");
        var document: Document;
        if (isShow) {
            val idSplit = idCombined.split("_")
            val titleId = idSplit[0]
            val seasonNumber = if (idSplit.size > 1) idSplit[1] else ""
            val episodeNumber = if (idSplit.size > 1) idSplit[2] else ""
            document = service.getEpisodeStreams(titleId, seasonNumber, episodeNumber)
        } else {
            val idSplit = idCombined.split("#")
            val watchId = idSplit[1]
            document = service.getStreams(watchId)
        }

        val documentWithoutEscapeChars = document.select("body").text()
        val json = JSONObject(documentWithoutEscapeChars)

        return json.getJSONObject("episode").getJSONArray("videos").map { videoItem ->
            Server(
                id = videoItem?.getString("id") ?: "",
                name = URL(videoItem?.getString("src")).host + " ( " + videoItem?.getString("name") + ")",
                src = videoItem?.getString("src") ?: ""
            )
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
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
                val client = clientBuilder.build()

                val dns =
                    DnsOverHttps.Builder().client(client).url(DNS_QUERY_URL.toHttpUrl()).build()
                val clientToReturn = clientBuilder.dns(dns).build()
                return clientToReturn
            }

            fun build(): MStreamService {
                val client = getOkHttpClient()
                val retrofit = Retrofit.Builder().client(client).baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create()).build()
                return retrofit.create(MStreamService::class.java)
            }
        }

        @GET("/api/v1/channel/{channelId}")
        suspend fun getChannel(@Path("channelId") id: String): Document

        @GET("/api/v1/titles/{titleId}")
        suspend fun getTitle(@Path("titleId") id: String): Document

        @GET("/api/v1/titles/{titleId}/seasons/{seasonNumber}/episodes?perPage=999&orderBy=episode_number&orderDir=asc")
        suspend fun getEpisodes(
            @Path("titleId") titleId: String, @Path("seasonNumber") seasonNumber: String
        ): Document

        @GET("/api/v1/titles/{titleId}/seasons/{seasonNumber}/episodes/{episodeNumber}?loader=episodePage")
        suspend fun getEpisodeStreams(
            @Path("titleId") titleId: String,
            @Path("seasonNumber") seasonNumber: String,
            @Path("episodeNumber") episodeNumber: String
        ): Document

        @GET("/api/v1/watch/{watchId}")
        suspend fun getStreams(@Path("watchId") id: String): Document
    }
}