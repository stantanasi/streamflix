package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class OkruExtractor : Extractor() {

    override val name = "Okru"
    override val mainUrl = "https://ok.ru"

    private val service = Service.build(mainUrl)

    override suspend fun extract(link: String): Video {
        val document = service.get(link)

        val videoString = document.selectFirst("div[data-options]")
            ?.attr("data-options")
            ?: throw Exception("No se encontró 'data-options' en la página de Ok.ru")

        val arrayData = videoString.substringAfterLast("\\\"videos\\\":[{\\\"name\\\":\\\"").substringBefore("]")
        val videos = arrayData.split("{\\\"name\\\":\\\"").reversed().mapNotNull {
            val videoUrl = it.substringAfter("url\\\":\\\"").substringBefore("\\\"").replace("\\\\u0026", "&")
            val quality = fixQuality(it.substringBefore("\\\""))

            if (videoUrl.startsWith("https://")) {
                Pair(quality, videoUrl)
            } else {
                null
            }
        }

        if (videos.isEmpty()) {
            throw Exception("No se encontraron videos válidos en el JSON de Ok.ru")
        }

        val bestVideoUrl = videos.first().second

        return Video(
            source = bestVideoUrl,
            headers = mapOf("Referer" to mainUrl)
        )
    }

    private fun fixQuality(quality: String): String {
        return when (quality) {
            "ultra" -> "2160p"
            "quad" -> "1440p"
            "full" -> "1080p"
            "hd" -> "720p"
            "sd" -> "480p"
            "low" -> "360p"
            "lowest" -> "240p"
            "mobile" -> "144p"
            else -> quality
        }
    }

    private interface Service {
        companion object {
            fun build(baseUrl: String): Service {
                val client = OkHttpClient.Builder()
                    .followRedirects(true)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                            .build()
                        chain.proceed(request)
                    }
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET
        suspend fun get(@Url url: String): Document
    }
}