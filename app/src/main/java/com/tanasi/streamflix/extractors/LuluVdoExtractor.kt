package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

class LuluVdoExtractor : Extractor() {

    override val name = "LuluVdo"
    override val mainUrl = "https://luluvdo.com/"
    override val aliasUrls = listOf("https://luluvdoo.com")
    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val document = service.get(link)

        val source = Regex("sources: \\[\\{file:\"(.*?)\"\\}").find(document.toString())
            ?.groupValues?.get(1)
            ?: throw Exception("Can't retrieve source")

        val subtitles = Regex("file: \"(.*?)\", label: \"(.*?)\"").findAll(
            Regex("tracks: \\[(.*?)]").find(document.toString())
                ?.groupValues?.get(1)
                ?: ""
        )
            .map {
                Video.Subtitle(
                    label = it.groupValues[2],
                    file = it.groupValues[1],
                )
            }
            .toList()
            .filter { it.label != "Upload captions" }

        return Video(
            source = source,
            subtitles = subtitles,
        )
    }


    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val client = OkHttpClient.Builder()
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(Service::class.java)
            }
        }


        @GET
        suspend fun get(@Url url: String): Document
    }
}