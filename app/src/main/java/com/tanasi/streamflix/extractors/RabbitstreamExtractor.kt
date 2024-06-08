package com.tanasi.streamflix.extractors

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.tanasi.streamflix.BuildConfig
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.StringConverterFactory
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
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

open class RabbitstreamExtractor : Extractor() {

    override val name = "Rabbitstream"
    override val mainUrl = "https://rabbitstream.net"
    protected open val embed = "ajax/v2/embed-4"
    protected open val key = "https://keys4.fun"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val sourceId = link.substringAfterLast("/").substringBefore("?")

        val response = service.getSources(
            url = BuildConfig.RABBITSTREAM_SOURCE_API + sourceId,
        )

        val sources = when (response) {
            is Service.Sources -> response
            is Service.Sources.Encrypted -> response.decrypt(
                key = service.getSourceEncryptedKey(key).rabbitstream.keys.key,
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


    class MegacloudExtractor : RabbitstreamExtractor() {

        override val name = "Megacloud"
        override val mainUrl = "https://megacloud.tv"
        override val embed = "embed-2/ajax/e-1"
        private val scriptUrl = "$mainUrl/js/player/a/prod/e1-player.min.js"

        override suspend fun extract(link: String): Video {
            val service = Service.build(mainUrl)

            val response = service.getSources(
                url = "$mainUrl/$embed/getSources",
                id = link.substringAfterLast("/").substringBefore("?"),
                referer = mainUrl,
            )

            val sources = when (response) {
                is Service.Sources -> response
                is Service.Sources.Encrypted -> {
                    val (key, sources) = extractRealKey(response.sources)
                    response.sources = sources
                    response.decrypt(key)
                }
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

        private suspend fun extractRealKey(source: String): Pair<String, String> {
            val rawKeys = getKeys()

            var secret = ""
            var encryptedSource = source
            var totalInc = 0
            for (i in 0 until rawKeys[0]) {
                val start = rawKeys[(i + 1) * 2]
                val inc = rawKeys[(i + 1) * 2 - 1]

                val from = start + totalInc
                val to = from + inc

                secret += source.slice(from until to)
                encryptedSource = encryptedSource.replace(
                    source.substring(from, to),
                    ""
                )
                totalInc += inc
            }

            return secret to encryptedSource
        }

        private suspend fun getKeys(): List<Int> {
            val service = Service.build(mainUrl)
            val script = service.getScript(scriptUrl, Date().time / 1000)

            val keys = Regex("const \\w{1,2}=new URLSearchParams.+?;(?=function)")
                .findAll(script).toList().lastOrNull()?.let { match ->
                    match.value
                        .substring(0, match.value.length - 1)
                        .split("=")
                        .drop(1)
                        .mapNotNull { pair -> pair.split(",").first().replace("0x", "").toIntOrNull(16) }
                }
                ?: throw Exception("Can't retrieve encryption key")

            return keys
        }
    }

    class DokicloudExtractor : RabbitstreamExtractor() {
        override val name = "Dokicloud"
        override val mainUrl = "https://dokicloud.one"
    }


    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(StringConverterFactory.create())
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
        @Headers(
            "Accept: */*",
            "Accept-Language: en-US,en;q=0.5",
            "Connection: keep-alive",
            "TE: trailers",
            "X-Requested-With: XMLHttpRequest",
        )
        suspend fun getSources(
            @Url url: String,
        ): SourcesResponse

        @GET
        suspend fun getSourceEncryptedKey(@Url url: String): KeysResponse

        @GET
        suspend fun getScript(@Url url: String, @Query("v") v: Long): String


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
                var sources: String,
                val sourcesBackup: String? = null,
                val tracks: List<Track> = listOf(),
                val server: Int? = null,
            ) : SourcesResponse() {
                fun decrypt(key: String): Sources {
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

                        var output = md5(secret + salt)
                        var currentKey = output
                        while (currentKey.size < 48) {
                            output = md5(output + secret + salt)
                            currentKey += output
                        }
                        return currentKey
                    }

                    val decrypted = decryptSourceUrl(
                        generateKey(
                            Base64.decode(sources, Base64.DEFAULT).copyOfRange(8, 16),
                            key.toByteArray(),
                        ),
                        sources,
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

        data class KeysResponse(
            val rabbitstream: Rabbitstream,
            val megacloud_m: MegacloudM,
        ) {

            data class Rabbitstream(
                val keys: Keys,
                val updated_at: Int,
            ) {
                data class Keys(
                    val v: String,
                    val h: String,
                    val b: String,
                    val agent: String,
                    val key: String,
                )
            }

            data class MegacloudM(
                val keys: List<Int>,
                val updated_at: Int,
            )
        }
    }
}