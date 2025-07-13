package com.tanasi.streamflix.providers

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.SerienStreamDatabase
import com.tanasi.streamflix.database.dao.TvShowDao
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.SerienStreamUpdateTvShowWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


object SerienStreamProvider : Provider {

    private val URL = Base64.decode(
        "aHR0cHM6Ly9z", Base64.NO_WRAP
    ).toString(Charsets.UTF_8) + Base64.decode(
        "LnRvLw==", Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    override val baseUrl = URL
    @SuppressLint("StaticFieldLeak")
    override val name = Base64.decode(
        "U2VyaWVuU3RyZWFt", Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    override val logo =
        "$URL/public/img/logo-sto-serienstream-sx-to-serien-online-streaming-vod.png"
    override val language = "de"
    private val service = SerienStreamService.build()


    private var tvShowDao: TvShowDao? = null
    private var isWorkerScheduled = false
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        if (tvShowDao == null) {
            tvShowDao = SerienStreamDatabase.getInstance(context).tvShowDao()

            this.appContext = context.applicationContext

        }
        if (!isWorkerScheduled) {
            scheduleUpdateWorker(context)
            isWorkerScheduled = true
        }
    }

    private fun scheduleUpdateWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SerienStreamUpdateTvShowWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SerienStreamUpdateTvShowWorker",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun getDao(): TvShowDao {
        return tvShowDao ?: throw IllegalStateException("SerienStreamProvider not initialized")
    }


    private fun getTvShowIdFromLink(link: String): String {
        val linkWithoutStaticPrefix = link.replace("/serie/stream/", "")
        val linkWithSplitData = linkWithoutStaticPrefix.split("/")
        val justTvShowId = linkWithSplitData[0]
        return justTvShowId
    }

    private fun getSeasonIdFromLink(link: String): String {
        val linkWithoutStaticPrefix = link.replace("/serie/stream/", "")
        val linkWithSplitData = linkWithoutStaticPrefix.split("/")
        val justTvShowId = linkWithSplitData[0]
        val justTvShowSeason = linkWithSplitData[1]
        return "$justTvShowId/$justTvShowSeason"
    }

    private fun getEpisodeIdFromLink(link: String): String {
        val linkWithoutStaticPrefix = link.replace("/serie/stream/", "")
        val linkWithSplitData = linkWithoutStaticPrefix.split("/")
        val justTvShowId = linkWithSplitData[0]
        val justTvShowSeason = linkWithSplitData[1]
        val justTvShowEpisode = linkWithSplitData[2]
        return "$justTvShowId/$justTvShowSeason/$justTvShowEpisode"
    }

