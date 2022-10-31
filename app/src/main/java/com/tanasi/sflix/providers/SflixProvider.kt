package com.tanasi.sflix.providers

import android.util.Base64
import com.google.gson.*
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.sflix.models.*
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SflixProvider {

    private val sflixService = SflixService.build()


    suspend fun getHome(): List<Category> {
        val document = sflixService.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = "Featured",
                list = document
                    .select("div.swiper-wrapper > div.swiper-slide")
                    .map {
                        val isMovie = it.selectFirst("a")
                            ?.attr("href")
                            ?.contains("/movie/")
                            ?: false

                        val id = it.selectFirst("a")
                            ?.attr("href")
                            ?.substringAfterLast("-") ?: ""
                        val title = it.select("h2.film-title").text()
                        val overview = it.selectFirst("p.sc-desc")?.text() ?: ""
                        val poster = it.selectFirst("img.film-poster-img")
                            ?.attr("src") ?: ""
                        val banner = it.selectFirst("div.slide-photo img")
                            ?.attr("src") ?: ""

                        when (isMovie) {
                            true -> {
                                val info = it
                                    .select("div.sc-detail > div.scd-item")
                                    .toList()
                                    .map { element -> element.text() }
                                    .let { info ->
                                        object {
                                            val rating = when (info.size) {
                                                1 -> info[0].toDoubleOrNull()
                                                2 -> info[0].toDoubleOrNull()
                                                3 -> info[0].toDoubleOrNull()
                                                else -> null
                                            }
                                            val quality = when (info.size) {
                                                2 -> info[1] ?: ""
                                                3 -> info[1] ?: ""
                                                else -> null
                                            }
                                            val released = when (info.size) {
                                                3 -> info[2] ?: ""
                                                else -> null
                                            }
                                        }
                                    }

                                Movie(
                                    id = id,
                                    title = title,
                                    overview = overview,
                                    released = info.released,
                                    quality = info.quality ?: "",
                                    rating = info.rating,
                                    poster = poster,
                                    banner = banner,
                                )
                            }
                            false -> {
                                val info = it
                                    .select("div.sc-detail > div.scd-item")
                                    .toList()
                                    .map { element -> element.text() }
                                    .let { info ->
                                        object {
                                            val rating = when (info.size) {
                                                1 -> info[0].toDoubleOrNull()
                                                2 -> info[0].toDoubleOrNull()
                                                3 -> info[0].toDoubleOrNull()
                                                else -> null
                                            }
                                            val quality = when (info.size) {
                                                3 -> info[1] ?: ""
                                                else -> null
                                            }
                                            val lastEpisode = when (info.size) {
                                                2 -> info[1] ?: ""
                                                3 -> info[2] ?: ""
                                                else -> null
                                            }
                                        }
                                    }

                                TvShow(
                                    id = id,
                                    title = title,
                                    overview = overview,
                                    quality = info.quality ?: "",
                                    rating = info.rating,
                                    poster = poster,
                                    banner = banner,

                                    seasons = info.lastEpisode?.let { lastEpisode ->
                                        listOf(
                                            Season(
                                                id = "",
                                                number = lastEpisode
                                                    .substringAfter("S")
                                                    .substringBefore(" :")
                                                    .toIntOrNull() ?: 0,

                                                episodes = listOf(
                                                    Episode(
                                                        id = "",
                                                        number = lastEpisode
                                                            .substringAfter(":")
                                                            .substringAfter("E")
                                                            .toIntOrNull() ?: 0,
                                                    )
                                                )
                                            )
                                        )
                                    } ?: listOf(),
                                )
                            }
                        }
                    },
            )
        )

        categories.add(
            Category(
                name = "Trending Movies",
                list = document
                    .select("div#trending-movies")
                    .select("div.flw-item")
                    .map {
                        val info = it
                            .select("div.film-detail > div.fd-infor > span")
                            .toList()
                            .map { element -> element.text() }
                            .let { info ->
                                object {
                                    val released = when (info.size) {
                                        1 -> info[0] ?: ""
                                        2 -> info[1] ?: ""
                                        3 -> info[2] ?: ""
                                        else -> null
                                    }
                                    val quality = when (info.size) {
                                        3 -> info[1] ?: ""
                                        else -> null
                                    }
                                    val rating = when (info.size) {
                                        2 -> info[0].toDoubleOrNull()
                                        3 -> info[0].toDoubleOrNull()
                                        else -> null
                                    }
                                }
                            }

                        Movie(
                            id = it.selectFirst("a")
                                ?.attr("href")
                                ?.substringAfterLast("-")
                                ?: "",
                            title = it.select("h3.film-name").text(),
                            released = info.released ?: "",
                            quality = info.quality ?: "",
                            rating = info.rating,
                            poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                ?.attr("data-src")
                                ?: "",
                        )
                    },
            )
        )

        categories.add(
            Category(
                name = "Trending TV Shows",
                list = document
                    .select("div#trending-tv")
                    .select("div.flw-item")
                    .map {
                        val info = it
                            .select("div.film-detail > div.fd-infor > span")
                            .toList()
                            .map { element -> element.text() }
                            .let { info ->
                                object {
                                    val quality = when (info.size) {
                                        3 -> info[1] ?: ""
                                        else -> null
                                    }
                                    val rating = when (info.size) {
                                        2 -> info[0].toDoubleOrNull()
                                        3 -> info[0].toDoubleOrNull()
                                        else -> null
                                    }
                                    val lastEpisode = when (info.size) {
                                        1 -> info[0] ?: ""
                                        2 -> info[1] ?: ""
                                        3 -> info[2] ?: ""
                                        else -> null
                                    }
                                }
                            }

                        TvShow(
                            id = it.selectFirst("a")
                                ?.attr("href")
                                ?.substringAfterLast("-")
                                ?: "",
                            title = it.select("h3.film-name").text(),
                            quality = info.quality ?: "",
                            rating = info.rating,
                            poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                ?.attr("data-src")
                                ?: "",

                            seasons = info.lastEpisode?.let { lastEpisode ->
                                listOf(
                                    Season(
                                        id = "",
                                        number = lastEpisode
                                            .substringAfter("S")
                                            .substringBefore(":")
                                            .toIntOrNull() ?: 0,

                                        episodes = listOf(
                                            Episode(
                                                id = "",
                                                number = lastEpisode
                                                    .substringAfter(":")
                                                    .substringAfter("E")
                                                    .toIntOrNull() ?: 0,
                                            )
                                        )
                                    )
                                )
                            } ?: listOf()
                        )
                    },
            )
        )

        categories.add(
            Category(
                name = "Latest Movies",
                list = document
                    .select(".section-id-02:has(h2:matchesOwn(Latest Movies))")
                    .select("div.flw-item")
                    .map {
                        val info = it
                            .select("div.film-detail > div.fd-infor > span")
                            .toList()
                            .map { element -> element.text() }
                            .let { info ->
                                object {
                                    val released = when (info.size) {
                                        1 -> info[0] ?: ""
                                        2 -> info[1] ?: ""
                                        3 -> info[2] ?: ""
                                        else -> null
                                    }
                                    val quality = when (info.size) {
                                        3 -> info[1] ?: ""
                                        else -> null
                                    }
                                    val rating = when (info.size) {
                                        2 -> info[0].toDoubleOrNull()
                                        3 -> info[0].toDoubleOrNull()
                                        else -> null
                                    }
                                }
                            }

                        Movie(
                            id = it.selectFirst("a")
                                ?.attr("href")
                                ?.substringAfterLast("-")
                                ?: "",
                            title = it.select("h3.film-name").text(),
                            released = info.released ?: "",
                            quality = info.quality ?: "",
                            rating = info.rating,
                            poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                ?.attr("data-src")
                                ?: "",
                        )
                    },
            )
        )

        categories.add(
            Category(
                name = "Latest TV Shows",
                list = document
                    .select(".section-id-02:has(h2:matchesOwn(Latest TV Shows))")
                    .select("div.flw-item")
                    .map {
                        val info = it
                            .select("div.film-detail > div.fd-infor > span")
                            .toList()
                            .map { element -> element.text() }
                            .let { info ->
                                object {
                                    val quality = when (info.size) {
                                        3 -> info[1] ?: ""
                                        else -> null
                                    }
                                    val rating = when (info.size) {
                                        2 -> info[0].toDoubleOrNull()
                                        3 -> info[0].toDoubleOrNull()
                                        else -> null
                                    }
                                    val lastEpisode = when (info.size) {
                                        1 -> info[0] ?: ""
                                        2 -> info[1] ?: ""
                                        3 -> info[2] ?: ""
                                        else -> null
                                    }
                                }
                            }

                        TvShow(
                            id = it.selectFirst("a")
                                ?.attr("href")
                                ?.substringAfterLast("-")
                                ?: "",
                            title = it.select("h3.film-name").text(),
                            quality = info.quality ?: "",
                            rating = info.rating,
                            poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                ?.attr("data-src")
                                ?: "",

                            seasons = info.lastEpisode?.let { lastEpisode ->
                                listOf(
                                    Season(
                                        id = "",
                                        number = lastEpisode
                                            .substringAfter("S")
                                            .substringBefore(":")
                                            .toIntOrNull() ?: 0,

                                        episodes = listOf(
                                            Episode(
                                                id = "",
                                                number = lastEpisode
                                                    .substringAfter(":")
                                                    .substringAfter("E")
                                                    .toIntOrNull() ?: 0,
                                            )
                                        )
                                    )
                                )
                            } ?: listOf()
                        )
                    },
            )
        )

        return categories
    }

    suspend fun search(query: String): List<Show> {
        if (query.isEmpty()) return listOf()

        val document = sflixService.search(query.replace(" ", "-"))

        val results = document.select("div.flw-item").map {
            val isMovie = it.selectFirst("a")
                ?.attr("href")
                ?.contains("/movie/")
                ?: false

            val id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: ""
            val title = it.select("h2.film-name").text()
            val poster =
                it.selectFirst("div.film-poster > img.film-poster-img").let { img ->
                    img?.attr("data-src") ?: img?.attr("src")
                } ?: ""

            when (isMovie) {
                true -> {
                    val info = it
                        .select("div.film-detail > div.fd-infor > span")
                        .toList()
                        .map { element -> element.text() }
                        .let { info ->
                            object {
                                val released = when (info.size) {
                                    1 -> info[0] ?: ""
                                    2 -> info[1] ?: ""
                                    3 -> info[2] ?: ""
                                    else -> null
                                }
                                val quality = when (info.size) {
                                    3 -> info[1] ?: ""
                                    else -> null
                                }
                                val rating = when (info.size) {
                                    2 -> info[0].toDoubleOrNull()
                                    3 -> info[0].toDoubleOrNull()
                                    else -> null
                                }
                            }
                        }

                    Movie(
                        id = id,
                        title = title,
                        released = info.released,
                        quality = info.quality ?: "",
                        rating = info.rating,
                        poster = poster,
                    )
                }
                false -> {
                    val info = it
                        .select("div.film-detail > div.fd-infor > span")
                        .toList()
                        .map { element -> element.text() }
                        .let { info ->
                            object {
                                val quality = when (info.size) {
                                    3 -> info[1] ?: ""
                                    else -> null
                                }
                                val rating = when (info.size) {
                                    2 -> info[0].toDoubleOrNull()
                                    3 -> info[0].toDoubleOrNull()
                                    else -> null
                                }
                                val lastEpisode = when (info.size) {
                                    1 -> info[0] ?: ""
                                    2 -> info[1] ?: ""
                                    3 -> info[2] ?: ""
                                    else -> null
                                }
                            }
                        }

                    TvShow(
                        id = id,
                        title = title,
                        quality = info.quality ?: "",
                        rating = info.rating,
                        poster = poster,

                        seasons = info.lastEpisode?.let { lastEpisode ->
                            listOf(
                                Season(
                                    id = "",
                                    number = lastEpisode
                                        .substringAfter("S")
                                        .substringBefore(":")
                                        .toIntOrNull() ?: 0,

                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = lastEpisode
                                                .substringAfter(":")
                                                .substringAfter("E")
                                                .toIntOrNull() ?: 0,
                                        )
                                    )
                                )
                            )
                        } ?: listOf(),
                    )
                }
            }
        }

        return results
    }


    interface SflixService {

        companion object {
            fun build(): SflixService {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://sflix.to/")
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(
                        GsonConverterFactory.create(
                            GsonBuilder()
                                .registerTypeAdapter(
                                    SourcesResponse::class.java,
                                    SourcesResponse.Deserializer(),
                                )
                                .create()
                        )
                    )
                    .build()

                return retrofit.create(SflixService::class.java)
            }
        }


        @GET("home")
        suspend fun getHome(): Document

        @GET("search/{query}")
        suspend fun search(@Path("query") query: String): Document

        @GET("movie")
        suspend fun getMovies(): Document

        @GET("tv-show")
        suspend fun getTvShows(): Document


        @GET("movie/free-{id}")
        suspend fun getMovieById(@Path("id") id: String): Document

        @GET("ajax/movie/episodes/{id}")
        suspend fun getMovieServersById(@Path("id") movieId: String): Document


        @GET("tv/free-{id}")
        suspend fun getTvShowById(@Path("id") id: String): Document

        @GET("ajax/v2/tv/seasons/{id}")
        suspend fun getTvShowSeasonsById(@Path("id") tvShowId: String): Document

        @GET("ajax/v2/season/episodes/{id}")
        suspend fun getSeasonEpisodesById(@Path("id") seasonId: String): Document

        @GET("ajax/v2/episode/servers/{id}")
        suspend fun getEpisodeServersById(@Path("id") episodeId: String): Document


        @GET("cast/{slug}")
        suspend fun getPeopleBySlug(@Path("slug") slug: String): Document


        @GET("ajax/get_link/{id}")
        suspend fun getLink(@Path("id") id: String): Link

        @GET
        @Headers(
            "Accept: */*",
            "Accept-Language: en-US,en;q=0.5",
            "Connection: keep-alive",
            "referer: https://sflix.to",
            "TE: trailers",
            "X-Requested-With: XMLHttpRequest",
        )
        suspend fun getSources(
            @Url url: String,
            @Query("id") id: String,
        ): SourcesResponse

        @GET("https://raw.githubusercontent.com/consumet/rapidclown/dokicloud/key.txt")
        suspend fun getSourceEncryptedKey(): Document


        data class Link(
            val type: String = "",
            val link: String = "",
            val sources: List<String> = listOf(),
            val tracks: List<String> = listOf(),
            val title: String = "",
        )

        sealed class SourcesResponse {
            class Deserializer : JsonDeserializer<SourcesResponse> {
                override fun deserialize(
                    json: JsonElement?,
                    typeOfT: Type?,
                    context: JsonDeserializationContext?
                ): SourcesResponse {
                    val jsonObject = json?.asJsonObject ?: JsonObject()

                    return when (jsonObject.get("sources")?.isJsonArray ?: false) {
                        true -> Gson().fromJson(json, Sources::class.java)
                        false -> Gson().fromJson(json, Sources.Encrypted::class.java)
                    }
                }
            }
        }

        data class Sources(
            val sources: List<Source> = listOf(),
            val sourcesBackup: List<Source> = listOf(),
            val tracks: List<Track> = listOf(),
            val server: Int? = null,
        ) : SourcesResponse() {

            data class Encrypted(
                val sources: String,
                val sourcesBackup: String? = null,
                val tracks: List<Track> = listOf(),
                val server: Int? = null,
            ) : SourcesResponse() {
                fun decrypt(secret: String): Sources {
                    fun decryptSourceUrl(decryptionKey: ByteArray, sourceUrl: String): String {
                        val cipherData = Base64.decode(sourceUrl, Base64.DEFAULT)
                        val encrypted = cipherData.copyOfRange(16, cipherData.size)
                        val aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding")!!

                        aesCBC.init(
                            Cipher.DECRYPT_MODE,
                            SecretKeySpec(
                                decryptionKey.copyOfRange(0, 32),
                                "AES"
                            ),
                            IvParameterSpec(decryptionKey.copyOfRange(32, decryptionKey.size))
                        )
                        val decryptedData = aesCBC.doFinal(encrypted)
                        return String(decryptedData, StandardCharsets.UTF_8)
                    }

                    fun generateKey(salt: ByteArray, secret: ByteArray): ByteArray {
                        fun md5(input: ByteArray) = MessageDigest.getInstance("MD5").digest(input)

                        var key = md5(secret + salt)
                        var currentKey = key
                        while (currentKey.size < 48) {
                            key = md5(key + secret + salt)
                            currentKey += key
                        }
                        return currentKey
                    }

                    val decrypted = decryptSourceUrl(
                        generateKey(
                            Base64.decode(sources, Base64.DEFAULT).copyOfRange(8, 16),
                            secret.toByteArray(),
                        ),
                        sources,
                    )

                    return Sources(
                        sources = Gson().fromJson(decrypted, Array<Source>::class.java).toList(),
                        tracks = tracks
                    )
                }

                data class SecretKey(val key: String = "")
            }

            data class Source(
                val file: String = "",
                val type: String = "",
            )

            data class Track(
                val file: String = "",
                val label: String = "",
                val kind: String = "",
                val default: Boolean = false,
            )
        }
    }
}