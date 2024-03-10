package com.tanasi.streamflix.providers

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.fragments.player.PlayerFragment
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import okhttp3.ResponseBody
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.nodes.Document
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit


object SerienStreamProvider : Provider {
    override val name = "SerienStream (German)"
    override val logo =
        "https://s.to/public/img/logo-sto-serienstream-sx-to-serien-online-streaming-vod.png"
    override val url = "https://serienstream.to/"

    private val service = BurningSeriesService.build()

    fun getTvShowIdFromLink(link: String): String {
        val linkWithoutStaticPrefix = link.replace("/serie/stream/", "")
        val linkWithSplitedData = linkWithoutStaticPrefix.split("/")
        val justTvShowId = linkWithSplitedData[0]
        return justTvShowId
    }

    fun getSeasonIdFromLink(link: String): String {
        val linkWithoutStaticPrefix = link.replace("/serie/stream/", "")
        val linkWithSplitedData = linkWithoutStaticPrefix.split("/")
        val justTvShowId = linkWithSplitedData[0]
        val justTvShowSeason = linkWithSplitedData[1]
        return justTvShowId + "/" + justTvShowSeason
    }

    fun getEpisodeIdFromLink(link: String): String {
        val linkWithoutStaticPrefix = link.replace("/serie/stream/", "")
        val linkWithSplitedData = linkWithoutStaticPrefix.split("/")
        val justTvShowId = linkWithSplitedData[0]
        val justTvShowSeason = linkWithSplitedData[1]
        val justTvShowEpisode = linkWithSplitedData[2]
        return justTvShowId + "/" + justTvShowSeason + "/" + justTvShowEpisode
    }

    suspend fun getPosterUrlFromTvShowLink(seriesLink: String): String {
        Log.d("streamflixDebug", "dynamic posterpath request: " + seriesLink)
        val seriesId = getTvShowIdFromLink(seriesLink)
        val document = service.getTvShow(seriesId)
        val posterPath = document.select("img").attr("data-src")
        Log.d("streamflixDebug", "dynamic posterpath: " + posterPath)
        return url + posterPath
    }