    override suspend fun getHome(): List<Category> {
        preloadSeriesAlphabet()
        val document = service.getHome()
        val categories = mutableListOf<Category>()
        categories.add(
            Category(name = "Beliebte Serien",
                list = document.select("div.container > div:nth-child(7) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = getTvShowIdFromLink(it.selectFirst("a")?.attr("href") ?: ""),
                            title = it.selectFirst("a h3")?.text() ?: "",
                            poster = URL + it.selectFirst("img")?.attr("data-src")
                        )
                    })
        )
        categories.add(
            Category(name = "Vorgestellte Serien",
                list = document.select("div.container > div:nth-child(11) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = getTvShowIdFromLink(it.selectFirst("a")?.attr("href") ?: ""),
                            title = it.selectFirst("a h3")?.text() ?: "",
                            poster = URL + it.selectFirst("img")?.attr("data-src")
                        )
                    })
        )
        categories.add(
            Category(name = "Derzeit beliebte Serien",
                list = document.select("div.container > div:nth-child(16) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = getTvShowIdFromLink(it.selectFirst("a")?.attr("href") ?: ""),
                            title = it.selectFirst("a h3")?.text() ?: "",
                            poster = URL + it.selectFirst("img")?.attr("data-src")
                        )
                    })
        )
        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getSeriesListWithCategories()
            return document.select("#seriesContainer h3").map {
                Genre(id = it.text().lowercase(Locale.getDefault()), name = it.text())
            }
        }
        val lowerQuery = query.trim().lowercase(Locale.getDefault())
        val limit = chunkSize
        val offset = (page - 1) * chunkSize
        val results = getDao().searchTvShows(lowerQuery, limit, offset)
        return results
    }



    override suspend fun getMovies(page: Int): List<Movie> {
        throw Exception("Keine Filme verfügbar")
    }


    private val cacheLock = Any()

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val fromIndex = (page - 1) * chunkSize
        val toIndex = page * chunkSize

        if (!isSeriesCacheLoaded) {
            val cachedShows = getDao().getAll().first()
            if (cachedShows.isNotEmpty()) {
                synchronized(cacheLock) {
                    seriesCache.clear()
                    seriesCache.addAll(cachedShows)
                    isSeriesCacheLoaded = true
                }
            } else {
                preloadSeriesAlphabet()
            }
        }
        CoroutineScope(Dispatchers.IO).launch { preloadSeriesAlphabet() }
        synchronized(cacheLock) {
            if (fromIndex >= seriesCache.size) return emptyList()
            val actualToIndex = minOf(toIndex, seriesCache.size)
            return seriesCache.subList(fromIndex, actualToIndex).toList()
        }
    }

    override suspend fun getMovie(id: String): Movie {
        throw Exception("Keine Filme verfügbar")
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTvShow(id)
        val imdbTitleUrl = document.selectFirst("div.series-title a.imdb-link")?.attr("href") ?: ""
        val imdbDocument = service.getCustomUrl(imdbTitleUrl)
        return TvShow(id = id,
            title = document.selectFirst("h1 > span")?.text() ?: "",
            overview = document.selectFirst("p.seri_des")?.attr("data-full-description"),
            released = document.selectFirst("div.series-title > small > span:nth-child(1)")?.text()
                ?: "",
            rating = imdbDocument.selectFirst("div[data-testid='hero-rating-bar__aggregate-rating__score'] span")
                ?.text()?.toDoubleOrNull() ?: 0.0,

            directors = document.select(".cast li[itemprop='director']").map {
                People(
                    id = it.selectFirst("a")?.attr("href")?.replace("/serien/", "") ?: "",
                    name = it.selectFirst("span")?.text() ?: ""
                )
            },
            cast = document.select(".cast li[itemprop='actor']").map {
                People(
                    id = it.selectFirst("a")?.attr("href")?.replace("/serien/", "") ?: "",
                    name = it.selectFirst("span")?.text() ?: ""
                )
            },
            genres = document.select(".genres li").map {
                Genre(
                    id = it.selectFirst("a")?.text()?.lowercase(Locale.getDefault()) ?: "",
                    name = it.selectFirst("a")?.text() ?: ""
                )
            },
            trailer = document.selectFirst("div[itemprop='trailer'] a")?.attr("href") ?: "",
            poster = URL + document.selectFirst("div.seriesCoverBox img")?.attr("data-src"),
            banner = URL + document.selectFirst("#series > section > div.backdrop")?.attr("style")
                ?.replace("background-image: url(/", "")?.replace(")", ""),
            seasons = document.select("#stream > ul:nth-child(1) > li")
                .filter { it -> it.select("a").size > 0 }.map {
                    Season(
                        id = it.selectFirst("a")?.attr("href")
                            ?.let { it1 -> getSeasonIdFromLink(it1) } ?: "",
                        number = it.selectFirst("a")?.text()?.toIntOrNull() ?: 0,
                        title = it.selectFirst("a")?.attr("title"),
                    )
                })
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val linkWithSplitData = seasonId.split("/")
        val showName = linkWithSplitData[0]
        val seasonNumber = linkWithSplitData[1]

        val document = service.getTvShowEpisodes(showName, seasonNumber)
        return document.select("tbody tr").map {
            Episode(
                id = it.selectFirst("a")?.attr("href")?.let { it1 -> getEpisodeIdFromLink(it1) }
                    ?: "",
                number = it.selectFirst("meta")?.attr("content")?.toIntOrNull() ?: 0,
                title = it.selectFirst("strong")?.text(),
            )
        }
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        if (page > 1) return Genre(id, "")
        val shows = mutableListOf<TvShow>()
        val document = service.getGenre(id, page)
        document.select(".seriesListContainer > div").map {
            shows.add(TvShow(id = it.selectFirst("a")?.attr("href")
                ?.let { it1 -> getTvShowIdFromLink(it1) } ?: "",
                title = it.selectFirst("h3")?.text() ?: "",
                poster = URL + it.selectFirst("img")?.attr("data-src")))
        }
        return Genre(id = id, name = id, shows = shows)
    }

    override suspend fun getPeople(id: String, page: Int): People {
        if (page > 1) return People(id, "")
        val document = service.getPeople(id)
        return People(id = id,
            name = document.selectFirst("h1 strong")?.text() ?: "",
            filmography = document.select(".seriesListContainer > div").map {
                TvShow(id = it.selectFirst("a")?.attr("href")
                    ?.let { it1 -> getTvShowIdFromLink(it1) } ?: "",
                    title = it.selectFirst("h3")?.text() ?: "",
                    poster = URL + it.selectFirst("img")?.attr("data-src"))
            })
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val servers = mutableListOf<Video.Server>()
        val linkWithSplitData = id.split("/")
        val showName = linkWithSplitData[0]
        val seasonNumber = linkWithSplitData[1]
        val episodeNumber = linkWithSplitData[2]
        val document = service.getTvShowEpisodeServers(showName, seasonNumber, episodeNumber)

        for (element in document.select("div.hosterSiteVideo > ul > li")) {
            val serverName = element.selectFirst("h4")?.text() ?: "Unknown server"
            val href = element.selectFirst("a")?.attr("href") ?: "No href"
            try {
                val redirectUrl = URL + href

                val serverAfterRedirect = try {
                    service.getRedirectLink(redirectUrl)
                } catch (exception: Exception) {
                    val unsafeOkHttpClient = SerienStreamService.buildUnsafe()
                    unsafeOkHttpClient.getRedirectLink(redirectUrl)
                }
                val videoUrl = (serverAfterRedirect.raw() as okhttp3.Response).request.url
                var videoUrlString = videoUrl.toString()
                if (serverName == "VOE") {
                    videoUrlString = "https://voe.sx" + videoUrl.encodedPath
                }
                servers.add(
                    Video.Server(
                        id = videoUrlString,
                        name = serverName
                    )
                )
            }catch (e: Exception) {
                Log.e("SerienStreamProvider","Failed to process server '$serverName' with URL '$href'")
            }
        }
        return servers

    }

    override suspend fun getVideo(server: Video.Server): Video {
        val link = server.id
        return Extractor.extract(link)
    }

    private val seriesCache = mutableListOf<TvShow>()
    private const val chunkSize = 25
    private var isSeriesCacheLoaded = false

    private suspend fun preloadSeriesAlphabet() {
        val document = service.getSeriesListAlphabet()
        val elements = document.select(".genre > ul > li")

        val loadedShows = elements.map {
            val title = Jsoup.parse(it.text()).text()
            TvShow(
                id = getTvShowIdFromLink(it.selectFirst("a")?.attr("href") ?: ""),
                title = title,
                overview = "",
            )
        }
        val dao = getDao()
        val existingIds = dao.getAllIds()
        val newShows = loadedShows.filter { it.id !in existingIds }

        if (newShows.isNotEmpty()) {
            dao.insertAll(newShows)
        }
        val allShows = dao.getAll().first()
        synchronized(cacheLock) {
            seriesCache.clear()
            seriesCache.addAll(allShows)
            isSeriesCacheLoaded = true
        }

        scheduleUpdateWorker(appContext)
    }



    fun invalidateCache() {
        synchronized(cacheLock) {
            seriesCache.clear()
            isSeriesCacheLoaded = false
        }
    }


    interface SerienStreamService {

        companion object {
            private const val DNS_QUERY_URL = "https://1.1.1.1/dns-query"

            private fun getOkHttpClient(): OkHttpClient {
                val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
                val clientBuilder = OkHttpClient.Builder()
                    .cache(appCache)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                val client = clientBuilder.build()

                val dns = DnsOverHttps.Builder()
                    .client(client)
                    .url(DNS_QUERY_URL.toHttpUrl())
                    .build()
                return clientBuilder
                    .dns(dns)
                    .build()
            }

            private fun getUnsafeOkHttpClient(): OkHttpClient {
                try {
                    val trustAllCerts = arrayOf<TrustManager>(
                        object : X509TrustManager {
                            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        }
                    )
                    val sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, trustAllCerts, SecureRandom())
                    val sslSocketFactory = sslContext.socketFactory

                    val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
                    val clientBuilder = OkHttpClient.Builder()
                        .cache(appCache)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                        .hostnameVerifier { _, _ -> true }

                    val client = clientBuilder.build()

                    val dns = DnsOverHttps.Builder()
                        .client(client)
                        .url(DNS_QUERY_URL.toHttpUrl())
                        .build()

                    return clientBuilder
                        .dns(dns)
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .build()
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }

            fun build(): SerienStreamService {
                val client = getOkHttpClient()
                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
                return retrofit.create(SerienStreamService::class.java)
            }

            fun buildUnsafe(): SerienStreamService {
                val client = getUnsafeOkHttpClient()
                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
                return retrofit.create(SerienStreamService::class.java)
            }
        }


        @GET(".")
        suspend fun getHome(): Document

        @POST("https://serienstream.to/ajax/search")
        @FormUrlEncoded
        suspend fun search(@Field("keyword") query: String): List<SearchItem>

        @GET("serien")
        suspend fun getSeriesListWithCategories(): Document

        @GET("serien-alphabet")
        suspend fun getSeriesListAlphabet(): Document

        @GET("genre/{genreName}/{page}")
        suspend fun getGenre(
            @Path("genreName") genreName: String, @Path("page") page: Int
        ): Document

        @GET("serien/{peopleId}")
        suspend fun getPeople(@Path("peopleId", encoded = true) peopleId: String): Document

        @GET("serie/stream/{tvShowName}")
        suspend fun getTvShow(@Path("tvShowName") tvShowName: String): Document

        @GET("serie/stream/{tvShowName}/{seasonNumber}")
        suspend fun getTvShowEpisodes(
            @Path("tvShowName") showName: String, @Path("seasonNumber") seasonNumber: String
        ): Document

        @GET("serie/stream/{tvShowName}/{seasonNumber}/{episodeNumber}")
        suspend fun getTvShowEpisodeServers(
            @Path("tvShowName") tvShowName: String,
            @Path("seasonNumber") seasonNumber: String,
            @Path("episodeNumber") episodeNumber: String
        ): Document

        @GET
        @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        suspend fun getCustomUrl(@Url url: String): Document

        @GET
        @Headers(
            "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36",
            "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language: en-US,en;q=0.5",
            "Connection: keep-alive"
        )
        suspend fun getRedirectLink(@Url url: String): Response<ResponseBody>

        data class SearchItem(
            val title: String,
            val description: String,
            val link: String,
        )
    }
}