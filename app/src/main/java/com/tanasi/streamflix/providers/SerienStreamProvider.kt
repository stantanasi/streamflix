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
import java.util.Locale
import java.util.concurrent.TimeUnit


object SerienStreamProvider : Provider {

    override val name = "SerienStream (German)"
    override val logo =
        "https://s.to/public/img/logo-sto-serienstream-sx-to-serien-online-streaming-vod.png"
    val url = "https://serienstream.to/"

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

    override suspend fun getHome(): List<Category> {
        val document = service.getHome()
        val categories = mutableListOf<Category>()
        categories.add(
            Category(name = "Popular TV Shows",
                list = document.select("div.container > div:nth-child(7) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = getTvShowIdFromLink(it.selectFirst("a")?.attr("href") ?: ""),
                            title = it.selectFirst("a")?.text() ?: "",
                            poster = url + it.selectFirst("img")?.attr("data-src")
                        )
                    })
        )
        categories.add(
            Category(name = "Featured TV Shows",
                list = document.select("div.container > div:nth-child(11) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = getTvShowIdFromLink(it.selectFirst("a")?.attr("href") ?: ""),
                            title = it.selectFirst("a")?.text() ?: "",
                            poster = url + it.selectFirst("img")?.attr("data-src")
                        )
                    })
        )
        categories.add(
            Category(name = "Currently Popular TV Shows",
                list = document.select("div.container > div:nth-child(16) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = getTvShowIdFromLink(it.selectFirst("a")?.attr("href") ?: ""),
                            title = it.selectFirst("a")?.text() ?: "",
                            poster = url + it.selectFirst("img")?.attr("data-src")
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

        if (page == 2) return emptyList()
        val document = service.getSeriesListAlphabet()
        return document.select("a[data-alternative-titles]")
            .filter { it -> it.text().contains(query, true) }
            .map {
                TvShow(
                    id = getTvShowIdFromLink(it.attr("href")),
                    title = it.text()
                )
            }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        throw Exception("Not yet implemented")
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = service.getSeriesListAlphabet()
        return document.select(".genre > ul > li")
            .map {
                TvShow(
                    id = getTvShowIdFromLink(
                        it.selectFirst("a[data-alternative-titles]")?.attr("href") ?: ""
                    ),
                    title = it.selectFirst("a[data-alternative-titles]")?.text() ?: ""
                )
            }
    }

    override suspend fun getMovie(id: String): Movie {
        throw Exception("Not yet implemented")
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTvShow(id)
        val imdbTitleUrl =
            document.selectFirst("div.series-title a.imdb-link")?.attr("href") ?: ""
        val imdbDocument = service.getCustomUrl(imdbTitleUrl)
        return TvShow(
            id = id,
            title = document.selectFirst("h1 > span")?.text() ?: "",
            overview = document.selectFirst("p.seri_des")?.attr("data-full-description"),
            released = document.selectFirst("div.series-title > small > span:nth-child(1)")
                ?.text()
                ?: "",
            rating = imdbDocument.selectFirst("div[data-testid='hero-rating-bar__aggregate-rating__score'] span")
                ?.text()?.toDoubleOrNull() ?: 0.0,

            directors = document.select(".cast li[itemprop='director']")
                .map {
                    People(
                        id = it.selectFirst("a")?.attr("href")?.replace("/serien/", "") ?: "",
                        name = it.selectFirst("span")?.text() ?: ""
                    )
                },
            cast = document.select(".cast li[itemprop='actor']")
                .map {
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
            poster = url + document.selectFirst("div.seriesCoverBox img")?.attr("data-src"),
            banner = url + document.selectFirst("#series > section > div.backdrop")
                ?.attr("style")
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
        val seasonIdSplitted = seasonId.split("/")
        val showName = seasonIdSplitted[0]
        val seasonNumber = seasonIdSplitted[1]

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
        val shows = mutableListOf<TvShow>()
        val document = service.getGenre(id, page)
        document.select(".seriesListContainer > div")
            .map {
                shows.add(
                    TvShow(
                        id = it.selectFirst("a")?.attr("href")
                            ?.let { it1 -> getTvShowIdFromLink(it1) }
                            ?: "",
                        title = it.selectFirst("h3")?.text() ?: "",
                        poster = url + it.selectFirst("img")?.attr("data-src")
                    )
                )
            }
        return Genre(id = id, name = id, shows = shows)
    }

    override suspend fun getPeople(id: String, page: Int): People {
        val document = service.getPeople(id)
        return People(
            id = id,
            name = document.selectFirst("h1")?.text() ?: "",
            filmography = document.select(".seriesListContainer > div")
                .map {
                    TvShow(
                        id = it.selectFirst("a")?.attr("href")
                            ?.let { it1 -> getTvShowIdFromLink(it1) }
                            ?: "",
                        title = it.selectFirst("h3")?.text() ?: "",
                        poster = url + it.selectFirst("img")?.attr("data-src")
                    )
                }
        )
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val seasonIdSplitted = id.split("/")
        val showName = seasonIdSplitted[0]
        val seasonNumber = seasonIdSplitted[1]
        val episodeNumber = seasonIdSplitted[2]

        val document = service.getTvShowEpisodeServers(showName, seasonNumber, episodeNumber)
        return document.select("div.hosterSiteVideo > ul > li").map {
            val redirectUrl = url + it.selectFirst("a")?.attr("href")
            val serverAfterRedirect = service.getRedirectLink(redirectUrl)
            val videoUrl = (serverAfterRedirect.raw() as okhttp3.Response).request.url
            var videoUrlString = videoUrl.toString()
            if (it.selectFirst("h4")?.text() == "VOE")
                videoUrlString = "https://voe.sx" + videoUrl.encodedPath

            Video.Server(
                id = videoUrlString,
                name = it.selectFirst("h4")?.text() ?: ""
            )
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val link = server.id
        return Extractor.extract(link)
    }

    interface BurningSeriesService {

        companion object {
            val dnsQueryUrl = "https://1.1.1.1/dns-query"

            fun getOkHttpClient(): OkHttpClient {
                val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
                val bootstrapClient =
                    Builder().cache(appCache).readTimeout(30, TimeUnit.SECONDS)
                        .connectTimeout(30, TimeUnit.SECONDS).build()
                val dns =
                    DnsOverHttps.Builder().client(bootstrapClient).url(dnsQueryUrl.toHttpUrl())
                        .build()
                val client = Builder().dns(dns).build()
                return client
            }

            fun build(): BurningSeriesService {
                val client = getOkHttpClient()
                val retrofit = Retrofit.Builder().baseUrl(url)
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

        @GET("genre/{genreName}/{page}")
        suspend fun getGenre(
            @Path("genreName") genreName: String,
            @Path("page") page: Int
        ): Document

        @GET("serien/{peopleId}")
        suspend fun getPeople(@Path("peopleId") peopleId: String): Document

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