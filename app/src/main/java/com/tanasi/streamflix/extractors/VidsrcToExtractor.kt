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
import retrofit2.http.Url
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class VidsrcToExtractor : Extractor() {

    override val name = "Vidsrc.to"
    override val mainUrl = "https://vidsrc.to"

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

        val sources = service.getSources(mediaId).result
            ?: throw Exception("Can't retrieve sources")

        val video = retry(sources.size) { attempt ->
            val source = sources[attempt - 1]

            val embedRes = service.getEmbedSource(source.id)
            val finalUrl = decryptUrl(embedRes.result.url)

            if (finalUrl == embedRes.result.url) throw Exception("finalUrl == embedUrl")

            when (source.title) {
                "Vidplay" -> VidplayExtractor.Any(finalUrl.substringBefore("/e/"))
                    .extract(finalUrl)
//                    "Filemoon" -> FileMoon().getUrl(finalUrl, referer, subtitleCallback, callback)
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

    private fun decryptUrl(encUrl: String): String {
        var data = encUrl.toByteArray()
        data = Base64.decode(data, Base64.URL_SAFE)
        val rc4Key = SecretKeySpec("WXrUARXb1aDLaZjI".toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        data = cipher.doFinal(data)
        return URLDecoder.decode(data.toString(Charsets.UTF_8), "utf-8")
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

        @GET("ajax/embed/episode/{mediaId}/sources")
        suspend fun getSources(@Path("mediaId") mediaId: String): EpisodeSources

        @GET("ajax/embed/source/{sourceId}")
        suspend fun getEmbedSource(@Path("sourceId") sourceId: String): EmbedSource

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
}