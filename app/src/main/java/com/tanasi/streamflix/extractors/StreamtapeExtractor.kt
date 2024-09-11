package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import okhttp3.ResponseBody
import org.jsoup.nodes.Document
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Streaming
import retrofit2.http.Url

class StreamtapeExtractor : Extractor() {

    override val name = "Streamtape"
    override val mainUrl = "https://streamtape.com"

    override suspend fun extract(link: String): Video {
        val linkJustParameter = link.replace(mainUrl, "")
        val linkVideoId = link.replace("https://streamtape.com/e/", "")

        val service = StreamtapeExtractorService.build(mainUrl)
        val source = service.getSource(linkJustParameter)

        val requestVideoParamters = source.html().split(linkVideoId)[10].split("').substring(")[0]
        val finalVideoUrl = "$mainUrl/get_video?id=$linkVideoId$requestVideoParamters"

        val response = service.getVideo(finalVideoUrl)
        val sourceUrl = (response.raw() as okhttp3.Response).networkResponse?.request?.url?.toString()
            ?: throw Exception("Can't retrieve URL")

        val video = Video(
            source = sourceUrl,
            subtitles = listOf()
        )
        return video
    }

    private interface StreamtapeExtractorService {
        companion object {
            fun build(baseUrl: String): StreamtapeExtractorService {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()

                return retrofit.create(StreamtapeExtractorService::class.java)
            }
        }

        @GET
        @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        suspend fun getSource(@Url url: String): Document

        @GET
        @Streaming
        @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        suspend fun getVideo(@Url url: String): Response<ResponseBody>
    }
}