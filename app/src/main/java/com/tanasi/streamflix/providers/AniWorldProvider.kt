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
import okhttp3.ResponseBody
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
import java.util.Locale
import java.util.concurrent.TimeUnit

object AniWorldProvider : Provider {

    private const val URL = "https://aniworld.to/"

    override val name = "AniWorld"
    override val logo = ""
    override val language = "de"

    private val service = Service.build()

    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = "Beliebt bei AniWorld",
                list = document.select("div.container > div:nth-child(7) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = it.selectFirst("a")
                                ?.attr("href")?.substringAfter("/anime/stream/")
                                ?: "",
                            title = it.selectFirst("a h3")
                                ?.text()
                                ?: "",
                            poster = it.selectFirst("img")
                                ?.attr("data-src")?.let { src -> URL + src },
                        )
                    }
            )
        )

        categories.add(
            Category(
                name = "Neue Animes",
                list = document.select("div.container > div:nth-child(11) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = it.selectFirst("a")
                                ?.attr("href")?.substringAfter("/anime/stream/")
                                ?: "",
                            title = it.selectFirst("a h3")
                                ?.text()
                                ?: "",
                            poster = it.selectFirst("img")
                                ?.attr("data-src")?.let { src -> URL + src },
                        )
                    }
            )
        )

        categories.add(
            Category(
                name = "Derzeit beliebte Animes",
                list = document.select("div.container > div:nth-child(16) > div.previews div.coverListItem")
                    .map {
                        TvShow(
                            id = it.selectFirst("a")
                                ?.attr("href")?.substringAfter("/anime/stream/")
                                ?: "",
                            title = it.selectFirst("a h3")
                                ?.text()
                                ?: "",
                            poster = it.selectFirst("img")
                                ?.attr("data-src")?.let { src -> URL + src },
                        )
                    }
            )
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getGenres()

            val genres = document.select("#seriesContainer h3").map {
                Genre(
                    id = it.text().lowercase(Locale.getDefault()),
                    name = it.text(),
                )
            }

            return genres
        }

        if (page > 1) return emptyList()

        val results = service.search(query).map {
            TvShow(
                id = it.link.substringAfter("/anime/stream/"),
                title = Jsoup.parse(it.title).text(),
                overview = Jsoup.parse(it.description).text(),
            )
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        throw Exception("Keine Filme verfügbar")
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        if (page > 1) return emptyList()

        val document = service.getAnimesAlphabet()

        val tvShows = document.select(".genre > ul > li").map {
            TvShow(
                id = it.selectFirst("a[data-alternative-title]")
                    ?.attr("href")?.substringAfter("/anime/stream/")
                    ?: "",
                title = it.selectFirst("a[data-alternative-title]")
                    ?.text()
                    ?: "",
            )
        }

        return tvShows
    }

    override suspend fun getMovie(id: String): Movie {
        throw Exception("Keine Filme verfügbar")
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getAnime(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("h1 > span")
                ?.text()
                ?: "",
            overview = document.selectFirst("p.seri_des")
                ?.attr("data-full-description"),
            released = document.selectFirst("div.series-title > small > span:nth-child(1)")
                ?.text()
                ?: "",
            trailer = document.selectFirst("div[itemprop='trailer'] a")
                ?.attr("href"),
            poster = document.selectFirst("div.seriesCoverBox img")
                ?.attr("data-src")?.let { URL + it },
            banner = document.selectFirst("#series > section > div.backdrop")
                ?.attr("style")
                ?.replace("background-image: url(/", "")?.replace(")", "")
                ?.let { URL + it },


            seasons = document.select("#stream > ul:nth-child(1) > li")
                .filter { it -> it.select("a").size > 0 }
                .map {
                    Season(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringAfter("/anime/stream/")
                            ?: "",
                        number = it.selectFirst("a")
                            ?.text()?.toIntOrNull()
                            ?: 0,
                        title = it.selectFirst("a")
                            ?.attr("title"),
                    )
                },
            genres = document.select(".genres li").map {
                Genre(
                    id = it.selectFirst("a")
                        ?.text()?.lowercase(Locale.getDefault())
                        ?: "",
                    name = it.selectFirst("a")
                        ?.text()
                        ?: "",
                )
            },
            directors = document.select(".cast li[itemprop='director']").map {
                People(
                    id = it.selectFirst("a")
                        ?.attr("href")?.substringAfter("/animes/")
                        ?: "",
                    name = it.selectFirst("span")
                        ?.text()
                        ?: ""
                )
            },
            cast = document.select(".cast li[itemprop='actor']").map {
                People(
                    id = it.selectFirst("a")
                        ?.attr("href")?.substringAfter("/animes/")
                        ?: "",
                    name = it.selectFirst("span")
                        ?.text()
                        ?: "",
                )
            },
        )

        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val (tvShowId, season) = seasonId.split("/")

        val document = service.getSeason(tvShowId, season)

        val episodes = document.select("tbody tr").map {
            Episode(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringAfter("/anime/stream/")
                    ?: "",
                number = it.selectFirst("meta")
                    ?.attr("content")?.toIntOrNull()
                    ?: 0,
                title = it.selectFirst("strong")
                    ?.text(),
            )
        }

        return episodes
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        if (page > 1) return Genre(id, "")

        val document = service.getGenre(id, page)

        val genre = Genre(
            id = id,
            name = document.selectFirst("h1")
                ?.text()?.substringBefore(" Animes")
                ?: "",

            shows = document.select(".seriesListContainer > div").map {
                TvShow(
                    id = it.selectFirst("a")
                        ?.attr("href")?.substringAfter("/anime/stream/")
                        ?: "",
                    title = it.selectFirst("h3")
                        ?.text()
                        ?: "",
                    poster = it.selectFirst("img")
                        ?.attr("data-src")?.let { src -> URL + src },
                )
            }
        )

        return genre
    }

    override suspend fun getPeople(id: String, page: Int): People {
        if (page > 1) return People(id, "")

        val document = service.getPeople(id)

        val people = People(
            id = id,
            name = document.selectFirst("h1 strong")
                ?.text()
                ?: "",

            filmography = document.select(".seriesListContainer > div").map {
                TvShow(
                    id = it.selectFirst("a")
                        ?.attr("href")?.substringAfter("/anime/stream/")
                        ?: "",
                    title = it.selectFirst("h3")
                        ?.text() ?: "",
                    poster = it.selectFirst("img")
                        ?.attr("data-src")?.let { src -> URL + src },
                )
            }
        )

        return people
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val (tvShowId, seasonId, episodeId) = id.split("/")

        val document = service.getEpisode(tvShowId, seasonId, episodeId)

        val servers = document.select("div.hosterSiteVideo > ul > li").mapNotNull {
            val redirectUrl = it.selectFirst("a")
                ?.attr("href")?.let { href -> URL + href }
                ?: return@mapNotNull null

            val name = it.selectFirst("h4")
                ?.text()?.let { name ->
                    name + when (it.attr("data-lang-key")) {
                        "1" -> " - DUB"
                        "2" -> " - SUB English"
                        "3" -> " - SUB"
                        else -> ""
                    }
                }
                ?: ""

            Video.Server(
                id = name,
                name = name,
                src = redirectUrl,
            )
        }

        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val response = service.getRedirectLink(server.src)
            .let { response -> response.raw() as okhttp3.Response }
        val videoUrl = response.request.url

        val link = when (server.name) {
            "VOE" -> "https://voe.sx${videoUrl.encodedPath}"
            else -> videoUrl.toString()
        }

        return Extractor.extract(link)
    }


    private interface Service {

        companion object {
            fun build(): Service {
                val client = OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
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

        @POST("https://aniworld.to/ajax/search")
        @FormUrlEncoded
        suspend fun search(@Field("keyword") query: String): List<SearchItem>

        @GET("animes-genres")
        suspend fun getGenres(): Document

        @GET("animes-alphabet")
        suspend fun getAnimesAlphabet(): Document

        @GET("anime/stream/{id}")
        suspend fun getAnime(@Path("id") id: String): Document

        @GET("anime/stream/{tvShowId}/{seasonId}")
        suspend fun getSeason(
            @Path("tvShowId") tvShowId: String,
            @Path("seasonId") seasonId: String,
        ): Document

        @GET("genre/{id}/{page}")
        suspend fun getGenre(
            @Path("id") id: String,
            @Path("page") page: Int,
        ): Document

        @GET("animes/{id}")
        suspend fun getPeople(@Path("id", encoded = true) id: String): Document

        @GET("anime/stream/{tvShowId}/{seasonId}/{episodeId}")
        suspend fun getEpisode(
            @Path("tvShowId") tvShowId: String,
            @Path("seasonId") seasonId: String,
            @Path("episodeId") episodeId: String,
        ): Document

        @GET
        @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        suspend fun getRedirectLink(@Url url: String): Response<ResponseBody>


        data class SearchItem(
            val title: String,
            val description: String,
            val link: String,
        )
    }
}