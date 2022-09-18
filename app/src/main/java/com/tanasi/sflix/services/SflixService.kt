package com.tanasi.sflix.services

import android.util.Base64
import com.google.gson.*
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
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

    @GET("https://raw.githubusercontent.com/BlipBlob/blabflow/main/keys.json")
    suspend fun getSourceEncryptedKey(): Sources.Encrypted.SecretKey


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

                return when (jsonObject.get("encrypted")?.asBoolean ?: false) {
                    true -> Gson().fromJson(json, Sources.Encrypted::class.java)
                    false -> Gson().fromJson(json, Sources::class.java)
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
