package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.AesHelper
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

open class ChillxExtractor : Extractor() {

    override val name = "Chillx"
    override val mainUrl = "https://chillx.top"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val document = service.getDocument(link, mainUrl)
        val content = Regex("\\s*=\\s*'([^']+)").find(document.toString())
            ?.groupValues?.get(1)
            ?: throw Exception("Can't retrieve content")

        val key = service.getKeys().chillx[0]

        val decrypt = AesHelper.cryptoAESHandler(
            content,
            key.toByteArray(),
            false
        )
            ?.replace("\\n", "\n")
            ?.replace("\\", "")
            ?: throw Exception("Failed to decrypt")


        val video = Video(
            source = Regex("\"?file\"?:\\s*\"([^\"]+)").find(decrypt)
                ?.groupValues?.get(1)
                ?: throw Exception("Can't retrieve source"),
            subtitles = Regex("\\{\"file\":\"([^\"]+)\",\"label\":\"([^\"]+)\",\"kind\":\"captions\",\"default\":\\w+\\}")
                .findAll(decrypt)
                .map {
                    Video.Subtitle(
                        label = it.groupValues[2],
                        file = it.groupValues[1],
                    )
                }
                .toList(),
            headers = mapOf(
                "Referer" to mainUrl,
            ),
        )

        return video
    }

    class JeanExtractor : ChillxExtractor() {
        override val name = "Jean"
        override val mainUrl = "https://player.jeansaispasplus.homes/"
    }

    class MoviesapiExtractor : ChillxExtractor() {
        override val name = "Moviesapi"
        override val mainUrl = "https://moviesapi.club/"
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
        suspend fun getDocument(
            @Url url: String,
            @Header("referer") referer: String,
        ): Document

        @GET("https://raw.githubusercontent.com/Rowdy-Avocado/multi-keys/keys/index.html")
        suspend fun getKeys(): Keys


        data class Keys(
            val chillx: List<String>,
        )
    }
}