    override suspend fun getHome(): List<Category> {
        val document = service.getHome()
        val categories = mutableListOf<Category>()

        categories.add(
            Category(name = "Popular TV Shows",
                list = document.select("div.container > div:nth-child(7) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = getTvShowIdFromLink(it.select("a").attr("href")),
                            title = it.select("a").text(),
                            poster = url + it.select("img").attr("data-src")
                        )
                    })
        )
        categories.add(
            Category(name = "Featured TV Shows",
                list = document.select("div.container > div:nth-child(11) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = getTvShowIdFromLink(it.select("a").attr("href")),
                            title = it.select("a").text(),
                            poster = url + it.select("img").attr("data-src")
                        )
                    })
        )
        categories.add(
            Category(name = "Recently Popular TV Shows",
                list = document.select("div.container > div:nth-child(16) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = getTvShowIdFromLink(it.select("a").attr("href")),
                            title = it.select("a").text(),
                            poster = url + it.select("img").attr("data-src")
                        )
                    })
        )
        return categories
    }

    var resultsCount = 0
    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getSeriesListWithCategories()
            val genres = mutableListOf<Genre>()
            document.select("#seriesContainer h3").map {
                genres.add(Genre(id = it.text(), name = it.text()))
            }
            return genres
        }

        if (page == 1) resultsCount = 0
        val document = service.getSeriesListAlphabet()
        val tvShows = mutableListOf<TvShow>()
        val allTitles = document.select("a[data-alternative-titles]")
        val allTitlesFiltered =
            allTitles.filter { it ->
                it.text().contains(query, true)
            }
        allTitlesFiltered.filter {
            (resultsCount++ > ((page - 1) * 20))
                    && (resultsCount < (page * 20))
        }
            .map {
                tvShows.add(
                    TvShow(
                        id = getTvShowIdFromLink(it.attr("href")),
                        title = it.text(),
                        poster = getPosterUrlFromTvShowLink(it.attr("href"))
                    )
                )
            }
        return tvShows
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        return emptyList()
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = service.getSeriesListAlphabet()
        val tvShows = mutableListOf<TvShow>()
        document.select(".genre > ul > li")
            .filter { it ->
                (it.siblingIndex() > ((page - 1) * 20))
                        && (it.siblingIndex() < (page * 20))
            }
            .map {
                tvShows.add(
                    TvShow(
                        id = getTvShowIdFromLink(
                            it.select("a[data-alternative-titles]").attr("href")
                        ),
                        title = it.select("a[data-alternative-titles]").text(),
                        poster = getPosterUrlFromTvShowLink(
                            it.select("a[data-alternative-titles]").attr("href")
                        )
                    )
                )
            }
        return tvShows
    }

    override suspend fun getMovie(id: String): Movie {
        return Movie()
    }

    override suspend fun getTvShow(tvShowId: String): TvShow {
        val document = service.getTvShow(tvShowId)
        val imdbTitleUrl = document.selectFirst("div.series-title a.imdb-link")?.attr("href") ?: ""
        val imdbDocument = service.getCustomUrl(imdbTitleUrl)

        val tvShow = TvShow(id = tvShowId,
            title = document.selectFirst("h1 > span")?.text() ?: "",
            overview = document.selectFirst("p.seri_des")?.attr("data-full-description") ?: "",
            released = document.selectFirst("div.series-title > small > span:nth-child(1)")?.text()
                ?: "",
            rating = imdbDocument.selectFirst("div[data-testid='hero-rating-bar__aggregate-rating__score'] span")
                ?.text()?.toDoubleOrNull() ?: 0.0,
            cast = document.select("div.cast li[itemprop='actor']")
                .map { People(id = it.select("span").text(), name = it.select("span").text()) },
            trailer = document.selectFirst("div[itemprop='trailer'] a")?.attr("href") ?: "",
            poster = url + document.selectFirst("div.seriesCoverBox img")?.attr("data-src"),
            banner = url + document.selectFirst("#series > section > div.backdrop")?.attr("style")
                ?.replace("background-image: url(/", "")?.replace(")", ""),
            seasons = document.select("#stream > ul:nth-child(1) > li")
                .filter { it -> it.select("a").size > 0 }.map {
                    Season(
                        id = it.selectFirst("a")?.attr("href")
                            ?.let { it1 -> getSeasonIdFromLink(it1) } ?: "",
                        number = it.selectFirst("a")?.text()?.toIntOrNull() ?: 0,
                        title = it.selectFirst("a")?.attr("title") ?: "",
                    )
                })
        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val seasonIdSplitted = seasonId.split("/")
        val showName = seasonIdSplitted[0]
        val seasonNumber = seasonIdSplitted[1]

        val document = service.getTvShowEpisodes(showName, seasonNumber)
        val episodes = mutableListOf<Episode>()
        document.select("tbody tr").map {
            episodes.add(Episode(
                id = it.selectFirst("a")?.attr("href")?.let { it1 -> getEpisodeIdFromLink(it1) }
                    ?: "",
                number = it.selectFirst("meta")?.attr("content")?.toIntOrNull() ?: 0,
                title = it.selectFirst("strong")?.text() ?: "",
            ))
        }
        return episodes
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val shows = mutableListOf<TvShow>()
        val document = service.getSeriesListWithCategories()
        document.select("#seriesContainer .genre")
            .filter { it -> it.select("h3").text() == id }.get(0)
            .select("li")
            .filter { it ->
                it.siblingIndex() > (page - 1 * 20)
                        && it.siblingIndex() < ((page) * 20)
            }
            .map {
                Log.d("streamflixDebug", "siblingIndex: " + it.siblingIndex())
                shows.add(
                    TvShow(
                        id = it.select("a").attr("href"),
                        title = it.select("a").text(),
                        poster = getPosterUrlFromTvShowLink(it.select("a").attr("href"))
                    )
                )
            }

        return Genre(id = id, name = id, shows = shows)
    }

    override suspend fun getPeople(id: String, page: Int): People {
        return People(id = id, name = id)
    }

    @OptIn(UnstableApi::class)
    override suspend fun getServers(
        episodeId: String, videoType: PlayerFragment.VideoType
    ): List<Video.Server> {
        val seasonIdSplitted = episodeId.split("/")
        val showName = seasonIdSplitted[0]
        val seasonNumber = seasonIdSplitted[1]
        val episodeNumber = seasonIdSplitted[2]

        val document = service.getTvShowEpisodeServers(showName, seasonNumber, episodeNumber)

        val servers = mutableListOf<Video.Server>()
        document.select("div.hosterSiteVideo > ul > li").map {
            val redirectUrl = url + it.select("a").attr("href")
            val serverAfterRedirect = service.getRedirectLink(redirectUrl)

            Log.d(
                "streamflixDebug",
                "redirect: " + (serverAfterRedirect.raw() as okhttp3.Response).request.url + " from url: " + url + it.select(
                    "a"
                ).attr("href")
            )
            servers.add(
                Video.Server(
                    id = (serverAfterRedirect.raw() as okhttp3.Response).request.url.toString(),
                    name = it.select("h4").text()
                )
            )
        }
        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val link = server.id
        return Extractor.extract(link)
    }

    interface BurningSeriesService {

        companion object {
            val dnsQueryUrl = "https://1.1.1.1/dns-query"

            fun okHttpClient(): OkHttpClient {
                val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
                val bootstrapClient = Builder().cache(appCache).readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS).build()
                val dns =
                    DnsOverHttps.Builder().client(bootstrapClient).url(dnsQueryUrl.toHttpUrl())
                        .build()
                val client = Builder().dns(dns).build()
                return client
            }

            fun build(): BurningSeriesService {
                val client = okHttpClient()

                val retrofit = Retrofit.Builder().baseUrl(SerienStreamProvider.url)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create()).client(client).build()
                return retrofit.create(BurningSeriesService::class.java)
            }
        }

        @GET(".")
        suspend fun getHome(): Document

        @GET("serien")
        suspend fun getSeriesListWithCategories(): Document

        @GET("serien-alphabet")
        suspend fun getSeriesListAlphabet(): Document

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
        @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        suspend fun getRedirectLink(@Url url: String): Response<ResponseBody>
    }
}