package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class VidozaExtractor : Extractor() {

    override val name = "Vidoza"

    override val mainUrl = "https://vidoza.net"
    override val aliasUrls = listOf<String>("https://videzz.net")

    override suspend fun extract(link: String): Video {
        val service = VoeExtractorService.build(mainUrl)
        val source = service.getSource(link.replace(mainUrl, ""))
        val videoUrl = source.select("source").attr("src")
        return Video(
            source = videoUrl,
            subtitles = listOf()
        )
    }


    private interface VoeExtractorService {

        companion object {
            fun build(baseUrl: String): VoeExtractorService {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()

                return retrofit.create(VoeExtractorService::class.java)
            }
        }

        @GET
        suspend fun getSource(@Url url: String): Document
    }
}