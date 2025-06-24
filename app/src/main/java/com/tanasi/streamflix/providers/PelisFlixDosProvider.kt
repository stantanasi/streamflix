package com.tanasi.streamflix.providers

import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import android.util.Base64
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
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
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.File
import java.util.concurrent.TimeUnit

object PelisFlixDosProvider: Provider {
//    private val URL = Base64.decode(
//        "aHR0cHM6Ly9wZWxpc2ZsaXgyMC53aWtpLw==", Base64.NO_WRAP).toString()
    private val URL = "https://pelisflix20.wiki/"
    override val baseUrl = URL
    override val name = "PelisFlix 2"
    override val logo ="https://galaxycdn2.online/flix/imgs/logo3.png"
//    override val logo = Base64.decode("aHR0cHM6Ly9nYWxheHljZG4yLm9ubGluZS9mbGl4L2ltZ3MvbG9nbzMucG5n", Base64.NO_WRAP).toString()
    override val language = "es"

    private val service = PelisFlixDosService.build();


    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        val top10List = document.select("ul.hometop10")

        val top10Movies = top10List.select("li.mvnew").map { li ->
            val link = li.selectFirst("a")!!
            val href = link.attr("href")
            val id = href.substringAfterLast("/").trimEnd('/')

            val title = link.selectFirst("h2.Title")?.text() ?: ""

            // Poster image URL from data-src attribute of img
            val posterSrc = link.selectFirst("img")?.attr("data-src") ?: ""
            val fullPosterUrl = when {
                posterSrc.startsWith("//") -> "https:$posterSrc"
                posterSrc.startsWith("/") -> "https://pelisflix20.wiki$posterSrc"
                else -> posterSrc
            }

            Movie(
                id = id,
                title = title,
                overview = "", // No overview in this section
                released = "", // No release year in this section
                rating = 0.0, // No rating here either
                poster = fullPosterUrl
            )
        }



        val articles = document.select("ul#home-movies-post article.TPost")

        val movies = articles.map { article ->
            val title = article.select("h2.Title").text().ifEmpty {
                article.select("div.Title").firstOrNull()?.text() ?: ""
            }

            val href = article.select("a").attr("href")
            val id = href.substringAfterLast("/").trimEnd('/')

            // Poster image URL from img src or data-src
            val posterSrc = article.select("img").attr("data-src").takeIf { it.isNotEmpty() }
                ?: article.select("img").attr("src")
            val fullPosterUrl = if (posterSrc.startsWith("//")) {
                "https:$posterSrc"
            } else if (posterSrc.startsWith("/")) {
                "https://pelisflix20.wiki$posterSrc"
            } else {
                posterSrc
            }

            val overview = article.select("div.Description > p").firstOrNull()?.text() ?: ""

            val releaseYear = article.select("span.Qlty.Yr").text()
                .ifEmpty { article.select("div.Info > span.Date").text() }

            val duration = article.select("div.Info > span.Time").text()

            // Optional: Extract genres if needed
            val genres = article.select("p.Genre a").joinToString(", ") { it.text() }

            Movie(
                id = id,
                title = title,
                overview = overview,
                released = releaseYear,
                rating = 0.0, // no rating in this snippet, you could add logic if rating is somewhere
                poster = fullPosterUrl
            )
        }
        val seriesItems = document.select("html body#Tf-Wp.page-template.page-template-pages.page.BdGradient div.Tf-Wp div.Body div.MovieListSldCn div.MovieListSld div.Main.Container div.TpRwCont main section ul.MovieList.Rows.AX.A04.B03.C20.D03.E20.Serie > li")

        val series = seriesItems.map { li ->
            val title = li.select("h2.Title").text().ifEmpty {
                li.select("div.Title").firstOrNull()?.text() ?: ""
            }

            val href = li.select("a").attr("href")
            val id = href.substringAfterLast("/").trimEnd('/')

            val posterSrc = li.select("img").attr("data-src").takeIf { it.isNotEmpty() }
                ?: li.select("img").attr("src")
            val fullPosterUrl = when {
                posterSrc.startsWith("//") -> "https:$posterSrc"
                posterSrc.startsWith("/") -> "https://pelisflix20.wiki$posterSrc"
                else -> posterSrc
            }

            val overview = li.select("div.Description > p").firstOrNull()?.text() ?: ""

            val releaseYear = li.select("span.Qlty.Yr").text()
                .ifEmpty { li.select("div.Info > span.Date").text() }

            val duration = li.select("div.Info > span.Time").text()

            TvShow(
                id = id,
                title = title,
                overview = overview,
                released = releaseYear,
                rating = 0.0,
                poster = fullPosterUrl
            )
        }

        return listOf(
            Category(name="Top 10 en Pelisflix 2 hoy", list = top10Movies),
            Category(name="Ver Películas en pelisflix Online", list = movies),
            Category(name="Ver Series Online", list=series)

        )

    }

    override suspend fun search(
        query: String,
        page: Int
    ): List<AppAdapter.Item> {
        TODO("Not yet implemented")
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        TODO("Not yet implemented")
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        TODO("Not yet implemented")
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getMovie(id)
        TODO("Not yet implemented")
    }

    override suspend fun getTvShow(id: String): TvShow {
        TODO("Not yet implemented")
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        TODO("Not yet implemented")
    }

    override suspend fun getGenre(
        id: String,
        page: Int
    ): Genre {
        TODO("Not yet implemented")
    }

    override suspend fun getPeople(
        id: String,
        page: Int
    ): People {
        TODO("Not yet implemented")
    }

    override suspend fun getServers(
        id: String,
        videoType: Video.Type
    ): List<Video.Server> {
        TODO("Not yet implemented")
    }

    override suspend fun getVideo(server: Video.Server): Video {
        TODO("Not yet implemented")
    }

    private interface PelisFlixDosService {
        companion object {
            private const val DNS_QUERY_URL = "https://1.1.1.1/dns-query"

            private fun getOkHttpClient(): OkHttpClient {
                val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)
                val clientBuilder = Builder().cache(appCache).readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                val client = clientBuilder.build()

                val dns =
                    DnsOverHttps.Builder().client(client).url(DNS_QUERY_URL.toHttpUrl()).build()
                val clientToReturn = clientBuilder.dns(dns).build()
                return clientToReturn
            }

            fun build(): PelisFlixDosService {
                val client = getOkHttpClient()
                val retrofit = Retrofit.Builder().baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create()).client(client).build()
                return retrofit.create(PelisFlixDosService::class.java)
            }
        }


        @GET(".")
        suspend fun getHome(): Document

        @GET("/?q={query}")
        suspend fun search(@Path("query") query: String): Document

        @GET("pelicula/{name}/")
        suspend fun getMovie(@Path("name") id: String): Document

        @GET
        @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        suspend fun getRedirectLink(@Url url: String): Response<ResponseBody>

        @GET("archives/movies/page/{page}")
        suspend fun getMovies(@Path("page") page: Int): Document

        @GET("archives/series/page/{page}")
        suspend fun getTvShows(@Path("page") page: Int): Document

        @GET("series/{name}")
        suspend fun getTvShow(@Path("name") id: String): Document

        @GET("series/{name}/seasons/{seasonNumber}/episodes/{episodeNumber}")
        suspend fun getEpisode(
            @Path("name") name: String,
            @Path("seasonNumber") seasonNumber: String,
            @Path("episodeNumber") episodeNumber: String
        ): Document

        @GET("/genres/{genre}/page/{page}")
        suspend fun getGenre(@Path("genre") genre: String, @Path("page") page: Int): Document
    }
}