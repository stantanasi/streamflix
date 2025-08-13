package com.tanasi.streamflix.providers

import android.util.Log
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
import com.tanasi.streamflix.models.Show
import com.tanasi.streamflix.models.Video
import okhttp3.Cache
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


object OtakuversoProvider : Provider {

    override val name = "Otakuverso"
    override val baseUrl = "https://otakuverso.net"
    override val language = "es"
    override val logo = "$baseUrl/logo.png"

    private const val TAG = "OtakuversoProvider"

    private val client = getOkHttpClient()

    private val service = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JsoupConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
        .create(OtakuversoService::class.java)

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
            .cookieJar(object : CookieJar {
                private val cookieStore = mutableMapOf<String, List<Cookie>>()
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    cookieStore[url.host] = cookies
                }
                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return cookieStore[url.host] ?: emptyList()
                }
            })
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)

        val dns = DnsOverHttps.Builder().client(clientBuilder.build())
            .url("https://1.1.1.1/dns-query".toHttpUrl())
            .build()

        return clientBuilder.dns(dns).build()
    }

    private val genresList = listOf(
        Genre("jR", "Aventura"),
        Genre("k5", "Misterio"),
        Genre("l5", "Shounen"),
        Genre("mO", "Acción"),
        Genre("nR", "Fantasía"),
        Genre("oj", "Demonios"),
        Genre("p2", "Histórico"),
        Genre("q2", "Sobrenatural"),
        Genre("rE", "Artes Marciales"),
        Genre("vm", "Comedia"),
        Genre("wR", "Superpoderes"),
        Genre("x9", "Magia"),
        Genre("y7", "Deportes"),
        Genre("zY", "Drama"),
        Genre("AO", "Escolares"),
        Genre("BX", "Ciencia Ficción"),
    )

    private interface OtakuversoService {
        @GET
        suspend fun getPage(@Url url: String): Document

        @GET("/animes")
        suspend fun getAnimesPage(): Document

        @GET("/buscador")
        suspend fun search(
            @Query("q") query: String,
            @Query("page") page: Int
        ): Document

        @POST("/animes")
        suspend fun getShowsByFilter(
            @Body body: FormBody,
            @Query("page") page: Int
        ): Document

        @GET("/play-video")
        suspend fun getVideoEmbed(@Query("id") id: String): Document
    }

    private suspend fun getToken(): String {
        val document = service.getAnimesPage()
        val token = document.selectFirst("input[name=_token]")?.attr("value") ?: ""
        Log.d(TAG, "Token CSRF obtenido: $token")
        return token
    }

    override suspend fun getHome(): List<Category> {
        val categories = mutableListOf<Category>()

        coroutineScope {
            // Hacemos las llamadas iniciales en paralelo
            val estrenosDeferred = async { service.getPage("$baseUrl/estrenos") }
            val popularDeferred = async {
                val token = getToken()
                val formBody = FormBody.Builder()
                    .add("_token", token)
                    .add("search_genero", "0")
                    .add("search_anno", "0")
                    .add("search_tipo", "0")
                    .add("search_orden", "0")
                    .add("search_estado", "0")
                    .build()
                service.getShowsByFilter(formBody, 1)
            }

            // Paso 1: Obtener la lista de estrenos para obtener los IDs
            try {
                val estrenosDocument = estrenosDeferred.await()
                val estrenosListItems = parseShows(estrenosDocument)

                // Paso 2: Obtener los detalles (incluyendo el banner) de los 5 primeros estrenos en paralelo
                val featuredShowsDeferred = estrenosListItems.take(5).map { show ->
                    async {
                        try {
                            // Esta llamada obtiene el banner de la página de detalles
                            getTvShow(show.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al obtener detalles para el show ${show.id}: ${e.message}", e)
                            null
                        }
                    }
                }

                val featuredShows = featuredShowsDeferred.awaitAll().filterNotNull()
                if (featuredShows.isNotEmpty()) {
                    categories.add(Category(Category.FEATURED, featuredShows))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener la lista de Estrenos: ${e.message}", e)
            }

            // Paso 3: Añadir el resto de categorías (Populares, etc.)
            try {
                val popularDocument = popularDeferred.await()
                val popular = parseShows(popularDocument)
                if (popular.isNotEmpty()) {
                    categories.add(Category("Populares", popular))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener Populares: ${e.message}", e)
            }

            // Aquí puedes dejar la sección de géneros si la habías añadido
            val genreTasks = genresList.shuffled().take(3).map { genre ->
                async {
                    try {
                        val token = getToken()
                        val formBody = FormBody.Builder()
                            .add("_token", token)
                            .add("search_genero", genre.id)
                            .add("search_anno", "0")
                            .add("search_tipo", "0")
                            .add("search_orden", "0")
                            .add("search_estado", "0")
                            .build()
                        val document = service.getShowsByFilter(formBody, 1)
                        val shows = parseShows(document)
                        if (shows.isNotEmpty()) {
                            Category(genre.name, shows)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al obtener género ${genre.name}: ${e.message}", e)
                        null
                    }
                }
            }
            categories.addAll(genreTasks.awaitAll().filterNotNull())
        }

        return categories
    }

    // Nueva función auxiliar para el carrusel
    private fun parseCarouselShows(document: Document): List<Show> {
        return document.select("a.banner_link").mapNotNull { element ->
            val link = element.attr("abs:href")
            val title = element.selectFirst("h1")?.text().orEmpty()
            val bannerUrl = element.selectFirst("img")?.attr("abs:src") ?: ""
            val id = link.substringAfterLast("/")

            if (title.isNotEmpty() && bannerUrl.isNotEmpty()) {
                TvShow(
                    id = id,
                    title = title,
                    banner = bannerUrl,
                    poster = bannerUrl // Usar el banner como póster para el carrusel
                )
            } else {
                null
            }
        }
    }

    private fun parseShows(document: Document): List<TvShow> {
        val shows = document.select(".row [data-original-title]").mapNotNull { element ->
            val link = element.selectFirst("a") ?: return@mapNotNull null
            val title = element.selectFirst("p.font-GDSherpa-Bold")?.text().orEmpty()

            // Selector más robusto para la imagen, buscando en múltiples lugares
            val poster = element.selectFirst("img")?.attr("abs:src")
                ?: element.selectFirst("img")?.attr("data-src")
                ?: ""

            val url = link.attr("abs:href")
            val id = url.substringAfterLast("/")

            TvShow(
                id = id,
                title = title,
                poster = poster,
            )
        }
        Log.d(TAG, "Shows parseados: ${shows.size}")
        return shows
    }


    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isBlank()) {
            return genresList // <-- Cambio aquí
        }

        try {
            val document = service.search(query, page)
            return parseShows(document)
        } catch (e: Exception) {
            Log.e(TAG, "Error en search: ${e.message}", e)
            return emptyList()
        }
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        // Obtenemos los shows con tipo "2" (Películas) y los convertimos a objetos Movie
        return getShows(page, "2").map { tvShow ->
            Movie(
                id = tvShow.id,
                title = tvShow.title,
                poster = tvShow.poster,
            )
        }
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        // Simplemente obtenemos los shows con tipo "1" (Series)
        return getShows(page, "1")
    }

    private suspend fun getShows(page: Int, type: String): List<TvShow> {
        try {
            val token = getToken()
            if (token.isEmpty()) {
                throw Exception("No se pudo obtener el token CSRF.")
            }

            val formBody = FormBody.Builder()
                .add("_token", token)
                .add("search_genero", "0")
                .add("search_anno", "0")
                .add("search_tipo", type) // <-- Usamos el tipo que nos pasan
                .add("search_orden", "0")
                .add("search_estado", "0")
                .build()

            val document = service.getShowsByFilter(formBody, page)
            return parseShows(document)
        } catch (e: Exception) {
            Log.e(TAG, "Error en getShows (page: $page, type: $type): ${e.message}", e)
            return emptyList()
        }
    }

    // Reemplaza esta función completa en tu archivo
    override suspend fun getMovie(id: String): Movie {
        try {
            val url = "$baseUrl/anime/$id"
            val document = service.getPage(url)

            val title = document.selectFirst("#back_data_perfil .inn-text h1")?.text().orEmpty()
            val overview = document.selectFirst("#back_data_perfil .inn-text p.font14")?.ownText()

            // El banner se encuentra en el meta tag de og:image
            val banner = document.selectFirst("meta[property=og:image]")?.attr("content")

            // El póster se encuentra en la URL que nos mencionaste, la cual es una imagen más cuadrada
            val poster = document.selectFirst("div.img-in img[src*=storage/videos]")?.attr("abs:src")

            val genres = document.select(".pre a[href*=/genero/]").map {
                Genre(id = it.attr("href").substringAfterLast('/'), name = it.text())
            }

            val recommendations = document.select("h4:contains(También te puede interesar) + .row > div").mapNotNull { element ->
                val link = element.selectFirst("a") ?: return@mapNotNull null
                Movie(
                    id = link.attr("href").substringAfterLast('/'),
                    title = link.selectFirst(".font-GDSherpa-Bold")?.text().orEmpty(),
                    poster = element.selectFirst("img")?.attr("abs:src")
                )
            }

            return Movie(
                id = id,
                title = title,
                overview = overview,
                poster = poster,
                banner = banner,
                genres = genres,
                recommendations = recommendations
            )
        } catch (e: Exception) {
            throw e
        }
    }

    // Reemplaza esta función completa en tu archivo
    override suspend fun getTvShow(id: String): TvShow {
        try {
            val url = "$baseUrl/anime/$id"
            val document = service.getPage(url)

            val title = document.selectFirst("#back_data_perfil .inn-text h1")?.text().orEmpty()
            val overview = document.selectFirst("#back_data_perfil .inn-text p.font14")?.ownText()

            // El banner se encuentra en el meta tag de og:image
            val banner = document.selectFirst("meta[property=og:image]")?.attr("content")

            // El póster se encuentra en la URL que nos mencionaste, la cual es una imagen más cuadrada
            val poster = document.selectFirst("div.img-in img[src*=storage/videos]")?.attr("abs:src")

            val genres = document.select(".pre a[href*=/genero/]").map {
                Genre(id = it.attr("href").substringAfterLast('/'), name = it.text())
            }

            val recommendations = document.select("h4:contains(También te puede interesar) + .row > div").mapNotNull { element ->
                val link = element.selectFirst("a") ?: return@mapNotNull null
                TvShow(
                    id = link.attr("href").substringAfterLast('/'),
                    title = link.selectFirst(".font-GDSherpa-Bold")?.text().orEmpty(),
                    poster = element.selectFirst("img")?.attr("abs:src")
                )
            }

            val seasonElements = document.select(".dropdown-menu a")
            val seasons = if (seasonElements.isNotEmpty()) {
                seasonElements.mapIndexed { index, element ->
                    Season(
                        id = element.attr("abs:href"),
                        number = element.text().filter { it.isDigit() }.toIntOrNull() ?: (index + 1),
                        title = element.text()
                    )
                }
            } else {
                listOf(Season(id = url, number = 1, title = "Temporada 1"))
            }

            return TvShow(
                id = id,
                title = title,
                overview = overview,
                poster = poster,
                banner = banner,
                genres = genres,
                seasons = seasons.reversed(),
                recommendations = recommendations
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        try {
            // El seasonId que guardamos es la URL completa de la página de la temporada
            val document = service.getPage(seasonId)

            return document.select(".pl-lg-4 .container-fluid .row .col-6.text-center").map { element ->
                val episodeLink = element.selectFirst(".font-GDSherpa-Bold a")
                val episodeText = episodeLink?.text() ?: "Episodio"
                val episodeNumber = episodeText.substringAfter("Episodio").trim().toIntOrNull() ?: 0

                Episode(
                    // La ID del episodio será su propia URL
                    id = episodeLink?.attr("abs:href") ?: "",
                    number = episodeNumber,
                    title = episodeText,
                    // El poster del episodio no está disponible en esta vista, se podría añadir más tarde si se encuentra
                    poster = null,
                    released = element.selectFirst(".font14 .bog")?.text()?.trim()
                )
            }.reversed() // La web los muestra del más nuevo al más antiguo
        } catch (e: Exception) {
            Log.e(TAG, "Error en getEpisodesBySeason (seasonId: $seasonId): ${e.message}", e)
            return emptyList()
        }
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        try {
            val token = getToken()
            if (token.isEmpty()) {
                throw Exception("No se pudo obtener el token CSRF.")
            }

            val formBody = FormBody.Builder()
                .add("_token", token)
                .add("search_genero", id) // <-- Usamos el ID del género
                .add("search_anno", "0")
                .add("search_tipo", "0")
                .add("search_orden", "0")
                .add("search_estado", "0")
                .build()

            val document = service.getShowsByFilter(formBody, page)
            val shows = parseShows(document)

            // Buscamos el nombre del género en nuestra lista para devolver el objeto completo
            val genreName = genresList.find { it.id == id }?.name ?: id.replaceFirstChar { it.uppercase() }

            return Genre(id = id, name = genreName, shows = shows)
        } catch (e: Exception) {
            Log.e(TAG, "Error en getGenre (id: $id, page: $page): ${e.message}", e)
            return Genre(id = id, name = id, shows = emptyList())
        }
    }

    override suspend fun getPeople(id: String, page: Int): People {
        TODO("Not yet implemented")
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        return try {
            val servers = mutableListOf<Video.Server>()
            val episodeUrl: String = if (videoType is Video.Type.Movie) {
                val moviePageUrl = "$baseUrl/anime/$id"
                val movieDocument = service.getPage(moviePageUrl)

                movieDocument.selectFirst("a[href*=/episodio-]")
                    ?.attr("abs:href")
                    ?: throw Exception("La película se encuentra buscando manualmente en Buscar.")
            } else {
                id
            }

            val episodeDocument = service.getPage(episodeUrl)

            // 1. Extraemos el servidor de Filemoon (si está presente)
            val mainIframe = episodeDocument.selectFirst("iframe#ytplayer")
            if (mainIframe != null) {
                val serverUrl = mainIframe.attr("src")
                servers.add(Video.Server(id = serverUrl, name = "Filemoon"))
            }

            // 2. Extraemos los demás servidores del <select>
            val serverOptions = episodeDocument.select("#ssel option")

            for (option in serverOptions) {
                val serverName = option.text()
                val encodedValue = option.attr("value")

                // Si el servidor es Filemoon, lo ignoramos para no duplicarlo,
                // ya que lo extrajimos del iframe principal.
                if (serverName.equals("Filemoon", ignoreCase = true)) {
                    continue
                }

                // Aquí guardamos el valor codificado como ID para que getVideo() lo procese después.
                servers.add(Video.Server(id = encodedValue, name = serverName))
            }

            if (servers.isEmpty()) {
                throw Exception("No se encontraron servidores de video.")
            }

            servers
        } catch (e: Exception) {
            Log.e(TAG, "Error en getServers: ${e.message}", e)
            emptyList()
        }
    }

    // Nueva función auxiliar para decodificar los links de servidores
    private suspend fun decodeServerUrl(encodedValue: String): String? {
        return try {
            // Hacemos una petición POST con el valor codificado para obtener el iframe
            val document = service.getVideoEmbed(encodedValue)
            document.selectFirst("iframe")?.attr("src")
        } catch (e: Exception) {
            Log.e(TAG, "Error al decodificar URL del servidor: ${e.message}", e)
            null
        }
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return try {
            val finalUrl = if (server.id.startsWith("http")) {
                // Caso 1: Ya es una URL directa (como el iframe de Filemoon).
                server.id
            } else {
                // Caso 2: Es un valor codificado, necesitamos decodificarlo.
                decodeServerUrl(server.id)
                    ?: throw Exception("No se pudo obtener la URL del video para el servidor: ${server.name}")
            }

            // Pasamos la URL final (sea directa o decodificada) al extractor.
            Extractor.extract(finalUrl, server)
        } catch (e: Exception) {
            Log.e(TAG, "Error en getVideo: ${e.message}", e)
            throw e
        }
    }
}