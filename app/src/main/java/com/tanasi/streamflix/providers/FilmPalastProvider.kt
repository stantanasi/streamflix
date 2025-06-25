package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import okhttp3.ResponseBody
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url
import java.io.File
import java.util.concurrent.TimeUnit

object FilmPalastProvider : Provider {

    private val BASE_URL = "https://filmpalast.to/"
    override val baseUrl = BASE_URL
    override val name = "Filmpalast"
    override val logo = "$BASE_URL/themes/downloadarchive/images/logo.png"
    override val language = "de"

    private val service = FilmpalastService.build()

    override suspend fun getHome(): List<Category> {
        val document = service.getHome()
        val featured = document.select("div.headerslider ul#sliderDla li").map { li ->
            val title = li.select("span.title.rb").text()
            val href = li.select("a.moviSliderPlay").attr("href")
            val id = href.substringAfterLast("/")
            val posterSrc = li.select("a img").attr("src")
            val fullPosterUrl = if (posterSrc.startsWith("/")) {
                "https://filmpalast.to$posterSrc"
            } else {
                posterSrc
            }
            val overview = li.select("div.moviedescription").text()

            val releaseYearText = li.select("span.releasedate b").text()

            val imdbRatingText =
                li.select("span.views b").lastOrNull()?.text()?.split("/")?.get(1)?.trim()
            val rating = imdbRatingText?.toDoubleOrNull() ?: 0.0

            Movie(
                id = id,
                title = title,
                overview = overview,
                released = releaseYearText,
                rating = rating,
                poster = fullPosterUrl
            )
        }


        val main_content = document.select("div#content article").map { article ->
            val href = article.selectFirst("h2 a")?.attr("href") ?: ""
            val title = article.selectFirst("h2 a")?.text() ?: ""
            val posterSrc = article.selectFirst("a img")?.attr("src") ?: ""

            val fullPosterUrl = if (posterSrc.startsWith("/")) {
                "https://filmpalast.to$posterSrc"
            } else {
                posterSrc
            }

            val info = article.select("*").toInfo()

            Movie(
                id = href.substringAfterLast("/"),
                title = title,
                released = info.released,
                quality = info.quality,
                rating = info.rating ?: 0.0,
                poster = fullPosterUrl
            )
        }
        return listOf(
            Category(name = "Featured", list = featured),
            Category(name = "Mixed", list = main_content)
        )
    }


    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getHome()
            val genres = document.select("aside#sidebar section#genre ul li a").map { element ->
                val text = element.text()
                Genre(text, text)
            }
            return genres
        }
        var document = service.searchNoPage(query)

        if (page > 1){
            val paging = document.selectFirst("div#paging a.pageing.button-small.rb")
            if (paging != null){
                document = service.search(query, page)
            } else {
                return emptyList()
            }
        }

        val movies = document.select("div#content article").map { article ->
            val href = article.selectFirst("h2 a")?.attr("href") ?: ""
            val title = article.selectFirst("h2 a")?.text() ?: ""
            val posterSrc = article.selectFirst("a img")?.attr("src") ?: ""

            val fullPosterUrl = if (posterSrc.startsWith("/")) {
                "https://filmpalast.to$posterSrc"
            } else {
                posterSrc
            }

            val info = article.select("*").toInfo()

            Movie(
                id = href.substringAfterLast("/"),
                title = title,
                released = info.released,
                quality = info.quality,
                rating = info.rating ?: 0.0,
                poster = fullPosterUrl
            )
        }.distinctBy { it.id }

        return movies

    }



    override suspend fun getMovie(id: String): Movie {
        val relativeId = BASE_URL + "stream/" + id;
        val document = service.getMoviePage(relativeId)
        val title = document.selectFirst("h2")?.text() ?: ""
        val poster = document.selectFirst("img.cover2")?.attr("src")?.let {
            if (it.startsWith("http")) it else "${BASE_URL.removeSuffix("/")}$it"
        }
        val description = document.selectFirst("span[itemprop=description]")?.text()
        val rating = document.selectFirst("div#star-rate")?.attr("data-rating")?.toDoubleOrNull()
        val genres =
            document.select("ul#detail-content-list > li:has(p:matchesOwn(Kategorien, Genre)) a")
                .map { Genre(id = it.text().trim(), name = it.text().trim()) }
        val directors = document.select("ul#detail-content-list > li:has(p:matchesOwn(Regie)) a")
            .map { People(id = it.text().trim(), name = it.text().trim()) }
        val actors =
            document.select("ul#detail-content-list > li:has(p:matchesOwn(Schauspieler)) a")
                .map { People(id = it.text().trim(), name = it.text().trim()) }

        return Movie(
            id = id,
            title = title,
            poster = poster,
            genres = genres,
            directors = directors,
            cast = actors,
            rating = rating,
            overview = description
        )
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val relativeId = BASE_URL + "stream/" + id;
        val document = service.getMoviePage(relativeId)
        val servers = mutableListOf<Video.Server>()

        val serverBlocks = document.select("ul.currentStreamLinks")
        val keywords = listOf("bigwarp", "vinovo")
        for (block in serverBlocks) {
            val name = block.selectFirst("li.hostBg p.hostName")?.text()?.trim() ?: "Unbekannt"
            var linkElement = block.selectFirst("a[href]")
            var url = linkElement?.attr("href")?.trim()
            if (linkElement == null){
                linkElement = block.selectFirst("a[data-player-url]")
                url = linkElement?.attr("data-player-url")?.trim();
            }


            if (!url.isNullOrEmpty()) {
                val displayName = if (keywords.none { name.lowercase().contains(it) }) {
                    name
                } else {
                    "$name (VLC Only)"
                }
                servers.add(
                    Video.Server(
                        id = name.split(" ")[0],
                        name = displayName,
                        src = url
                    )
                )
            }
        }

        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val response = service.getRedirectLink(server.src)
            .let { response -> response.raw() as okhttp3.Response }


        val videoUrl = response.request.url
        val link = when (server.name) {
            "VOE" -> {
                val baseUrl = "https://voe.sx"
                val path = videoUrl.encodedPath.trimStart('/')
                "$baseUrl/e/$path?"
            }

            else -> videoUrl.toString()
        }
        return Extractor.extract(link, server)
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val document = service.getMovies(page)
        val movies = document.select("div#content article").map { article ->
            val href = article.selectFirst("h2 a")?.attr("href") ?: ""
            val title = article.selectFirst("h2 a")?.text() ?: ""
            val posterSrc = article.selectFirst("a img")?.attr("src") ?: ""

            val fullPosterUrl = if (posterSrc.startsWith("/")) {
                "https://filmpalast.to$posterSrc"
            } else {
                posterSrc
            }

            val info = article.select("*").toInfo()

            Movie(
                id = href.substringAfterLast("/"),
                title = title,
                released = info.released,
                quality = info.quality,
                rating = info.rating ?: 0.0,
                poster = fullPosterUrl
            )
        }
        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        throw Exception("Serien nicht implementiert")
    }

    override suspend fun getTvShow(id: String): TvShow {
        throw Exception("Serien nicht implementiert")
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = service.getGenre(id, page)

        val shows = document.select("div#content article").map { article ->
            val aTag = article.selectFirst("h2 a")
            val href = aTag?.attr("href").orEmpty()
            val title = aTag?.text().orEmpty()

            val posterSrc = article.selectFirst("a img")?.attr("src").orEmpty()
            val fullPosterUrl = if (posterSrc.startsWith("/")) {
                "https://filmpalast.to$posterSrc"
            } else {
                posterSrc
            }

            val info = article.select("*").toInfo()

            Movie(
                id = href.substringAfterLast("/"),
                title = title,
                released = info.released,
                quality = info.quality,
                rating = info.rating ?: 0.0,
                poster = fullPosterUrl
            )
        }

        return Genre(
            id = id,
            name = id.replaceFirstChar { it.uppercase() },
            shows = shows
        )
    }


    override suspend fun getEpisodesBySeason(seasonId: String) = emptyList<Episode>()
    override suspend fun getPeople(id: String, page: Int) = People(id, "")


    private fun Elements.toInfo() =
        this.mapNotNull { it.text().trim().takeIf { it.isNotEmpty() } }.let { textList ->
            val starCount = this.select("img[src*=star_on]").size
            val rating = starCount / 10.0

            object {
                val rating =
                    rating.takeIf { starCount > 0 }

                val quality = textList.find { it in listOf("HD", "SD", "CAM", "TS", "HDRip") }

                val released = textList.find { it.matches(Regex("\\d{4}")) }

                val lastEpisode = textList.find { it.matches(Regex("S\\d+\\s*:E\\d+")) }?.let { s ->
                    val result = Regex("S(\\d+)\\s*:E(\\d+)").find(s)?.groupValues
                    object {
                        val season = result?.getOrNull(1)?.toIntOrNull() ?: 0
                        val episode = result?.getOrNull(2)?.toIntOrNull() ?: 0
                    }
                }
            }
        }


    interface FilmpalastService {

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

            fun build(): FilmpalastService {
                val client = getOkHttpClient()
                val retrofit = Retrofit.Builder().baseUrl(BASE_URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create()).client(client).build()
                return retrofit.create(FilmpalastService::class.java)
            }
        }

        @GET("/")
        suspend fun getHome(): Document

        @GET
        suspend fun getMoviePage(@Url url: String): Document

        @GET
        suspend fun getTvShow(@Url url: String): Document

        @GET("movies/new/page/{page}")
        suspend fun getMovies(@Path("page") page: Int): Document

        @GET("serien/view/page/{page}")
        suspend fun getTvShows(@Path("page") page: Int): Document

        @GET("search/title/{query}/{page}")
        suspend fun search(@Path("query") query: String,@Path("page") page: Int): Document
        @GET("search/title/{query}")
        suspend fun searchNoPage(@Path("query") query: String): Document

        @GET
        @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        suspend fun getRedirectLink(@Url url: String): Response<ResponseBody>

        @GET("search/genre/{genre}/{page}")
        suspend fun getGenre(@Path("genre") genre: String, @Path("page") page: Int): Document

    }

}
