package com.tanasi.streamflix.providers

import android.util.Base64
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.*
import com.tanasi.streamflix.models.sololatino.Item
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.nodes.Document
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.io.File
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SoloLatinoProvider : Provider {

    override val name = "SoloLatino"
    override val baseUrl = "https://sololatino.net"
    override val language = "es"

    private val client = getOkHttpClient()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JsoupConverterFactory.create())
        .client(client)
        .build()

    private val service = retrofit.create(SoloLatinoService::class.java)

    private fun getOkHttpClient(): OkHttpClient {
        val appCache = Cache(File("cacheDir", "okhttpcache"), 10 * 1024 * 1024)

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .build()
                chain.proceed(request)
            }
            .cache(appCache)
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)

        val dns = DnsOverHttps.Builder().client(clientBuilder.build())
            .url("https://1.1.1.1/dns-query".toHttpUrl())
            .build()

        return clientBuilder.dns(dns).build()
    }

    private interface SoloLatinoService {
        @GET
        suspend fun getPage(@Url url: String): Document

        @POST("wp-admin/admin-ajax.php")
        suspend fun getPlayerAjax(
            @Header("Referer") referer: String,
            @Body body: RequestBody
        ): Response<ResponseBody>
    }

    override val logo = "$baseUrl/wp-content/uploads/2022/11/logo-final.png"

    override suspend fun getHome(): List<Category> = coroutineScope {
        val categories = mutableListOf<Category>()

        val deferredMap = mapOf(
            "Tendencias" to async { service.getPage("$baseUrl/tendencias/page/1") },
            "Películas de Estreno" to async { service.getPage("$baseUrl/pelicula/estrenos") },
            "Series Mejor Valoradas" to async { service.getPage("$baseUrl/series/mejor-valoradas") },
            "Animes Mejor Valorados" to async { service.getPage("$baseUrl/animes/mejor-valoradas") },
            "Toons" to async { service.getPage("$baseUrl/genre_series/toons") },
            "KDramas" to async { service.getPage("$baseUrl/genre_series/kdramas") }
        )

        try {
            val trendingDoc = deferredMap["Tendencias"]?.await()
            if (trendingDoc != null) {
                val bannerShows = parseBannerShows(trendingDoc).take(12)
                if (bannerShows.isNotEmpty()) {
                    categories.add(Category(Category.FEATURED, bannerShows))
                }
            }
        } catch (e: Exception) { /* Ignore */ }

        for ((categoryName, deferred) in deferredMap) {
            try {
                val doc = deferred.await()
                val shows = if (categoryName.contains("Películas")) parseMovies(doc) else parseTvShows(doc)
                if (shows.isNotEmpty()) {
                    categories.add(Category(categoryName, shows.take(12)))
                }
            } catch (e: Exception) { /* Ignore */ }
        }

        categories
    }

    private fun parseBannerShows(document: Document): List<Show> {
        return document.select("article.item").mapNotNull { element ->
            val linkElement = element.selectFirst("a") ?: return@mapNotNull null
            val href = linkElement.attr("href")
            val absoluteUrl = if (href.startsWith("http")) href else "$baseUrl$href"
            val imageUrl = element.selectFirst("img")?.attr("data-srcset") ?: ""

            TvShow(
                id = absoluteUrl,
                title = element.selectFirst("img")!!.attr("alt"),
                banner = if (imageUrl.startsWith("http")) imageUrl else "$baseUrl$imageUrl"
            )
        }
    }

    private fun parseMovies(document: Document): List<Movie> {
        return document.select("article.item").mapNotNull { element ->
            val linkElement = element.selectFirst("a") ?: return@mapNotNull null
            val href = linkElement.attr("href")
            val absoluteUrl = if (href.startsWith("http")) href else "$baseUrl$href"
            val posterUrl = element.selectFirst("img")?.attr("data-srcset") ?: ""

            Movie(
                id = absoluteUrl,
                title = element.selectFirst("img")!!.attr("alt"),
                poster = if (posterUrl.startsWith("http")) posterUrl else "$baseUrl$posterUrl"
            )
        }
    }

    private fun parseTvShows(document: Document): List<TvShow> {
        return document.select("article.item").mapNotNull { element ->
            val linkElement = element.selectFirst("a") ?: return@mapNotNull null
            val href = linkElement.attr("href")
            val absoluteUrl = if (href.startsWith("http")) href else "$baseUrl$href"
            val posterUrl = element.selectFirst("img")?.attr("data-srcset") ?: ""

            TvShow(
                id = absoluteUrl,
                title = element.selectFirst("img")!!.attr("alt"),
                poster = if (posterUrl.startsWith("http")) posterUrl else "$baseUrl$posterUrl"
            )
        }
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isBlank()) {
            return listOf(
                Genre("accion", "Acción"),
                Genre("action-adventure", "Action & Adventure"),
                Genre("animacion", "Animación"),
                Genre("aventura", "Aventura"),
                Genre("belica", "Bélica"),
                Genre("ciencia-ficcion", "Ciencia Ficción"),
                Genre("comedia", "Comedia"),
                Genre("crimen", "Crimen"),
                Genre("disney", "Disney"),
                Genre("documental", "Documental"),
                Genre("drama", "Drama"),
                Genre("familia", "Familia"),
                Genre("fantasia", "Fantasía"),
                Genre("hbo", "HBO"),
                Genre("historia", "Historia"),
                Genre("kids", "Kids"),
                Genre("misterio", "Misterio"),
                Genre("musica", "Música"),
                Genre("romance", "Romance"),
                Genre("sci-fi-fantasy", "Sci-Fi & Fantasy"),
                Genre("soap", "Soap"),
                Genre("suspense", "Suspense"),
                Genre("talk", "Talk"),
                Genre("terror", "Terror"),
                Genre("war-politics", "War & Politics"),
                Genre("western", "Western"),
            )
        }

        return try {
            val document = service.getPage("$baseUrl/page/$page?s=$query")
            parseTvShows(document) // Usamos TvShow como genérico para búsqueda
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        return try {
            val document = service.getPage("$baseUrl/pelicula/estrenos/page/$page")
            parseMovies(document)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        return try {
            val document = service.getPage("$baseUrl/series/page/$page")
            parseTvShows(document)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        return try {
            val document = service.getPage("$baseUrl/page/$page?s=$id")
            val shows = parseTvShows(document)
            Genre(
                id = id,
                name = id.replaceFirstChar { it.uppercase() },
                shows = shows
            )
        } catch (e: Exception) {
            Genre(id = id, name = id.replaceFirstChar { it.uppercase() }, shows = emptyList())
        }
    }

    override suspend fun getMovie(id: String): Movie {
        return try {
            val document = service.getPage(id)
            val sheader = document.selectFirst("div.sheader")!!

            val posterUrl = sheader.selectFirst("div.poster > img")?.attr("src") ?: ""
            val title = sheader.selectFirst("div.data > h1")?.text() ?: "Sin Título"
            val genres = sheader.select("div.data > div.sgeneros > a").map {
                Genre(id = it.attr("href").substringAfter("/genres/").removeSuffix("/"), name = it.text())
            }
            val overview = document.selectFirst("div.wp-content > p")?.text()
            val banner = document.selectFirst("div.wallpaper")?.attr("style")?.substringAfter("url(")?.substringBefore(")")
            val rating = document.selectFirst("div.nota > span")?.text()?.substringBefore(" ")?.toDoubleOrNull()

            val extraInfo = document.selectFirst("div.sbox.extra")
            val runtime = extraInfo?.selectFirst("span.runtime")?.text()?.removeSuffix(" Min.")?.trim()?.toIntOrNull()
            val released = sheader.selectFirst("span.date")?.text()?.split(", ")?.getOrNull(1)
            val trailer = extraInfo?.selectFirst("li > span > a[href*=youtube]")?.attr("href")

            val cast = document.select("div.sbox.srepart div.person").map {
                People(
                    id = it.selectFirst("a")?.attr("href") ?: "",
                    name = it.selectFirst(".name a")?.text() ?: "",
                    image = it.selectFirst(".img img")?.attr("src")
                )
            }

            val recommendations = document.select("div.sbox.srelacionados article").mapNotNull { article ->
                val linkElement = article.selectFirst("a") ?: return@mapNotNull null
                val imgElement = article.selectFirst("img") ?: return@mapNotNull null
                val href = linkElement.attr("href")
                val absoluteUrl = if (href.startsWith("http")) href else "$baseUrl$href"
                val recPoster = imgElement.attr("data-srcset")

                TvShow(
                    id = absoluteUrl,
                    title = imgElement.attr("alt"),
                    poster = if (recPoster.startsWith("http")) recPoster else "$baseUrl$recPoster"
                )
            }

            Movie(
                id = id,
                title = title,
                poster = if (posterUrl.startsWith("http")) posterUrl else "$baseUrl$posterUrl",
                banner = banner,
                genres = genres,
                overview = overview,
                rating = rating,
                runtime = runtime,
                released = released,
                trailer = trailer,
                cast = cast,
                recommendations = recommendations,
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getTvShow(id: String): TvShow {
        return try {
            val document = service.getPage(id)
            val sheader = document.selectFirst("div.sheader")!!

            val posterUrl = sheader.selectFirst("div.poster > img")?.attr("src") ?: ""
            val title = sheader.selectFirst("div.data > h1")?.text() ?: "Sin Título"
            val genres = sheader.select("div.data > div.sgeneros > a").map {
                Genre(id = it.attr("href").substringAfter("/genres/").removeSuffix("/"), name = it.text())
            }
            val overview = document.selectFirst("div.wp-content > p")?.text()
            val seasons = document.select("div#seasons div.se-c").map { seasonElement ->
                val seasonNumber = seasonElement.attr("data-season").toIntOrNull() ?: 0
                Season(id = "$id@$seasonNumber", number = seasonNumber, title = "Temporada $seasonNumber")
            }.filter { it.number != 0 }

            val banner = document.selectFirst("div.wallpaper")?.attr("style")?.substringAfter("url(")?.substringBefore(")")
            val rating = document.selectFirst("div.nota > span")?.text()?.substringBefore(" ")?.toDoubleOrNull()
            val extraInfo = document.selectFirst("div.sbox.extra")
            val runtime = extraInfo?.selectFirst("span.runtime")?.text()?.removeSuffix(" Min.")?.trim()?.toIntOrNull()
            val released = sheader.selectFirst("span.date")?.text()?.split(", ")?.getOrNull(1)
            val trailer = extraInfo?.selectFirst("li > span > a[href*=youtube]")?.attr("href")

            val cast = document.select("div.sbox.srepart div.person").map {
                People(
                    id = it.selectFirst("a")?.attr("href") ?: "",
                    name = it.selectFirst(".name a")?.text() ?: "",
                    image = it.selectFirst(".img img")?.attr("src")
                )
            }

            val recommendations = document.select("div.sbox.srelacionados article").mapNotNull { article ->
                val linkElement = article.selectFirst("a") ?: return@mapNotNull null
                val imgElement = article.selectFirst("img") ?: return@mapNotNull null
                val href = linkElement.attr("href")
                val absoluteUrl = if (href.startsWith("http")) href else "$baseUrl$href"
                val recPoster = imgElement.attr("data-srcset")

                TvShow(
                    id = absoluteUrl,
                    title = imgElement.attr("alt"),
                    poster = if (recPoster.startsWith("http")) recPoster else "$baseUrl$recPoster"
                )
            }

            TvShow(
                id = id,
                title = title,
                poster = if (posterUrl.startsWith("http")) posterUrl else "$baseUrl$posterUrl",
                banner = banner,
                genres = genres,
                overview = overview,
                seasons = seasons.reversed(),
                rating = rating,
                runtime = runtime,
                released = released,
                trailer = trailer,
                cast = cast,
                recommendations = recommendations,
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        return try {
            val showId = seasonId.substringBefore("@")
            val seasonNumber = seasonId.substringAfter("@")
            val document = service.getPage(showId)
            val seasonElement = document.select("div.se-c[data-season=$seasonNumber]").firstOrNull() ?: return emptyList()
            seasonElement.select("ul.episodios li").map { episodeElement ->
                val numerando = episodeElement.selectFirst("div.numerando")?.text() ?: "0 - 0"
                val episodeNum = numerando.split("-").getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                val episodeTitle = episodeElement.selectFirst("div.epst > h3.title")?.text() ?: "Episodio $episodeNum"
                Episode(
                    id = episodeElement.selectFirst("a")!!.attr("href"),
                    number = episodeNum,
                    title = episodeTitle,
                    poster = episodeElement.selectFirst("div.imagen > img")?.attr("src")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        return try {
            val doc = service.getPage(id)
            val servers = mutableListOf<Video.Server>()
            val linkElements = doc.select("li[data-type][data-post][data-nume]")

            for (element in linkElements) {
                val post = element.attr("data-post")
                val nume = element.attr("data-nume")
                val type = element.attr("data-type")

                val formBody = FormBody.Builder()
                    .add("action", "doo_player_ajax")
                    .add("post", post)
                    .add("nume", nume)
                    .add("type", type)
                    .build()

                val ajaxResponse = service.getPlayerAjax(id, formBody)
                val ajaxBody = ajaxResponse.body()?.string() ?: continue

                val iframeUrl = ajaxBody.substringAfter("src='").substringBefore("'")
                if (iframeUrl.isBlank()) continue

                val iframeDoc = service.getPage(iframeUrl)
                val iframeHtml = iframeDoc.html()

                val dataLinkMatch = Regex("""dataLink = (\[.+?\]);""").find(iframeHtml)
                if (dataLinkMatch != null) {
                    val items = json.decodeFromString<List<Item>>(dataLinkMatch.groupValues[1])
                    for (item in items) {
                        val lang = when(item.video_language) {
                            "LAT" -> "[LAT]"
                            "ESP" -> "[CAST]"
                            "SUB" -> "[SUB]"
                            else -> ""
                        }
                        for (embed in item.sortedEmbeds) {
                            val decryptedLink = decryptAES(embed.link) ?: continue
                            servers.add(Video.Server(id = decryptedLink, name = "${embed.servername} $lang".trim()))
                        }
                    }
                }
            }
            servers.distinctBy { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun decryptAES(encrypted: String): String? {
        return try {
            val key = "Ak7qrvvH4WKYxV2OgaeHAEg2a5eh16vE".toByteArray()
            val decoded = Base64.decode(encrypted, Base64.DEFAULT)
            val iv = decoded.copyOfRange(0, 16)
            val cipherText = decoded.copyOfRange(16, decoded.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            String(cipher.doFinal(cipherText))
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return Extractor.extract(server.id, server)
    }

    override suspend fun getPeople(id: String, page: Int): People {
        return try {
            val document = service.getPage(id)
            val name = document.selectFirst(".data h1")?.text() ?: ""
            val poster = document.selectFirst(".poster img")?.attr("src")
            val filmography = parseTvShows(document) // La estructura de la lista es la misma
            People(
                id = id,
                name = name,
                image = poster?.let { if (it.startsWith("http")) it else "$baseUrl$it" },
                filmography = filmography
            )
        } catch (e: Exception) {
            throw e
        }
    }
}