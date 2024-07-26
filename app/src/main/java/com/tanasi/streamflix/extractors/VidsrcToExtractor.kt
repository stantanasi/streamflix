package com.tanasi.streamflix.extractors

import android.util.Base64
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.retry
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class VidsrcToExtractor : Extractor() {

    override val name = "Vidsrc.to"
    override val mainUrl = "https://vidsrc.to"
    val key = "https://raw.githubusercontent.com/Ciarands/vidsrc-keys/main/keys.json"

    fun server(videoType: Video.Type): Video.Server {
        return Video.Server(
            id = name,
            name = name,
            src = when (videoType) {
                is Video.Type.Episode -> "$mainUrl/embed/tv/${videoType.tvShow.id}/${videoType.season.number}/${videoType.number}"
                is Video.Type.Movie -> "$mainUrl/embed/movie/${videoType.id}"
            },
        )
    }

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val mediaId = service.get(link)
            .selectFirst("ul.episodes li a")
            ?.attr("data-id")
            ?: throw Exception("Can't retrieve media ID")

        val keys = service.getKeys(key)

        val sources = service.getSources(
            mediaId,
            token = encode(keys.encrypt[0], mediaId)
        ).result
            ?: throw Exception("Can't retrieve sources")

        val video = retry(sources.size) { attempt ->
            val source = sources[attempt - 1]

            val embedRes = service.getEmbedSource(
                source.id,
                token = encode(keys.encrypt[0], source.id)
            )
            val finalUrl = decryptUrl(keys.decrypt[0], embedRes.result.url)

            if (finalUrl == embedRes.result.url) throw Exception("finalUrl == embedUrl")

            when (source.title) {
                "F2Cloud",
                "Vidplay" -> VidplayExtractor.Any(finalUrl.substringBefore("/e/"))
                    .extract(finalUrl)

                "Filemoon" -> FilemoonExtractor.Any(finalUrl.substringBefore("/e/"))
                    .extract(finalUrl)

                else -> Extractor.extract(finalUrl)
            }
        }

        return video.copy(
            subtitles = service.getSubtitles(mediaId).map {
                Video.Subtitle(
                    it.label,
                    it.file,
                )
            }
        )
    }

    private fun decryptUrl(key: String, encUrl: String): String {
        var data = Base64.decode(encUrl.toByteArray(), Base64.URL_SAFE)
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        data = cipher.doFinal(data)
        return URLDecoder.decode(data.toString(Charsets.UTF_8), "utf-8")
    }

    private fun encode(key: String, vId: String): String {
        val decodedId = decodeData(key, vId)

        val encodedBase64 = Base64.encode(decodedId, Base64.NO_WRAP).toString(Charsets.UTF_8)

        val decodedResult = encodedBase64
            .replace("/", "_")
            .replace("+", "-")

        return decodedResult
    }

    private fun decodeData(key: String, data: String): ByteArray {
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val s = ByteArray(256) { it.toByte() }
        var j = 0

        for (i in 0 until 256) {
            j = (j + s[i].toInt() + keyBytes[i % keyBytes.size].toInt()) and 0xff
            s[i] = s[j].also { s[j] = s[i] }
        }

        val decoded = ByteArray(data.length)
        var i = 0
        var k = 0

        for (index in decoded.indices) {
            i = (i + 1) and 0xff
            k = (k + s[i].toInt()) and 0xff
            s[i] = s[k].also { s[k] = s[i] }
            val t = (s[i].toInt() + s[k].toInt()) and 0xff

            decoded[index] = (data[index].code xor s[t].toInt()).toByte()
        }

        return decoded
    }

    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET
        suspend fun get(@Url url: String): Document

        @GET
        suspend fun getKeys(@Url url: String): Keys

        @GET("ajax/embed/episode/{mediaId}/sources")
        suspend fun getSources(
            @Path("mediaId") mediaId: String,
            @Query("token") token: String,
        ): EpisodeSources

        @GET("ajax/embed/source/{sourceId}")
        suspend fun getEmbedSource(
            @Path("sourceId") sourceId: String,
            @Query("token") token: String,
        ): EmbedSource

        @GET("ajax/embed/episode/{mediaId}/subtitles")
        suspend fun getSubtitles(@Path("mediaId") mediaId: String): List<Subtitles>
    }


    data class EpisodeSources(
        val status: Int,
        val result: List<Result>?
    ) {

        data class Result(
            val id: String,
            val title: String
        )
    }

    data class EmbedSource(
        val status: Int,
        val result: Result
    ) {
        data class Result(
            val url: String
        )
    }

    data class Subtitles(
        val label: String,
        val file: String,
    )

    data class Keys(
        val encrypt: List<String>,
        val decrypt: List<String>,
    )
}