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
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import com.tanasi.streamflix.models.Show

object CuevanaDosProvider : Provider {

    private const val URL = "https://www.cuevana2espanol.net/"
    override val baseUrl = URL
    override val name = "Cuevana2 Español"
    override val logo =
        "https://www.cuevana2espanol.net/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Flogo.33e4f182.png&w=640&q=7"
    override val language = "es"

    private val service = CuevanaDosService.build();

    override suspend fun getHome(): List<Category> {
        val document = service.getHome()
        val categories = mutableListOf<Category>()

        fun parseTvShows(row: Element?): List<TvShow> {
            return row?.select("div.col")?.mapNotNull { col ->
                val a = col.selectFirst("a") ?: return@mapNotNull null
                val title = col.selectFirst("h3")?.text() ?: return@mapNotNull null
                val poster = col.selectFirst("img")?.attr("src") ?: return@mapNotNull null
                TvShow(
                    id = a.attr("href").substringAfterLast("/"),
                    title = title,
                    poster = "https://www.cuevana2espanol.net$poster"
                )
            } ?: emptyList()
        }

        fun parseMovies(row: Element?): List<Movie> {
            return row?.select("div.col")?.mapNotNull { col ->
                val a = col.selectFirst("a") ?: return@mapNotNull null
                val title = col.selectFirst("h3")?.text() ?: return@mapNotNull null
                val poster = col.selectFirst("img")?.attr("src") ?: return@mapNotNull null
                Movie(
                    id = a.attr("href").substringAfterLast("/"),
                    title = title,
                    poster = "https://www.cuevana2espanol.net$poster",
                    overview = null,
                    released = null,
                    runtime = null,
                    rating = null,
                    genres = emptyList(),
                    cast = emptyList()
                )
            } ?: emptyList()
        }

        val rows = document.select("div.row.row-cols-xl-5.row-cols-lg-4.row-cols-3")

        categories.add(
            Category(
                name = "Películas destacadas del día",
                list = parseMovies(rows.getOrNull(0))
            )
        )

        categories.add(
            Category(
                name = "Series destacadas del día",
                list = parseTvShows(rows.getOrNull(1))
            )
        )

        categories.add(
            Category(
                name = "Últimas películas online",
                list = parseMovies(rows.getOrNull(2))
            )
        )

        categories.add(
            Category(
                name = "Últimas series online",
                list = parseTvShows(rows.getOrNull(3))
            )
        )

        categories.add(
            Category(
                name = "Últimos episodios online",
                list = parseTvShows(rows.getOrNull(4))
            )
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val genres = listOf(
                Genre("accion", "Acción"),
                Genre("animacion", "Animación"),
                Genre("crimen", "Crimen"),
                Genre("familia", "Fámilia"),
                Genre("misterio", "Misterio"),
                Genre("suspenso", "Suspenso"),
                Genre("aventura", "Aventura"),
                Genre("ciencia-ficcion", "Ciencia Ficción"),
                Genre("drama", "Drama"),
                Genre("fantasia", "Fantasía"),
                Genre("romance", "Romance"),
                Genre("terror", "Terror")
            )
            return genres
        }
        if (page > 1) {
            return emptyList()
        }
        val document = service.search(query)

        val items = mutableListOf<AppAdapter.Item>()
        val elements =
            document.select("div.col article:not(.MovieSidebarItem_item__U15hi):not(.SerieSidebarItem_item__Y_r4w)")
        for (element in elements) {
            val linkElement = element.selectFirst("a")
            val href = linkElement?.attr("href") ?: continue
            val title = element.selectFirst("h3")?.text()?.trim() ?: continue
            val year = element.selectFirst("span")?.text()?.trim() ?: ""
            val rawImgUrl = element.selectFirst("img")?.attr("src") ?: ""
            val imgUrl = rawImgUrl.toUri().getQueryParameter("url") ?: rawImgUrl

            val id = href.removePrefix("/movies/").removePrefix("/series/")

            when {
                href.startsWith("/movies/") -> {
                    items.add(
                        Movie(
                            id = id,
                            title = title,
                            overview = "",
                            released = year,
                            runtime = 0,
                            poster = imgUrl,
                            banner = "",
                        )
                    )
                }

                href.startsWith("/series/") -> {
                    items.add(
                        TvShow(
                            id = id,
                            title = title,
                            overview = "",
                            released = year,
                            runtime = 0,
                            poster = imgUrl,
                            banner = "",
                        )
                    )
                }
            }
        }

        return items
    }


