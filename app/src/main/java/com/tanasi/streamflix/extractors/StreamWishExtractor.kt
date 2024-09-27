package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

open class StreamWishExtractor : Extractor() {

    override val name = "Streamwish"
    override val mainUrl = "https://streamwish.to"

    protected var referer = ""

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val document = service.get(getEmbedUrl(link), referer = referer)

        val script = document.select("script")
            .let { scripts -> scripts.getOrNull(scripts.size - 2) }
            ?.html()
            ?.let { JsUnpacker(it).unpack() }
            ?: throw Exception("Can't retrieve script")

        val video = Video(
            source = Regex("file:\\s*\"(.*?m3u8.*?)\"").find(script)
                ?.groupValues?.getOrNull(1)
                ?: throw Exception("Can't retrieve m3u8"),
            subtitles = emptyList(),
        )

        return video
    }

    private fun getEmbedUrl(url: String): String {
        return if (url.contains("/f/")) {
            val videoId = url.substringAfter("/f/")
            "$mainUrl/$videoId"
        } else {
            url
        }
    }


    class UqloadsXyz : StreamWishExtractor() {
        override val name = "Uqloads"
        override val mainUrl = "https://uqloads.xyz"

        suspend fun extract(link: String, referer: String): Video {
            this.referer = referer
            return extract(link)
        }
    }


    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET
        suspend fun get(
            @Url url: String,
            @Header("referer") referer: String = "",
        ): Document
    }
}