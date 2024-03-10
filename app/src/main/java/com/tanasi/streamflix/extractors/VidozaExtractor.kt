package com.tanasi.streamflix.extractors

import android.util.Log
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class VidozaExtractor : Extractor() {

    override val name = "Vidoza"
    override val mainUrl = "https://vidoza.net"

    override suspend fun extract(link: String): Video {
        Log.d("streamflixDebug", "Vidoza link: " + link)

        val service = VoeExtractorService.build(mainUrl)
        val source = service.getSource(link.replace(mainUrl, ""))

        val videoUrl = source.select("source").attr("src")

        Log.d("streamflixDebug", "VoeExtractor videoUrl: " + videoUrl)
        val video = Video(
            source = videoUrl,
            subtitles = listOf()
        )

        return video
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