    override suspend fun getMovies(page: Int): List<Movie> {
        val document = service.getMovies(page)

        return document.select("div.col article").mapNotNull { article ->
            val linkElement = article.selectFirst("a")
            val title = article.selectFirst("h3")?.text() ?: return@mapNotNull null
            val href = linkElement?.attr("href") ?: return@mapNotNull null
            val poster = article.selectFirst("img")?.attr("src") ?: return@mapNotNull null

            Movie(
                id = href.substringAfterLast("/"),
                title = title,
                poster = if (poster.startsWith("http")) poster else "https://www.cuevana2espanol.net$poster",
                overview = null,
                released = article.selectFirst("span")?.text(),
                runtime = null,
                rating = null,
                genres = emptyList(),
                cast = emptyList()
            )
        }
    }


    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = service.getTvShows(page)

        return document.select("div.col article").mapNotNull { article ->
            val linkElement = article.selectFirst("a")
            val title = article.selectFirst("h3")?.text() ?: return@mapNotNull null
            val href = linkElement?.attr("href") ?: return@mapNotNull null
            val poster = article.selectFirst("img")?.attr("src") ?: return@mapNotNull null
            TvShow(
                id = href.substringAfterLast("/"),
                title = title,
                poster = if (poster.startsWith("http")) poster else "https://www.cuevana2espanol.net$poster",
                overview = null,
                released = article.selectFirst("span")?.text(),
                runtime = null,
                rating = null,
                genres = emptyList(),
                cast = emptyList()
            )
        }
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getMovie(id)

        val title = document.selectFirst("h1")?.text() ?: ""
        val overview = document.select("div.movieInfo_data__HL5zl > div.row")
            .lastOrNull()?.text()
        val year = document.selectFirst("div.movieInfo_extra__bP44U span")?.text()
        val runtimeText = document.select("div.movieInfo_extra__bP44U span")
            .firstOrNull { it.text().contains("Min.") }?.text()?.substringBefore(" Min.")?.trim()
        val ratingText = document.select("div.movieInfo_extra__bP44U span")
            .firstOrNull { it.text().contains("⭐") }?.text()?.substringAfter("⭐")
            ?.substringBefore("/")?.trim()
        val poster = document.selectFirst("div.movieInfo_image__LJrqk img")
            ?.attr("src")?.let { "https://www.cuevana2espanol.net$it" }

        val genres = document.select("tr:contains(Géneros) td a").map {
            Genre(
                id = it.attr("href").substringAfterLast("/"),
                name = it.text()
            )
        }

        val cast = document.select("tr:contains(Actores) td").text()
            .split(",").map {
                People(
                    id = "",
                    name = it.trim().replace("Actores", ""),
                    image = null
                )
            }

