package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class VoeExtractor : Extractor() {

    override val name = "VOE"
    override val mainUrl = "https://voe.sx/"

    override suspend fun extract(link: String): Video {
        val service = VoeExtractorService.build(mainUrl)
        val source = service.getSource(link.replace(mainUrl, ""))
        val hlsUrl = source.html().split("'hls': '")[1].split("',")[0]
        return Video(
            source = hlsUrl,
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