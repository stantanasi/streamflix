package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class TwoEmbedExtractor : Extractor() {

    override val name = "2Embed"
    override val mainUrl = "https://www.2embed.cc"

    fun server(videoType: Video.Type): Video.Server {
        return Video.Server(
            id = name,
            name = name,
            src = when (videoType) {
                is Video.Type.Episode -> "$mainUrl/embedtv/${videoType.tvShow.id}&s=${videoType.season.number}&e=${videoType.number}"
                is Video.Type.Movie -> "$mainUrl/embed/${videoType.id}"
            },
        )
    }

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val document = service.get(link)

        val iframeSrc = document.selectFirst("iframe")
            ?.attr("data-src")
            ?: throw Exception("Can't retrieve iframe src")

        val referer = getBaseUrl(iframeSrc)
        val id = iframeSrc.substringAfter("id=").substringBefore("&")

        val finalUrl = "https://uqloads.xyz/e/$id"

        return StreamWishExtractor.UqloadsXyz().extract(finalUrl, referer)
    }

    private fun getBaseUrl(url: String): String {
        val endIndex = url.indexOf("/", url.indexOf("://") + 3)
        return if (endIndex == -1) url else url.substring(0, endIndex)
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
        suspend fun get(@Url url: String): Document
    }
}