        return Movie(
            id = id,
            title = title,
            overview = overview,
            released = year,
            runtime = runtimeText?.toIntOrNull(),
            rating = ratingText?.toDoubleOrNull(),
            poster = poster,
            genres = genres,
            cast = cast
        )
    }


    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTvShow(id)
        val jsonData = document.selectFirst("script#__NEXT_DATA__")?.data() ?: return TvShow(
            id = id,
            title = "Unknown"
        )

        val json = JSONObject(jsonData)
            .getJSONObject("props")
            .getJSONObject("pageProps")
            .getJSONObject("post")

        val title = json.getJSONObject("titles").getString("name")
        val overview = json.optString("overview", null)
        val releaseDate = json.optString("releaseDate", null)?.take(10)
        val rating = json.optJSONObject("rate")?.optDouble("average", 0.0) ?: 0.0

        val poster = json.optJSONObject("images")?.optString("poster")
            ?.let { "https://image.tmdb.org/t/p/original$it" }

        val seasonsJson = json.optJSONArray("seasons") ?: JSONArray()
        val seasons = mutableListOf<Season>()
        for (i in 0 until seasonsJson.length()) {
            val seasonObj = seasonsJson.getJSONObject(i)
            val number = seasonObj.getInt("number")
            val slug = seasonObj.getJSONArray("episodes")
                .optJSONObject(0)
                ?.optJSONObject("slug")?.getString("name") ?: id

            seasons.add(
                Season(
                    id = "$slug/$number",
                    number = number,
                    title = "Temporada $number"
                )
            )
        }

        return TvShow(
            id = id,
            title = title,
            overview = overview,
            released = releaseDate,
            rating = rating,
            poster = poster,
            banner = null,
            trailer = null,
            genres = emptyList(),
            cast = emptyList(),
            directors = emptyList(),
            seasons = seasons
        )
    }

    private val seasonCache = mutableMapOf<String, JSONArray>()

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val (slug, seasonNumStr) = seasonId.split("/")
        val seasonNumber = seasonNumStr.toIntOrNull() ?: return emptyList()

        val seasonsJson: JSONArray = seasonCache.getOrPut(slug) {
            val document = service.getTvShow(slug)
            val jsonData = document.selectFirst("script#__NEXT_DATA__")?.data()
                ?: return emptyList<Episode>().also {
                    seasonCache.remove(slug)
                }

            val root = JSONObject(jsonData)
                .getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("post")

            root.optJSONArray("seasons") ?: JSONArray()
        }

        for (i in 0 until seasonsJson.length()) {
            val seasonObj = seasonsJson.getJSONObject(i)
            if (seasonObj.optInt("number") != seasonNumber) continue

            val episodesJson = seasonObj.optJSONArray("episodes") ?: return emptyList()

            return (0 until episodesJson.length()).mapNotNull { idx ->
                val ep = episodesJson.getJSONObject(idx)
                val slugObj = ep.optJSONObject("slug") ?: return@mapNotNull null

                val id = "${slugObj.optString("name")}/${slugObj.optString("season")}/${
                    slugObj.optString("episode")
                }"

                Episode(
                    id = id,
                    number = ep.optInt("number"),
                    title = ep.optString("title"),
                    poster = ep.optString("image"),
                    released = ep.optString("releaseDate")?.take(10)
                )
            }
        }

        return emptyList()
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = service.getGenre(id, page)
        val elements =
            document.select("div.col article:not(.MovieSidebarItem_item__U15hi):not(.SerieSidebarItem_item__Y_r4w)")

        val shows = mutableListOf<Show>()

        for (element in elements) {
            val linkElement = element.selectFirst("a") ?: continue
            val href = linkElement.attr("href")
            val title = element.selectFirst("h3")?.text()?.trim() ?: continue
            val year = element.selectFirst("span")?.text()?.trim() ?: ""
            val rawImgUrl = linkElement.selectFirst("img")?.attr("src") ?: ""
            val poster = rawImgUrl.toUri().getQueryParameter("url") ?: rawImgUrl

            val id = href.removePrefix("/movies/").removePrefix("/series/")

            val show: Show? = when {
                href.startsWith("/movies/") -> Movie(
                    id = id,
                    title = title,
                    overview = "",
                    released = year,
                    runtime = 0,
                    poster = poster,
                    banner = ""
                )

                href.startsWith("/series/") -> TvShow(
                    id = id,
                    title = title,
                    overview = "",
                    released = year,
                    runtime = 0,
                    poster = poster,
                    banner = ""
                )

                else -> null
            }

            show?.let { shows.add(it) }
        }

        val genre = Genre(id = id, name = id.replaceFirstChar { it.uppercase() }, shows = shows)

        return genre

    }


    override suspend fun getPeople(
        id: String,
        page: Int
    ): People {
        throw Exception("Function not available for Cuevana2")
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        var isSerie = false
        val document: Document = when (videoType) {
            is Video.Type.Movie -> {
                service.getMovie(id)
            }

            is Video.Type.Episode -> {
                val splitString = id.split("/")
                isSerie = true
                service.getEpisode(splitString[0], splitString[1], splitString[2])
            }
        }

        val scriptJson = document.selectFirst("script#__NEXT_DATA__")?.data() ?: return emptyList()

        val json = JSONObject(scriptJson)
        var players: JSONObject
        if (isSerie) {
            players = json
                .getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("episode")
                .getJSONObject("players")
        } else {
            players = json
                .getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("post")
                .getJSONObject("players")
        }


        val servers = mutableListOf<Video.Server>()
        for (language in listOf("latino", "spanish", "english")) {
            if (players.has(language)) {
                val serverArray = players.getJSONArray(language)
                for (i in 0 until serverArray.length()) {
                    val obj = serverArray.getJSONObject(i)
                    val embedUrl = obj.getString("result")
                    val realUrl = resolveRealProviderUrl(embedUrl)
                    val name = obj.getString("cyberlocker") + " (${language.capitalized()})"
                    servers.add(
                        Video.Server(
                            id = realUrl ?: embedUrl,
                            name = name,
                            src = realUrl ?: embedUrl
                        )
                    )
                }
            }
        }

        return servers
    }

    fun resolveRealProviderUrl(playerUrl: String): String? {
        val playerPage = Jsoup.connect(playerUrl)
            .header("Referer", URL)
            .get()
        val scriptContent = playerPage.select("script").firstOrNull {
            it.data().contains("var url =")
        }?.data() ?: return null
        val regex = Regex("var url\\s*=\\s*['\"](https?://[^'\"]+)['\"]")
        val match = regex.find(scriptContent)
        return match?.groupValues?.get(1)
    }

    fun String.capitalized(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase())
                it.titlecase(Locale.getDefault())
            else it.toString()
        }
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


    private interface CuevanaDosService {
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

            fun build(): CuevanaDosService {
                val client = getOkHttpClient()
                val retrofit = Retrofit.Builder().baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create()).client(client).build()
                return retrofit.create(CuevanaDosService::class.java)
            }
        }

        @GET(".")
        suspend fun getHome(): Document

        @GET("/search")
        suspend fun search(@Query("q") query: String): Document

        @GET("movies/{name}")
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