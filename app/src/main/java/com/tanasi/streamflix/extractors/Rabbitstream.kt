package com.tanasi.streamflix.extractors

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.tanasi.streamflix.models.Video
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

open class Rabbitstream : Extractor() {

    override val name = "Rabbitstream"
    override val mainUrl = "https://rabbitstream.net"
    protected open val embed = "ajax/embed-4"
    protected open val key = "http://zoro-keys.freeddns.org/keys/e4/key.txt"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val response = service.getSources(
            url = "$mainUrl/$embed/getSources",
            id = link.substringAfterLast("/").substringBefore("?"),
            referer = mainUrl,
        )

        val sources = when (response) {
            is Service.Sources -> response
            is Service.Sources.Encrypted -> response.decrypt(
                enikey = service.getSourceEncryptedKey(key)
            )
        }

        val video = Video(
            source = sources.sources.map { it.file }.firstOrNull() ?: "",
            subtitles = sources.tracks
                .filter { it.kind == "captions" }
                .map {
                    Video.Subtitle(
                        label = it.label,
                        file = it.file,
                    )
                }
        )

        return video
    }


    class Megacloud : Rabbitstream() {
        override val name = "Megacloud"
        override val mainUrl = "https://megacloud.tv"
        override val embed = "embed-2/ajax/e-1"
        override val key = "http://zoro-keys.freeddns.org/keys/e6/key.txt"
    }

    class Dokicloud : Rabbitstream() {
        override val name = "Dokicloud"
        override val mainUrl = "https://dokicloud.one"
    }


    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
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

                return retrofit.create(Service::class.java)
            }
        }


        @GET
        @Headers(
            "Accept: */*",
            "Accept-Language: en-US,en;q=0.5",
            "Connection: keep-alive",
            "TE: trailers",
            "X-Requested-With: XMLHttpRequest",
        )
        suspend fun getSources(
            @Url url: String,
            @Query("id") id: String,
            @Header("referer") referer: String,
        ): SourcesResponse

        @GET
        suspend fun getSourceEncryptedKey(@Url url: String): List<List<Int>>


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
                fun decrypt(enikey: List<List<Int>>): Sources {
                    fun extract(
                        encryptedSources: String,
                        key: List<List<Int>>
                    ): Pair<String, String> {
                        var extractedSources = ""
                        var extractedKey = ""

                        for (i in encryptedSources.indices) {
                            val currentKey = key.firstOrNull { i < it[1] } ?: key[0]

                            if (i in currentKey[0] until currentKey[1]) {
                                extractedKey += encryptedSources[i]
                            } else {
                                extractedSources += encryptedSources[i]
                            }
                        }

                        return extractedSources to extractedKey
                    }

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

                    val (extractedSources, extractedKey) = extract(sources, enikey)

                    val decrypted = decryptSourceUrl(
                        generateKey(
                            Base64.decode(extractedSources, Base64.DEFAULT).copyOfRange(8, 16),
                            extractedKey.toByteArray(),
                        ),
                        extractedSources,
                    )

                    return Sources(
                        sources = Gson().fromJson(decrypted, Array<Source>::class.java).toList(),
                        tracks = tracks
                    )
                }
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